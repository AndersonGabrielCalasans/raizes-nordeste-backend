# Modelagem do domínio

[Voltar ao índice da documentação](../README.md)

## Índice

- [DER do MVP](#der-do-mvp)
- [Restrições principais](#restrições-principais)
- [Cobertura conceitual](#cobertura-conceitual)

## DER do MVP

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
        unidade_id FK "nulo para Cliente e Administrador"
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
        pedido_id FK "nulo em movimento manual"
        tipo
        quantidade
        origem
        criado_em
    }
    PEDIDO {
        id PK
        unidade_id FK
        cliente_id FK "nulo para BALCAO"
        criador_id FK
        canal
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
        responsavel_id FK "nulo se nao identificado"
        operacao
        recurso
        recurso_id
        resultado
        criado_em
    }
```

O diagrama representa o modelo físico planejado. Os identificadores, tipos concretos, nomes de colunas e enums serão definidos pelas migrations Flyway; as relações e restrições abaixo devem ser preservadas.

## Restrições principais

| Elemento | Restrição |
|---|---|
| Usuário | CPF e e-mail são únicos. `perfil` admite Cliente, Atendente, Cozinha ou Administrador. Atendente e Cozinha exigem unidade; Cliente e Administrador não recebem unidade de atuação. |
| ProdutoUnidade | O par `unidade_id` e `produto_id` é único. Preço não é negativo e saldo não pode ser negativo. Produto indisponível ou com saldo zero não integra o cardápio disponível. |
| Pedido | `canal` admite APP, WEB ou BALCAO. APP/WEB exigem `cliente_id`; BALCAO não o recebe. `criador_id`, unidade, canal, status, situação de pagamento, total e data/hora são obrigatórios. |
| ItemPedido | Todo pedido possui ao menos um item. Quantidade é inteira positiva; preço unitário, subtotal e total não são recebidos como valores livres do solicitante. O produto da unidade deve pertencer à unidade do pedido. |
| MovimentaçãoEstoque | Quantidade é inteira positiva. Entrada e saída identificam a variação; movimentações automáticas referenciam o pedido e não podem ser repetidas para a mesma operação. Saídas não reduzem o saldo abaixo de zero. |
| TentativaPagamento | Registra aprovação, recusa ou falha. O mesmo retorno não pode produzir novo pagamento aprovado nem nova mudança de status; estorno é somente simulado e decorre de cancelamento permitido de pedido pago. |
| Pedido e status | `status` admite AGUARDANDO_PAGAMENTO, RECEBIDO, EM_PREPARO, PRONTO, CONCLUIDO ou CANCELADO. As transições, cancelamento e efeitos de pagamento obedecem RF06 e RF08. |
| Auditoria | Responsável é opcional para eventos sem identificação. O registro não armazena senha, hash, token completo ou dado pessoal completo. |

O saldo será alterado juntamente com a criação ou o cancelamento permitido do pedido, sem registros parciais. A unicidade de operações automáticas e a consistência entre item e unidade do pedido serão implementadas nas migrations e na camada de serviço.

Requisitos relacionados: RF01–RF08 e RNF01–RNF04, RNF07.

[Voltar ao topo](#modelagem-do-domínio)

## Cobertura conceitual

TOTEM, fidelidade, campanhas, Owner, múltiplos perfis e vínculo posterior de pedido BALCAO a Cliente permanecem conceituais. Não possuem tabelas no DER do MVP.

[Voltar ao topo](#modelagem-do-domínio)
