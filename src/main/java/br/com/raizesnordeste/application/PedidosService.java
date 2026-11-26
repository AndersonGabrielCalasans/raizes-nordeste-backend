package br.com.raizesnordeste.application;

import br.com.raizesnordeste.domain.Auditoria;
import br.com.raizesnordeste.domain.RepositorioPedidos;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@ApplicationScoped
public class PedidosService {
    @Inject
    RepositorioPedidos repositorio;
    @Inject
    Auditoria auditoria;

    @Schema(requiredProperties = { "produtoUnidadeId", "quantidade" })
    public record ItemEntrada(UUID produtoUnidadeId,
            @JsonDeserialize(using = CatalogoService.QuantidadeInteira.class) Integer quantidade) {
    }

    @Schema(requiredProperties = { "unidadeId", "canalPedido", "itens" })
    public record Criacao(UUID unidadeId, String canalPedido, List<ItemEntrada> itens) {
    }

    private RepositorioPedidos.Ator ator(UUID id) {
        return repositorio.ator(id)
                .orElseThrow(() -> UsuariosService.erro(403, "ACESSO_NEGADO", "Usuário sem acesso."));
    }

    @Transactional
    public RepositorioPedidos.Pedido criar(UUID usuarioId, Criacao dados) {
        var ator = ator(usuarioId);
        if (dados == null || dados.unidadeId() == null || dados.canalPedido() == null
                || !Set.of("APP", "WEB", "BALCAO").contains(dados.canalPedido())
                || dados.itens() == null || dados.itens().isEmpty())
            throw UsuariosService.erro(400, "DADOS_INVALIDOS", "Unidade, canal ou itens inválidos.");
        if (!(ator.perfil().equals("CLIENTE") && Set.of("APP", "WEB").contains(dados.canalPedido()))
                && !(ator.perfil().equals("ATENDENTE") && dados.canalPedido().equals("BALCAO")))
            throw UsuariosService.erro(403, "ACESSO_NEGADO", "Perfil não autorizado para o canal.");
        if (ator.perfil().equals("ATENDENTE") && !dados.unidadeId().equals(ator.unidadeId()))
            throw UsuariosService.erro(403, "ACESSO_NEGADO", "Unidade não autorizada.");
        if (!repositorio.unidadeExiste(dados.unidadeId()))
            throw UsuariosService.erro(404, "UNIDADE_NAO_ENCONTRADA", "Unidade não encontrada.");
        var quantidades = new TreeMap<UUID, Integer>();
        for (var item : dados.itens()) {
            if (item == null || item.produtoUnidadeId() == null || item.quantidade() == null || item.quantidade() <= 0)
                throw UsuariosService.erro(400, "DADOS_INVALIDOS",
                        "Produtos inválidos ou quantidades inválidas; as quantidades devem ser inteiras e positivas.");
            try {
                quantidades.merge(item.produtoUnidadeId(), item.quantidade(), Math::addExact);
            } catch (ArithmeticException e) {
                throw UsuariosService.erro(400, "DADOS_INVALIDOS", "Quantidade acima do limite.");
            }
        }
        var itens = new ArrayList<RepositorioPedidos.Item>();
        BigDecimal total = BigDecimal.ZERO;
        for (var entrada : quantidades.entrySet()) {
            var produto = repositorio.bloquearProduto(entrada.getKey())
                    .orElseThrow(() -> UsuariosService.erro(404, "PRODUTO_NAO_ENCONTRADO",
                            "Produto da unidade não encontrado."));
            if (!produto.unidadeId().equals(dados.unidadeId()))
                throw UsuariosService.erro(404, "PRODUTO_NAO_ENCONTRADO", "Produto da unidade não encontrado.");
            if (!produto.disponivel() || produto.saldo() < entrada.getValue())
                throw UsuariosService.erro(409, "ESTOQUE_INSUFICIENTE",
                        "Produto indisponível ou estoque insuficiente.");
            BigDecimal subtotal = produto.preco().multiply(BigDecimal.valueOf(entrada.getValue()));
            total = total.add(subtotal);
            itens.add(new RepositorioPedidos.Item(produto.id(), entrada.getValue(), produto.preco(), subtotal));
        }
        if (total.compareTo(new BigDecimal("9999999999.99")) > 0)
            throw UsuariosService.erro(400, "DADOS_INVALIDOS", "Valor do pedido acima do limite.");
        UUID id = UUID.randomUUID();
        var pedido = new RepositorioPedidos.Pedido(id, dados.unidadeId(),
                ator.perfil().equals("CLIENTE") ? usuarioId : null,
                usuarioId, dados.canalPedido(), "AGUARDANDO_PAGAMENTO", "PENDENTE", total, List.copyOf(itens));
        repositorio.gravar(pedido);
        for (var item : itens)
            repositorio.baixar(item.produtoUnidadeId(), id, item.quantidade());
        auditoria.registrar(new Auditoria.Evento(usuarioId, "CRIAR_PEDIDO", "PEDIDO", id, "SUCESSO"));
        return pedido;
    }

    public RepositorioPedidos.Pedido buscar(UUID usuarioId, UUID id) {
        var ator = ator(usuarioId);
        var pedido = repositorio.buscar(id)
                .orElseThrow(() -> UsuariosService.erro(404, "PEDIDO_NAO_ENCONTRADO", "Pedido não encontrado."));
        if (!podeConsultar(ator, pedido))
            throw UsuariosService.erro(403, "ACESSO_NEGADO", "Pedido não disponível para consulta.");
        return pedido;
    }

    public RepositorioPedidos.Pagina listar(UUID usuarioId, String canal, String status, int pagina, int limite) {
        if (pagina < 1 || limite < 1 || limite > 100 || ((long) pagina - 1) * limite > Integer.MAX_VALUE)
            throw UsuariosService.erro(400, "PAGINACAO_INVALIDA", "Paginação inválida.");
        if (canal != null && !Set.of("APP", "WEB", "BALCAO", "TOTEM", "PICKUP").contains(canal))
            throw UsuariosService.erro(400, "DADOS_INVALIDOS", "Canal inválido.");
        if (status != null
                && !Set.of("AGUARDANDO_PAGAMENTO", "RECEBIDO", "EM_PREPARO", "PRONTO", "CONCLUIDO", "CANCELADO")
                        .contains(status))
            throw UsuariosService.erro(400, "DADOS_INVALIDOS", "Status inválido.");
        return repositorio.listar(ator(usuarioId), canal, status, pagina, limite);
    }

    private boolean podeConsultar(RepositorioPedidos.Ator ator, RepositorioPedidos.Pedido pedido) {
        return switch (ator.perfil()) {
            case "ADMINISTRADOR" -> true;
            case "CLIENTE" -> ator.id().equals(pedido.clienteId());
            case "ATENDENTE" -> pedido.unidadeId().equals(ator.unidadeId());
            case "COZINHA" -> pedido.unidadeId().equals(ator.unidadeId())
                    && (pedido.canalPedido().equals("BALCAO") || pedido.situacaoPagamento().equals("APROVADO"));
            default -> false;
        };
    }
}
