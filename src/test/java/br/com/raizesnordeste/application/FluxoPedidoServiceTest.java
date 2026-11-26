package br.com.raizesnordeste.application;

import br.com.raizesnordeste.domain.GatewayPagamento;
import br.com.raizesnordeste.domain.RepositorioFluxoPedido;
import br.com.raizesnordeste.domain.RepositorioPedidos;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FluxoPedidoServiceTest {
    final UUID cliente = UUID.randomUUID(), atendente = UUID.randomUUID(), cozinha = UUID.randomUUID();
    final UUID unidade = UUID.randomUUID(), pedidoId = UUID.randomUUID();
    final Fake repo = new Fake();
    final PedidosServiceTest.Fake usuarios = new PedidosServiceTest().new Fake();
    final FluxoPedidoService service = new FluxoPedidoService();
    final AtomicInteger chamadas = new AtomicInteger();

    FluxoPedidoServiceTest() {
        usuarios.atores.put(cliente, new RepositorioPedidos.Ator(cliente, "CLIENTE", null));
        usuarios.atores.put(atendente, new RepositorioPedidos.Ator(atendente, "ATENDENTE", unidade));
        usuarios.atores.put(cozinha, new RepositorioPedidos.Ator(cozinha, "COZINHA", unidade));
        service.usuarios = usuarios;
        service.repositorio = repo;
        service.auditoria = e -> repo.auditorias++;
        service.gateway = s -> { chamadas.incrementAndGet(); assertEquals(new BigDecimal("25.00"), s.valor()); return GatewayPagamento.Resultado.APROVADO; };
    }

    @Test void aprovacaoAvancaPedidoERepeticaoNaoDuplica() {
        var primeira = service.pagar(cliente, pedidoId);
        var repetida = service.pagar(cliente, pedidoId);
        assertEquals("RECEBIDO", primeira.statusPedido());
        assertEquals(primeira.tentativaId(), repetida.tentativaId());
        assertEquals(1, repo.tentativas); assertEquals(1, chamadas.get()); assertEquals(1, repo.auditorias);
    }

    @Test void recusaEFalhaPermitemNovaTentativa() {
        service.gateway = s -> GatewayPagamento.Resultado.RECUSADO;
        assertEquals("RECUSADO", service.pagar(cliente, pedidoId).resultado());
        assertEquals("AGUARDANDO_PAGAMENTO", repo.pedido.status());
        service.gateway = s -> { throw new IllegalStateException("gateway indisponível"); };
        assertEquals("FALHA", service.pagar(cliente, pedidoId).resultado());
        assertEquals(2, repo.tentativas); assertEquals("AGUARDANDO_PAGAMENTO", repo.pedido.status());
    }

    @Test void tempoLimiteRegistraFalhaSemAvancar() {
        service.gateway = s -> { Thread.sleep(10000); return GatewayPagamento.Resultado.APROVADO; };
        assertEquals("FALHA", service.pagar(cliente, pedidoId).resultado());
        assertEquals("AGUARDANDO_PAGAMENTO", repo.pedido.status());
        assertEquals(1, repo.tentativas);
    }

    @Test void pagamentoNaoAutorizadoNaoChamaGateway() {
        UUID outro = UUID.randomUUID();
        usuarios.atores.put(outro, new RepositorioPedidos.Ator(outro, "CLIENTE", null));
        assertEquals(403, assertThrows(FalhaAplicacao.class, () -> service.pagar(outro, pedidoId)).status());
        assertEquals(0, chamadas.get()); assertEquals(0, repo.tentativas);
    }

    @Test void balcaoPodePrepararAntesDePagarSemRegredirStatus() {
        repo.pedido = repo.com("BALCAO", "AGUARDANDO_PAGAMENTO", "PENDENTE", null);
        assertEquals("EM_PREPARO", service.atualizarStatus(cozinha, pedidoId, new FluxoPedidoService.AlteracaoStatus("EM_PREPARO")).status());
        assertEquals("EM_PREPARO", service.pagar(atendente, pedidoId).statusPedido());
        assertEquals("PRONTO", service.atualizarStatus(cozinha, pedidoId, new FluxoPedidoService.AlteracaoStatus("PRONTO")).status());
        assertEquals("CONCLUIDO", service.atualizarStatus(atendente, pedidoId, new FluxoPedidoService.AlteracaoStatus("CONCLUIDO")).status());
    }

    @Test void preparacaoDeAppEConclusaoExigemPagamento() {
        assertEquals(409, assertThrows(FalhaAplicacao.class, () -> service.atualizarStatus(cozinha, pedidoId, new FluxoPedidoService.AlteracaoStatus("EM_PREPARO"))).status());
        repo.pedido = repo.com("BALCAO", "PRONTO", "PENDENTE", null);
        assertEquals(409, assertThrows(FalhaAplicacao.class, () -> service.atualizarStatus(atendente, pedidoId, new FluxoPedidoService.AlteracaoStatus("CONCLUIDO"))).status());
    }

    @Test void perfilIncompativelRecebeProibicaoSemAlterarStatus() {
        assertEquals(403, assertThrows(FalhaAplicacao.class, () -> service.atualizarStatus(atendente, pedidoId, new FluxoPedidoService.AlteracaoStatus("EM_PREPARO"))).status());
        assertEquals(403, assertThrows(FalhaAplicacao.class, () -> service.atualizarStatus(cozinha, pedidoId, new FluxoPedidoService.AlteracaoStatus("CONCLUIDO"))).status());
        assertEquals("AGUARDANDO_PAGAMENTO", repo.pedido.status());
    }

    @Test void cancelamentoPagoEstornaERestituiUmaUnicaVez() {
        service.pagar(cliente, pedidoId);
        var cancelado = service.cancelar(cliente, pedidoId);
        assertEquals("CANCELADO", cancelado.status()); assertEquals("ESTORNADO", cancelado.situacaoPagamento());
        assertEquals(1, repo.restituicoes); assertEquals(1, repo.estornos);
        assertEquals(409, assertThrows(FalhaAplicacao.class, () -> service.cancelar(cliente, pedidoId)).status());
        assertEquals(409, assertThrows(FalhaAplicacao.class, () -> service.pagar(cliente, pedidoId)).status());
        assertEquals(1, repo.restituicoes); assertEquals(1, repo.estornos);
    }

    @Test void cancelamentoEmPreparoNaoAlteraEstoque() {
        repo.pedido = repo.com("APP", "EM_PREPARO", "APROVADO", cliente);
        assertEquals(409, assertThrows(FalhaAplicacao.class, () -> service.cancelar(cliente, pedidoId)).status());
        assertEquals(0, repo.restituicoes); assertEquals(0, repo.estornos);
    }

    class Fake implements RepositorioFluxoPedido {
        Pedido pedido = com("APP", "AGUARDANDO_PAGAMENTO", "PENDENTE", cliente);
        UUID ultimaAprovada; int tentativas, restituicoes, estornos, auditorias;
        Pedido com(String canal, String status, String pagamento, UUID dono) { return new Pedido(pedidoId, unidade, dono, canal, status, pagamento, new BigDecimal("25.00")); }
        public Optional<Pedido> bloquearPedido(UUID id) { return pedidoId.equals(id) ? Optional.of(pedido) : Optional.empty(); }
        public Optional<Tentativa> pagamentoAprovado(Pedido p) { return Optional.ofNullable(ultimaAprovada).map(id -> new Tentativa(id,p.id(),"APROVADO",p.pagamento(),p.status())); }
        public void registrarTentativa(UUID id, Pedido p, String resultado) { tentativas++; if (resultado.equals("APROVADO")) ultimaAprovada=id; }
        public void atualizarPedido(UUID id, String status, String pagamento) { pedido=com(pedido.canal(),status,pagamento,pedido.clienteId()); }
        public void restituirEstoque(UUID id) { restituicoes++; }
        public void registrarEstorno(UUID id) { estornos++; }
    }
}
