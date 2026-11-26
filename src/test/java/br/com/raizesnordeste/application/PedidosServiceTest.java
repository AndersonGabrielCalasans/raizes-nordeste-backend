package br.com.raizesnordeste.application;

import br.com.raizesnordeste.domain.RepositorioPedidos;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PedidosServiceTest {
    final UUID cliente = UUID.randomUUID(), outro = UUID.randomUUID(), unidade = UUID.randomUUID(), produto = UUID.randomUUID();
    final Fake repo = new Fake();
    final PedidosService service = new PedidosService();
    PedidosServiceTest() { service.repositorio=repo; service.auditoria=e -> repo.auditado=true; }
    PedidosService.Criacao entrada(String canal, int quantidade) {
        return new PedidosService.Criacao(unidade,canal,List.of(new PedidosService.ItemEntrada(produto,quantidade)));
    }

    @Test void clienteCriaPedidoComPrecoServidorEAuditoria() {
        var p=service.criar(cliente,entrada("APP",2));
        assertEquals(cliente,p.clienteId()); assertEquals("AGUARDANDO_PAGAMENTO",p.status());
        assertEquals(new BigDecimal("25.00"),p.valorTotal()); assertEquals(8,repo.saldo); assertTrue(repo.auditado);
    }
    @Test void itensRepetidosSaoSomadosAntesDeValidar() {
        var dados=new PedidosService.Criacao(unidade,"WEB",List.of(new PedidosService.ItemEntrada(produto,6),new PedidosService.ItemEntrada(produto,5)));
        assertEquals(409,assertThrows(FalhaAplicacao.class,() -> service.criar(cliente,dados)).status());
        assertNull(repo.pedido); assertEquals(10,repo.saldo); assertFalse(repo.auditado);
    }
    @Test void recusaValoresCanalPerfilEEstoqueInvalidos() {
        assertEquals(400,assertThrows(FalhaAplicacao.class,() -> service.criar(cliente,entrada("TOTEM",1))).status());
        assertEquals(400,assertThrows(FalhaAplicacao.class,() -> service.criar(cliente,entrada("APP",0))).status());
        assertEquals(403,assertThrows(FalhaAplicacao.class,() -> service.criar(cliente,entrada("BALCAO",1))).status());
        assertEquals(409,assertThrows(FalhaAplicacao.class,() -> service.criar(cliente,entrada("APP",11))).status());
        assertNull(repo.pedido); assertEquals(10,repo.saldo);
    }
    @Test void produtoInexistenteNaoGeraPedidoNemBaixa() {
        var dados = new PedidosService.Criacao(unidade,"APP",List.of(new PedidosService.ItemEntrada(produto,1),
                new PedidosService.ItemEntrada(UUID.randomUUID(),1)));
        assertEquals(404,assertThrows(FalhaAplicacao.class,() -> service.criar(cliente,dados)).status());
        assertNull(repo.pedido); assertEquals(10,repo.saldo); assertFalse(repo.auditado);
    }
    @Test void atendenteCriaBalcaoApenasNaPropriaUnidade() {
        repo.atores.put(outro,new RepositorioPedidos.Ator(outro,"ATENDENTE",unidade));
        var p=service.criar(outro,entrada("BALCAO",1)); assertNull(p.clienteId()); assertEquals(outro,p.criadorId());
        var outra=new PedidosService.Criacao(UUID.randomUUID(),"BALCAO",entrada("BALCAO",1).itens());
        assertEquals(403,assertThrows(FalhaAplicacao.class,() -> service.criar(outro,outra)).status());
    }
    @Test void propriedadeEVisibilidadeDaCozinhaSaoRespeitadas() {
        var p=service.criar(cliente,entrada("APP",1));
        repo.atores.put(outro,new RepositorioPedidos.Ator(outro,"CLIENTE",null));
        assertEquals(403,assertThrows(FalhaAplicacao.class,() -> service.buscar(outro,p.id())).status());
        repo.atores.put(outro,new RepositorioPedidos.Ator(outro,"COZINHA",unidade));
        assertEquals(403,assertThrows(FalhaAplicacao.class,() -> service.buscar(outro,p.id())).status());
        repo.pedido=new RepositorioPedidos.Pedido(p.id(),p.unidadeId(),p.clienteId(),p.criadorId(),"BALCAO",p.status(),p.situacaoPagamento(),p.valorTotal(),p.itens());
        assertEquals(p.id(),service.buscar(outro,p.id()).id());
        repo.pedido=new RepositorioPedidos.Pedido(p.id(),p.unidadeId(),p.clienteId(),p.criadorId(),"APP",p.status(),"APROVADO",p.valorTotal(),p.itens());
        assertEquals(p.id(),service.buscar(outro,p.id()).id());
    }
    @Test void filtrosEValoresDePaginacaoSaoValidados() {
        assertEquals(400,assertThrows(FalhaAplicacao.class,() -> service.listar(cliente,"INVALIDO",null,1,20)).status());
        assertEquals(400,assertThrows(FalhaAplicacao.class,() -> service.listar(cliente,null,"INVALIDO",1,20)).status());
        assertEquals(400,assertThrows(FalhaAplicacao.class,() -> service.listar(cliente,null,null,0,20)).status());
        service.listar(cliente,"APP",null,1,20); assertEquals(cliente,repo.ultimoAtor.id());
    }

    class Fake implements RepositorioPedidos {
        Map<UUID,Ator> atores=new HashMap<>(); Pedido pedido; int saldo=10; boolean auditado; Ator ultimoAtor;
        Fake() { atores.put(cliente,new Ator(cliente,"CLIENTE",null)); }
        public Optional<Ator> ator(UUID id) { return Optional.ofNullable(atores.get(id)); }
        public boolean unidadeExiste(UUID id) { return unidade.equals(id); }
        public Optional<Produto> bloquearProduto(UUID id) { return produto.equals(id) ? Optional.of(new Produto(id,unidade,new BigDecimal("12.50"),true,saldo)) : Optional.empty(); }
        public void gravar(Pedido p) { pedido=p; }
        public void baixar(UUID id,UUID pedidoId,int qtd) { saldo-=qtd; }
        public Optional<Pedido> buscar(UUID id) { return pedido!=null && pedido.id().equals(id) ? Optional.of(pedido) : Optional.empty(); }
        public Pagina listar(Ator ator,String canal,String status,int pagina,int limite) { ultimoAtor=ator; return new Pagina(List.of(),pagina,limite,0); }
    }
}
