# Sequência do fluxo crítico

[Voltar à modelagem](modelagem.md)

```mermaid
sequenceDiagram
    actor Solicitante as Cliente ou Atendente
    participant API
    participant Estoque
    participant Gateway as Adaptador mock local
    participant Cozinha

    Solicitante->>API: criar pedido com canal e itens
    API->>Estoque: validar e baixar saldo
    Estoque-->>API: saldo confirmado
    API-->>Solicitante: pedido AGUARDANDO_PAGAMENTO
    opt BALCAO antes do pagamento
        Cozinha->>API: iniciar preparo na unidade
    end
    Solicitante->>API: solicitar pagamento
    API->>Gateway: enviar valor calculado
    Gateway-->>API: aprovado, recusado ou falha
    alt pagamento aprovado
        API->>API: atualizar para RECEBIDO quando aguardando pagamento
    else pagamento recusado ou falha
        API->>API: preservar status operacional
    end
    API-->>Solicitante: resultado registrado
    opt pagamento aprovado
        opt preparo ainda não iniciado
            Cozinha->>API: iniciar preparo
        end
        Cozinha->>API: marcar PRONTO após preparo
        API-->>Solicitante: pedido concluível pelo Atendente
    end
```

No BALCAO, o preparo pode ocorrer antes da solicitação de pagamento; se já iniciado, a aprovação não retrocede o status. O adaptador é executado na própria API, sem chamada HTTP externa. Recusa ou falha não aprovam o pedido, e repetir uma solicitação já aprovada não duplica efeitos. Cancelamento permitido devolve estoque uma única vez e registra estorno simulado quando aplicável.

[Voltar ao topo](#sequência-do-fluxo-crítico)
