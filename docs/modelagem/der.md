# DER do MVP

[Voltar à modelagem](modelagem.md)

```mermaid
erDiagram
    USUARIO o|--|| UNIDADE : atua_em
    UNIDADE ||--o{ PRODUTO_UNIDADE : oferece
    PRODUTO ||--o{ PRODUTO_UNIDADE : compoe
    PRODUTO_UNIDADE ||--o{ MOVIMENTACAO_ESTOQUE : movimenta
    UNIDADE ||--o{ PEDIDO : recebe
    USUARIO o|--o{ PEDIDO : possui_como_cliente
    USUARIO ||--o{ PEDIDO : cria
    PEDIDO ||--|{ ITEM_PEDIDO : contem
    PRODUTO_UNIDADE ||--o{ ITEM_PEDIDO : identifica
    PEDIDO ||--o{ TENTATIVA_PAGAMENTO : possui
    PEDIDO o|--o{ MOVIMENTACAO_ESTOQUE : origina
    USUARIO o|--o{ EVENTO_AUDITORIA : responsavel

    USUARIO {
        id PK
        nome
        cpf UK
        data_nascimento
        email UK
        senha_hash
        perfil
        unidade_id FK
    }
    UNIDADE {
        id PK
        nome
        endereco
    }
    PRODUTO {
        id PK
        nome
        descricao
    }
    PRODUTO_UNIDADE {
        id PK
        unidade_id FK
        produto_id FK
        preco
        disponivel
        saldo_estoque
    }
    MOVIMENTACAO_ESTOQUE {
        id PK
        produto_unidade_id FK
        pedido_id FK
        tipo
        quantidade
        origem
        criado_em
    }
    PEDIDO {
        id PK
        unidade_id FK
        cliente_id FK
        criador_id FK
        canal_pedido
        status
        situacao_pagamento
        valor_total
        criado_em
    }
    ITEM_PEDIDO {
        id PK
        pedido_id FK
        produto_unidade_id FK
        quantidade
        preco_unitario
        subtotal
    }
    TENTATIVA_PAGAMENTO {
        id PK
        pedido_id FK
        valor_solicitado
        resultado
        falha
        estorno_simulado
        criado_em
    }
    EVENTO_AUDITORIA {
        id PK
        responsavel_id FK
        operacao
        recurso
        recurso_id
        resultado
        criado_em
    }
```

## Restrições principais

| Elemento | Restrição |
|---|---|
| Usuário | CPF e e-mail são únicos. Perfil: Cliente, Atendente, Cozinha ou Administrador. Atendente e Cozinha exigem unidade; Cliente e Administrador não possuem unidade de atuação. |
| ProdutoUnidade | O par unidade/produto é único. Preço e saldo não são negativos. Produto indisponível ou sem saldo não aparece no cardápio disponível. |
| Pedido | `canal_pedido` é obrigatório e admite APP, TOTEM, BALCAO, PICKUP e WEB. APP/WEB exigem Cliente; BALCAO não recebe Cliente. TOTEM e PICKUP são conceituais no MVP. |
| ItemPedido | Pedido possui um ou mais itens; quantidade é inteira positiva. ProdutoUnidade do item deve pertencer à unidade do pedido. Valores são calculados pelo sistema. |
| Estoque | Movimentação tem quantidade positiva, entrada ou saída e referência a pedido quando automática. Operações automáticas não se repetem; saldo não fica negativo. |
| Pagamento | Tentativas registram aprovação, recusa ou falha. Retorno repetido não cria novo pagamento ou mudança de status. Estorno é simulado e depende de cancelamento permitido. |
| Status | AGUARDANDO_PAGAMENTO, RECEBIDO, EM_PREPARO, PRONTO, CONCLUIDO e CANCELADO. As transições obedecem RF06 e RF08. |
| Auditoria | Responsável pode ser nulo em evento sem identificação. Não armazena senha, hash, token completo ou dado pessoal completo. |

O diagrama é a fonte do DER. Antes da entrega final, será exportado como imagem ou PDF compatível com o formato exigido pelo roteiro; as migrations Flyway definirão tipos concretos e nomes físicos sem alterar suas relações e restrições.

Requisitos relacionados: RF01–RF08 e RNF01–RNF04, RNF07.

[Voltar ao topo](#der-do-mvp)
