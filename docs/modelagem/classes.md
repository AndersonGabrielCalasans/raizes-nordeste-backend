# Diagrama de classes

[Voltar à modelagem](modelagem.md)

```mermaid
classDiagram
    class Usuario {
        +id
        +nome
        +perfil
        +autenticar()
    }
    class Unidade {
        +id
        +nome
    }
    class Produto {
        +id
        +nome
    }
    class ProdutoUnidade {
        +preco
        +disponivel
        +saldoEstoque
        +baixarEstoque()
        +reporEstoque()
    }
    class Pedido {
        +canalPedido
        +status
        +situacaoPagamento
        +valorTotal
        +cancelar()
        +atualizarStatus()
    }
    class ItemPedido {
        +quantidade
        +precoUnitario
        +subtotal
    }
    class MovimentacaoEstoque {
        +tipo
        +quantidade
        +origem
    }
    class TentativaPagamento {
        +valorSolicitado
        +resultado
        +falha
        +registrarRetorno()
    }
    class EventoAuditoria {
        +operacao
        +recurso
        +resultado
    }

    Unidade "1" --> "0..*" ProdutoUnidade
    Produto "1" --> "0..*" ProdutoUnidade
    ProdutoUnidade "1" --> "0..*" MovimentacaoEstoque
    Unidade "1" --> "0..*" Pedido
    Usuario "0..1 cliente" --> "0..*" Pedido
    Usuario "1 criador" --> "0..*" Pedido
    Pedido "1" *-- "1..*" ItemPedido
    ProdutoUnidade "1" --> "0..*" ItemPedido
    Pedido "1" --> "0..*" TentativaPagamento
    Usuario "0..1" --> "0..*" EventoAuditoria
```

O diagrama mostra a visão de domínio. Perfil, canal, status e resultado de pagamento são valores controlados; persistência e detalhes de integração permanecem fora das classes de domínio.

[Voltar ao topo](#diagrama-de-classes)
