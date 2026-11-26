package br.com.raizesnordeste.application;

import br.com.raizesnordeste.domain.Auditoria;
import br.com.raizesnordeste.domain.GatewayPagamento;
import br.com.raizesnordeste.domain.RepositorioFluxoPedido;
import br.com.raizesnordeste.domain.RepositorioPedidos;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@ApplicationScoped
public class FluxoPedidoService {
    @Inject RepositorioPedidos usuarios;
    @Inject RepositorioFluxoPedido repositorio;
    @Inject GatewayPagamento gateway;
    @Inject Auditoria auditoria;

    @Schema(requiredProperties = { "status" })
    public record AlteracaoStatus(String status) { }
    public record Estado(UUID id, String status, String situacaoPagamento) { }

    private RepositorioPedidos.Ator ator(UUID id) {
        return usuarios.ator(id).orElseThrow(() -> UsuariosService.erro(403, "ACESSO_NEGADO", "Usuário sem acesso."));
    }

    private RepositorioFluxoPedido.Pedido pedido(UUID id) {
        return repositorio.bloquearPedido(id)
                .orElseThrow(() -> UsuariosService.erro(404, "PEDIDO_NAO_ENCONTRADO", "Pedido não encontrado."));
    }

    private boolean daUnidade(RepositorioPedidos.Ator ator, RepositorioFluxoPedido.Pedido pedido) {
        return pedido.unidadeId().equals(ator.unidadeId());
    }

    @Transactional
    public RepositorioFluxoPedido.Tentativa pagar(UUID usuarioId, UUID pedidoId) {
        var ator = ator(usuarioId);
        var pedido = pedido(pedidoId);
        boolean cliente = ator.perfil().equals("CLIENTE") && usuarioId.equals(pedido.clienteId())
                && Set.of("APP", "WEB").contains(pedido.canal());
        boolean atendente = ator.perfil().equals("ATENDENTE") && daUnidade(ator, pedido) && pedido.canal().equals("BALCAO");
        if (!cliente && !atendente) throw UsuariosService.erro(403, "ACESSO_NEGADO", "Pagamento não autorizado.");
        if (pedido.status().equals("CANCELADO") || pedido.status().equals("CONCLUIDO"))
            throw UsuariosService.erro(409, "PEDIDO_FINALIZADO", "Pedido finalizado não pode receber pagamento.");
        if (pedido.pagamento().equals("APROVADO"))
            return repositorio.pagamentoAprovado(pedido)
                    .orElseThrow(() -> new IllegalStateException("Pagamento aprovado sem tentativa registrada"));
        UUID tentativa = UUID.randomUUID();
        var solicitacao = new GatewayPagamento.Solicitacao(tentativa, pedidoId, pedido.total());
        GatewayPagamento.Resultado resultado = solicitarComLimite(solicitacao);
        String status = pedido.status();
        if (resultado == GatewayPagamento.Resultado.APROVADO && status.equals("AGUARDANDO_PAGAMENTO")) status = "RECEBIDO";
        repositorio.registrarTentativa(tentativa, pedido, resultado.name());
        repositorio.atualizarPedido(pedidoId, status, resultado.name());
        auditoria.registrar(new Auditoria.Evento(usuarioId, "PAGAR_PEDIDO", "PEDIDO", pedidoId, resultado.name()));
        return new RepositorioFluxoPedido.Tentativa(tentativa, pedidoId, resultado.name(), resultado.name(), status);
    }

    private GatewayPagamento.Resultado solicitarComLimite(GatewayPagamento.Solicitacao solicitacao) {
        var executor = Executors.newSingleThreadExecutor(r -> {
            var thread = new Thread(r, "gateway-pagamento-mock");
            thread.setDaemon(true);
            return thread;
        });
        var tarefa = executor.submit(() -> gateway.solicitar(solicitacao));
        try {
            var resultado = tarefa.get(5, TimeUnit.SECONDS);
            return resultado == null ? GatewayPagamento.Resultado.FALHA : resultado;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return GatewayPagamento.Resultado.FALHA;
        } catch (ExecutionException | TimeoutException e) {
            tarefa.cancel(true);
            return GatewayPagamento.Resultado.FALHA;
        } finally {
            executor.shutdownNow();
        }
    }

