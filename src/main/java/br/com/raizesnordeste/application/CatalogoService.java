package br.com.raizesnordeste.application;

import br.com.raizesnordeste.domain.Auditoria;
import br.com.raizesnordeste.domain.RepositorioCatalogo;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.UUID;
import java.io.IOException;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@ApplicationScoped
public class CatalogoService {
    @Inject RepositorioCatalogo repositorio;
    @Inject Auditoria auditoria;

    @Schema(requiredProperties = {"produtoUnidadeId", "tipo", "quantidade"})
    public record Movimentacao(UUID produtoUnidadeId, String tipo,
            @JsonDeserialize(using = QuantidadeInteira.class) Integer quantidade) { }

    public static class QuantidadeInteira extends JsonDeserializer<Integer> {
        @Override
        public Integer deserialize(JsonParser parser, DeserializationContext contexto) throws IOException {
            if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT))
                return (Integer) contexto.handleUnexpectedToken(Integer.class, parser);
            return parser.getIntValue();
        }
    }

    private void validarPagina(int pagina, int limite) {
        if (pagina < 1 || limite < 1 || limite > 100 || ((long) pagina - 1) * limite > Integer.MAX_VALUE)
            throw UsuariosService.erro(400, "PAGINACAO_INVALIDA", "Informe página e limite válidos (limite máximo: 100).");
    }

    public RepositorioCatalogo.Pagina<RepositorioCatalogo.Unidade> unidades(int pagina, int limite) {
        validarPagina(pagina, limite);
        return repositorio.unidades(pagina, limite);
    }

    public RepositorioCatalogo.Unidade unidade(UUID id) {
        return repositorio.unidade(id).orElseThrow(() -> UsuariosService.erro(404, "UNIDADE_NAO_ENCONTRADA", "Unidade não encontrada."));
    }

    public RepositorioCatalogo.Pagina<RepositorioCatalogo.Produto> cardapio(UUID id, int pagina, int limite) {
        validarPagina(pagina, limite);
        if (!repositorio.unidadeExiste(id)) throw UsuariosService.erro(404, "UNIDADE_NAO_ENCONTRADA", "Unidade não encontrada.");
        return repositorio.cardapio(id, pagina, limite);
    }

    public RepositorioCatalogo.Pagina<RepositorioCatalogo.Saldo> estoque(UUID usuarioId, boolean administrador,
            UUID unidadeId, int pagina, int limite) {
        validarPagina(pagina, limite);
        if (!administrador) {
            if (unidadeId != null) throw UsuariosService.erro(403, "ACESSO_NEGADO", "Filtro de unidade exclusivo do Administrador.");
            unidadeId = repositorio.unidadeDoUsuario(usuarioId)
                    .orElseThrow(() -> UsuariosService.erro(403, "ACESSO_NEGADO", "Usuário sem unidade de atuação."));
        } else if (unidadeId != null && !repositorio.unidadeExiste(unidadeId)) {
            throw UsuariosService.erro(404, "UNIDADE_NAO_ENCONTRADA", "Unidade não encontrada.");
        }
        return repositorio.estoques(unidadeId, pagina, limite);
    }

    @Transactional
    public RepositorioCatalogo.Movimento movimentar(UUID responsavel, Movimentacao dados) {
        if (dados == null || dados.produtoUnidadeId() == null || dados.quantidade() == null || dados.quantidade() <= 0
                || !("ENTRADA".equals(dados.tipo()) || "SAIDA".equals(dados.tipo())))
            throw UsuariosService.erro(400, "DADOS_INVALIDOS", "Informe produto, tipo e quantidade positiva.");
        int anterior = repositorio.bloquearSaldo(dados.produtoUnidadeId())
                .orElseThrow(() -> UsuariosService.erro(404, "PRODUTO_UNIDADE_NAO_ENCONTRADO", "Produto da unidade não encontrado."));
        long novo = (long) anterior + ("ENTRADA".equals(dados.tipo()) ? dados.quantidade() : -(long) dados.quantidade());
        if (novo < 0 || novo > Integer.MAX_VALUE) throw UsuariosService.erro(409, "SALDO_INVALIDO", "Saldo insuficiente ou acima do limite.");
        UUID id = UUID.randomUUID();
        repositorio.atualizarSaldo(dados.produtoUnidadeId(), (int) novo);
        repositorio.inserirMovimento(id, dados.produtoUnidadeId(), dados.tipo(), dados.quantidade());
        auditoria.registrar(new Auditoria.Evento(responsavel, "MOVIMENTAR_ESTOQUE", "MOVIMENTACAO_ESTOQUE", id, "SUCESSO"));
        return new RepositorioCatalogo.Movimento(id, (int) novo);
    }
}
