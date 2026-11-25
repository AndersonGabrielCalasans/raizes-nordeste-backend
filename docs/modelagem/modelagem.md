# Modelagem e diagramas

[Voltar ao índice da documentação](../README.md)

Esta seção reúne os diagramas da M01-004. Eles descrevem o domínio aprovado, sem transformar funcionalidades conceituais em escopo de implementação.

| Artefato | Conteúdo |
|---|---|
| [Casos de uso e feature crítica](casos-de-uso.md) | Atores exigidos, funcionalidades do sistema e descrição do fluxo Pedido → Pagamento mock → Status. |
| [DER](der.md) | Entidades, atributos principais, PK/FK, cardinalidades e restrições do banco PostgreSQL. |
| [Diagrama de classes](classes.md) | Visão de domínio das entidades e seus relacionamentos. |
| [Sequência do fluxo crítico](sequencia.md) | Interação recomendada entre canal, API, estoque, pagamento mock e cozinha. |

O roteiro também exige arquitetura em camadas. Essa documentação e os contratos da API pertencem à M01-005, atividade já planejada para arquitetura e contratos, antes do início da implementação.

## Cobertura

O MVP implementará cadastro/autenticação, unidades e cardápio, estoque, pedidos APP/WEB/BALCAO, pagamento mock, status, cancelamento e auditoria. TOTEM e PICKUP aparecem como canais de domínio, mas permanecem conceituais neste recorte. Fidelidade, campanhas, Owner, múltiplos perfis e vínculo posterior de pedido BALCAO a Cliente também permanecem conceituais.

Requisitos relacionados: RF01–RF10 e RNF01–RNF08.

[Voltar ao topo](#modelagem-e-diagramas)