    @Transactional
    public Estado atualizarStatus(UUID usuarioId, UUID pedidoId, AlteracaoStatus dados) {
        var ator = ator(usuarioId);
        var pedido = pedido(pedidoId);
        if (!daUnidade(ator, pedido) || !(ator.perfil().equals("COZINHA") || ator.perfil().equals("ATENDENTE")))
            throw UsuariosService.erro(403, "ACESSO_NEGADO", "Operação não autorizada.");
        if (dados == null || dados.status() == null || !Set.of("EM_PREPARO", "PRONTO", "CONCLUIDO").contains(dados.status()))
            throw UsuariosService.erro(400, "DADOS_INVALIDOS", "Status operacional inválido.");
        if ((dados.status().equals("CONCLUIDO") && !ator.perfil().equals("ATENDENTE"))
                || (!dados.status().equals("CONCLUIDO") && !ator.perfil().equals("COZINHA")))
            throw UsuariosService.erro(403, "ACESSO_NEGADO", "Perfil sem permissão para este status.");
        boolean permitido = switch (dados.status()) {
            case "EM_PREPARO" -> ator.perfil().equals("COZINHA") &&
                    ((pedido.status().equals("RECEBIDO") && pedido.pagamento().equals("APROVADO")) ||
                    (pedido.canal().equals("BALCAO") && pedido.status().equals("AGUARDANDO_PAGAMENTO")));
            case "PRONTO" -> ator.perfil().equals("COZINHA") && pedido.status().equals("EM_PREPARO");
            case "CONCLUIDO" -> ator.perfil().equals("ATENDENTE") && pedido.status().equals("PRONTO") && pedido.pagamento().equals("APROVADO");
            default -> false;
        };
        if (!permitido) throw UsuariosService.erro(409, "TRANSICAO_INVALIDA", "Transição de status não permitida.");
        repositorio.atualizarPedido(pedidoId, dados.status(), pedido.pagamento());
        auditoria.registrar(new Auditoria.Evento(usuarioId, "ALTERAR_STATUS", "PEDIDO", pedidoId, dados.status()));
        return new Estado(pedidoId, dados.status(), pedido.pagamento());
    }

    @Transactional
    public Estado cancelar(UUID usuarioId, UUID pedidoId) {
        var ator = ator(usuarioId);
        var pedido = pedido(pedidoId);
        boolean permitido = ator.perfil().equals("ADMINISTRADOR")
                || (ator.perfil().equals("CLIENTE") && usuarioId.equals(pedido.clienteId()))
                || (ator.perfil().equals("ATENDENTE") && daUnidade(ator, pedido));
        if (!permitido) throw UsuariosService.erro(403, "ACESSO_NEGADO", "Cancelamento não autorizado.");
        if (!Set.of("AGUARDANDO_PAGAMENTO", "RECEBIDO").contains(pedido.status()))
            throw UsuariosService.erro(409, "CANCELAMENTO_INVALIDO", "Pedido não pode ser cancelado neste status.");
        repositorio.restituirEstoque(pedidoId);
        String pagamento = pedido.pagamento();
        if (pagamento.equals("APROVADO")) {
            repositorio.registrarEstorno(pedidoId);
            pagamento = "ESTORNADO";
        }
        repositorio.atualizarPedido(pedidoId, "CANCELADO", pagamento);
        auditoria.registrar(new Auditoria.Evento(usuarioId, "CANCELAR_PEDIDO", "PEDIDO", pedidoId, "SUCESSO"));
        return new Estado(pedidoId, "CANCELADO", pagamento);
    }
}
