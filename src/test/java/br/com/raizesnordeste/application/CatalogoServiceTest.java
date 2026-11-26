package br.com.raizesnordeste.application;

import br.com.raizesnordeste.domain.RepositorioCatalogo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CatalogoServiceTest {
    private final UUID unidade = UUID.randomUUID();
    private final UUID produto = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();
    private final RepositorioFake repo = new RepositorioFake();
    private final CatalogoService service = new CatalogoService();

    CatalogoServiceTest() { service.repositorio = repo; service.auditoria = evento -> repo.auditado = true; }

    @Test
    void paginacaoEUnidadeSaoValidadas() {
        assertEquals(400, assertThrows(FalhaAplicacao.class, () -> service.unidades(0, 20)).status());
        assertEquals(400, assertThrows(FalhaAplicacao.class, () -> service.unidades(1, 101)).status());
        assertEquals(404, assertThrows(FalhaAplicacao.class, () -> service.cardapio(UUID.randomUUID(), 1, 20)).status());
        assertEquals(404, assertThrows(FalhaAplicacao.class, () -> service.unidade(UUID.randomUUID())).status());
        assertEquals(unidade, service.unidade(unidade).id());
        assertEquals(0, service.cardapio(unidade, 1, 20).total());
    }

    @Test
    void equipeConsultaSomenteSuaUnidade() {
        service.estoque(usuario, false, null, 1, 20);
        assertEquals(unidade, repo.unidadeConsultada);
        assertEquals(403, assertThrows(FalhaAplicacao.class, () -> service.estoque(usuario, false, UUID.randomUUID(), 1, 20)).status());
        service.estoque(usuario, true, null, 1, 20);
        assertNull(repo.unidadeConsultada);
    }

    @Test
    void entradaESaidaAtualizamSaldoEAuditoria() {
        var entrada = service.movimentar(usuario, new CatalogoService.Movimentacao(produto, "ENTRADA", 5));
        assertEquals(15, entrada.saldoAtual());
        assertTrue(repo.auditado);
        assertEquals(12, service.movimentar(usuario, new CatalogoService.Movimentacao(produto, "SAIDA", 3)).saldoAtual());
        assertEquals(2, repo.movimentos);
    }

    @Test
    void movimentacaoInvalidaNaoAlteraSaldo() {
        assertEquals(400, assertThrows(FalhaAplicacao.class, () -> service.movimentar(usuario, new CatalogoService.Movimentacao(produto, "SAIDA", 0))).status());
        assertEquals(409, assertThrows(FalhaAplicacao.class, () -> service.movimentar(usuario, new CatalogoService.Movimentacao(produto, "SAIDA", 11))).status());
        assertEquals(404, assertThrows(FalhaAplicacao.class, () -> service.movimentar(usuario, new CatalogoService.Movimentacao(UUID.randomUUID(), "SAIDA", 1))).status());
        assertEquals(10, repo.saldo);
        assertEquals(0, repo.movimentos);
        assertFalse(repo.auditado);
    }

    class RepositorioFake implements RepositorioCatalogo {
        int saldo = 10, movimentos; boolean auditado; UUID unidadeConsultada;
        public Pagina<Unidade> unidades(int p, int l) { return new Pagina<>(List.of(), p, l, 0); }
        public Optional<Unidade> unidade(UUID id) { return unidade.equals(id) ? Optional.of(new Unidade(id, "Unidade", "Rua")) : Optional.empty(); }
        public boolean unidadeExiste(UUID id) { return unidade.equals(id); }
        public Pagina<Produto> cardapio(UUID id, int p, int l) { return new Pagina<>(List.of(), p, l, 0); }
        public Optional<UUID> unidadeDoUsuario(UUID id) { return Optional.of(unidade); }
        public Pagina<Saldo> estoques(UUID id, int p, int l) { unidadeConsultada = id; return new Pagina<>(List.of(), p, l, 0); }
        public Optional<Integer> bloquearSaldo(UUID id) { return produto.equals(id) ? Optional.of(saldo) : Optional.empty(); }
        public void atualizarSaldo(UUID id, int novo) { saldo = novo; }
        public void inserirMovimento(UUID id, UUID produto, String tipo, int quantidade) { movimentos++; }
    }
}
