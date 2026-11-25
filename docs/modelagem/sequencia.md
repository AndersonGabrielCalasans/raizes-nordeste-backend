# Sequência do fluxo crítico

[Voltar à modelagem](modelagem.md)

```mermaid
sequenceDiagram
    actor Solicitante as Cliente ou Atendente
    participant API
    participant Estoque
    participant Gateway as Pagamento mock
    participant Cozinha

    Solicitante->>API: criar pedido com canal e itens
    API->>Estoque: validar e baixar saldo
    Estoque-->>API: saldo confirmado
    API-->>Solicitante: pedido AGUARDANDO_PAGAMENTO
    Solicitante->>API: solicitar pagamento
    API->>Gateway: enviar valor calculado
    Gateway-->>API: aprovado, recusado ou falha
    API-->>Solicitante: resultado registrado
    alt pagamento aprovado
        API->>API: atualizar para RECEBIDO
        Cozinha->>API: iniciar preparo e marcar PRONTO
        API-->>Solicitante: pedido concluível pelo Atendente
    else BALCAO antes de pagamento
        Cozinha->>API: iniciar preparo
        API-->>Solicitante: status operacional preservado
    end
```

Recusa, falha ou retorno repetido não duplicam efeitos. Cancelamento permitido devolve estoque uma única vez e registra estorno simulado quando aplicável.

[Voltar ao topo](#sequência-do-fluxo-crítico)
