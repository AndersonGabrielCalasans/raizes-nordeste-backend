# Documentação

Esta seção reúne a documentação técnica do backend da rede Raízes do Nordeste. Os documentos apresentam os comportamentos do sistema, suas regras e os limites da implementação, com base no estudo de caso e no roteiro da trilha Back-End.

## Índice

- [Análise e requisitos](#análise-e-requisitos)
- [MVP](#mvp)
- [Permissões principais](#permissões-principais)
- [Fluxo crítico](#fluxo-crítico)
- [Modelagem principal](#modelagem-principal)

## Análise e requisitos

| Documento | Conteúdo |
|---|---|
| [Requisitos funcionais](requisitos-funcionais/requisitos-funcionais.md) | Funcionalidades, regras, critérios de aceitação e cobertura do MVP, incluindo a justificativa do fluxo principal. |
| [Requisitos não funcionais](requisitos-nao-funcionais/requisitos-nao-funcionais.md) | Regras de qualidade e segurança, critérios de verificação e cobertura do MVP. |

## MVP

O MVP contempla o fluxo Pedido → Pagamento mock → Status, por representar a operação principal da rede e conectar autenticação, cardápio, estoque e atendimento.

| Cobertura | Escopo |
|---|---|
| MVP | Cadastro e autenticação; autorização por perfil, unidade e propriedade; consulta de unidades/cardápio; estoque simples; pedidos APP/WEB/BALCAO; pagamento mock; status, cancelamento e auditoria. |
| Conceitual | Gestão administrativa de unidades/produtos, TOTEM, fidelidade, campanhas, Owner, múltiplos perfis e vínculo de pedido BALCAO ao Cliente. |
| Base técnica | Quarkus, persistência real em PostgreSQL, migrations Flyway, DER compatível com o banco, execução local e OpenAPI/Swagger. |

Unidades, produtos, preços e associações iniciais ao cardápio serão provisionados no ambiente. O recorte permite concluir e testar o fluxo principal sem implementar os módulos conceituais. Regras detalhadas: RF01–RF10 e RNF01–RNF08 nos documentos acima.

[Voltar ao topo](#documentação)

## Permissões principais

Consultas de unidades/cardápio, cadastro público de Cliente e login dispensam token. Operações protegidas exigem JWT válido. Cada cadastro possui um perfil no MVP, sem herança automática de permissões.

| Perfil | Operações e limites |
|---|---|
| Cliente | Cria pedidos APP/WEB; consulta, paga e cancela somente os próprios pedidos, conforme as regras. |
| Atendente | Cria e solicita pagamento de BALCAO; consulta, cancela e conclui pedidos da unidade vinculada, respeitando status e pagamento. Consulta o estoque da unidade. |
| Cozinha | Consulta pedidos permitidos, inicia preparo e marca pronto na própria unidade. Consulta o estoque da unidade. |
| Administrador | Cadastra Atendente/Cozinha e altera sua unidade; consulta pedidos/estoque da rede, registra movimentações manuais e cancela pedidos conforme as regras. |

Atendente e Cozinha não acessam CPF, nascimento ou e-mail do Cliente nas consultas de pedidos. O mock de pagamentos é uma integração externa simulada, sem perfil humano. Requisitos relacionados: RF01, RF02, RF05–RF08 e RNF02–RNF03.

[Voltar ao topo](#documentação)

## Fluxo crítico

**Precondições:** unidade existente, produtos associados ao cardápio e disponíveis, estoque suficiente e solicitante autenticado com permissão para o canal.

1. Cliente em APP/WEB ou Atendente em BALCAO informa unidade, canal e itens.
2. A API valida os dados, calcula valores, cria o pedido em `AGUARDANDO_PAGAMENTO` e baixa o estoque, sem registros parciais.
3. O solicitante autorizado pede o pagamento mock. A aprovação registra o pagamento e altera o pedido para `RECEBIDO`.
4. A Cozinha da unidade altera o pedido para `EM_PREPARO` e depois `PRONTO`.
5. O Atendente da unidade conclui o pedido somente quando estiver pronto e com pagamento aprovado.

**Resultado:** pedido, itens, pagamento, movimentações de estoque e eventos de auditoria persistidos.

**Exceções:**

- BALCAO pode ser consultado e preparado pela Cozinha antes da aprovação do pagamento; aprovação posterior não retrocede `EM_PREPARO` ou `PRONTO`. APP/WEB e TOTEM conceitual exigem aprovação para consulta pela Cozinha e início do preparo.
- Recusa ou falha de pagamento preserva o status e permite nova tentativa conforme RF08, sem duplicar efeitos. O limite de espera do mock é de 5 segundos.
- Cancelamento somente em `AGUARDANDO_PAGAMENTO` ou `RECEBIDO`, por Cliente proprietário, Atendente da unidade ou Administrador. Restitui estoque uma vez e registra estorno simulado quando pago.
- Dados inválidos, estoque insuficiente ou falta de permissão impedem a operação sem registros parciais. `CONCLUIDO` e `CANCELADO` são finais.

Requisitos relacionados: RF05–RF08 e RNF04, RNF07.

[Voltar ao topo](#documentação)

## Modelagem principal

A visão de domínio abaixo orienta o DER e o diagrama de classes.

| Conceito | Relações e responsabilidade | Base |
|---|---|---|
| Usuário e perfil | Um perfil por cadastro; Atendente/Cozinha vinculados a uma unidade; Cliente associado aos próprios pedidos APP/WEB. | RF01–RF02 |
| Unidade e cardápio | Unidade reúne produtos associados, com preço e disponibilidade locais. | RF03–RF04 |
| Produto e estoque | Saldo por produto e unidade; movimentações manuais ou vinculadas à criação/cancelamento de pedidos. | RF07 |
| Pedido e item | Pedido pertence a uma unidade, registra canal e status e contém um ou mais itens com quantidade e valores. BALCAO é anônimo. | RF05–RF06 |
| Tentativa de pagamento | Vinculada ao pedido; registra valor, retorno e falha, com controle de efeitos repetidos e estorno simulado quando aplicável. | RF08 |
| Registro de auditoria | Registra operação, recurso, resultado, data/hora e responsável identificável, sem credenciais ou dados pessoais completos. | RNF04 |

O DER explicita PK/FK, cardinalidades e restrições e permanecer compatível com PostgreSQL e as migrations Flyway.

[Voltar ao topo](#documentação)

[Voltar à apresentação do projeto](../README.md)
