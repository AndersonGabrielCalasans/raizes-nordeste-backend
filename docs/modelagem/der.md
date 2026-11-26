# DER do MVP

[Voltar à modelagem](modelagem.md)

O [DER em imagem](der-raizes-nordeste.png) apresenta as entidades e relações do banco.

```mermaid
erDiagram
    USUARIO o{--o| UNIDADE : atua_em
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
        uuid id PK
        string nome
        string cpf UK
        date data_nascimento
        string email UK
        string senha_hash
        string perfil
        uuid unidade_id FK
    }
    UNIDADE {
        uuid id PK
        string nome
        string endereco
    }
    PRODUTO {
        uuid id PK
        string nome
        string descricao
    }
    PRODUTO_UNIDADE {
        uuid id PK
        uuid unidade_id FK
        uuid produto_id FK
        decimal preco
        boolean disponivel
        integer saldo_estoque
    }
    MOVIMENTACAO_ESTOQUE {
        uuid id PK
        uuid produto_unidade_id FK
        uuid pedido_id FK
        string tipo
        integer quantidade
        string origem
        datetime criado_em
    }
    PEDIDO {
        uuid id PK
        uuid unidade_id FK
        uuid cliente_id FK
        uuid criador_id FK
        string canal_pedido
        string status
        string situacao_pagamento
        decimal valor_total
        datetime criado_em
    }
    ITEM_PEDIDO {
        uuid id PK
        uuid pedido_id FK
        uuid produto_unidade_id FK
        integer quantidade
        decimal preco_unitario
        decimal subtotal
    }
    TENTATIVA_PAGAMENTO {
        uuid id PK
        uuid pedido_id FK
        decimal valor_solicitado
        string resultado
        string falha
        boolean estorno_simulado
        datetime criado_em
    }
    EVENTO_AUDITORIA {
        uuid id PK
        uuid responsavel_id FK
        string operacao
        string recurso
        uuid recurso_id
        string resultado
        datetime criado_em
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

O diagrama representa as relações do esquema criado pelas migrations Flyway. A exigência de pelo menos um item e a correspondência entre a unidade do pedido e a do produto são validadas pela aplicação; não são restrições independentes do PostgreSQL. CPF e data de nascimento são obrigatórios no cadastro pela API, embora as colunas aceitem nulos no esquema.

Requisitos relacionados: RF01–RF08 e RNF01–RNF04, RNF07.

[Voltar ao topo](#der-do-mvp)
