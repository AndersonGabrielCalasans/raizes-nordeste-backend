# Documentação

Esta seção reúne a documentação técnica do backend da rede Raízes do Nordeste. Os documentos apresentam os comportamentos do sistema, suas regras e os limites da implementação, com base no estudo de caso e no roteiro da trilha Back-End.

## Índice

- [Análise e requisitos](#análise-e-requisitos)
- [MVP](#mvp)
- [Permissões principais](#permissões-principais)
- [Fluxo crítico](#fluxo-crítico)
- [Modelagem e diagramas](#modelagem-e-diagramas)
- [Arquitetura e contratos](#arquitetura-e-contratos)
- [Testes](#testes)
- [Privacidade](#privacidade)
- [Entrega acadêmica](#entrega-acadêmica)

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

Unidades, produtos, preços e associações iniciais ao cardápio são provisionados no ambiente pelo seed. O recorte permite testar o fluxo principal sem implementar os módulos conceituais. Regras detalhadas: RF01–RF10 e RNF01–RNF08 nos documentos acima.

[Voltar ao topo](#documentação)

## Permissões principais

Consultas de unidades/cardápio, cadastro público de Cliente e login dispensam token. Operações protegidas exigem JWT válido. Cada cadastro possui um perfil no MVP, sem herança automática de permissões.

| Perfil | Operações e limites |
|---|---|
| Cliente | Cria pedidos APP/WEB; consulta, paga e cancela somente os próprios pedidos, conforme as regras. |
| Atendente | Cria e solicita pagamento de BALCAO; consulta, cancela e conclui pedidos da unidade vinculada, respeitando status e pagamento. Consulta o estoque da unidade. |
| Cozinha | Consulta pedidos permitidos, inicia preparo e marca pronto na própria unidade. Consulta o estoque da unidade. |
| Administrador | Cadastra Atendente/Cozinha e altera sua unidade; consulta pedidos/estoque da rede, registra movimentações manuais e cancela pedidos conforme as regras. |

Atendente e Cozinha não acessam CPF, nascimento ou e-mail do Cliente nas consultas de pedidos. O pagamento usa um adaptador mock local síncrono, sem serviço externo HTTP ou callback. Requisitos relacionados: RF01, RF02, RF05–RF08 e RNF02–RNF03.

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

## Modelagem e diagramas

A visão de domínio abaixo orienta os [diagramas da M01-004](modelagem/modelagem.md).

| Entidade | Relações e responsabilidade | Base |
|---|---|---|
| Usuário | Cadastro, credenciais e um perfil; Atendente/Cozinha vinculados a uma unidade; Cliente associado aos próprios pedidos APP/WEB. | RF01–RF02 |
| Unidade | Loja à qual pertencem o cardápio, o estoque e os pedidos. | RF03–RF07 |
| Produto | Identificação e descrição do produto vendido. | RF04 |
| ProdutoUnidade | Associação única entre produto e unidade, com preço local, disponibilidade e saldo de estoque. | RF04, RF07 |
| MovimentaçãoEstoque | Entrada ou saída de ProdutoUnidade, com quantidade e referência ao pedido nas movimentações automáticas. | RF07 |
| Pedido | Unidade, canal, status operacional, situação do pagamento e total; Cliente obrigatório em APP/WEB e ausente em BALCAO. Registra separadamente o usuário criador. | RF05–RF06, RF08 |
| ItemPedido | Pertence a um pedido e identifica o produto da unidade, quantidade e preço praticado na compra. Cada pedido contém pelo menos um item. | RF05 |
| TentativaPagamento | Pertence a um pedido; registra valor, resultado ou falha e eventual estorno simulado. Permite várias tentativas sem duplicar pagamento aprovado. | RF08 |
| EventoAuditoria | Registra operação, recurso, resultado, data/hora e responsável quando identificável, sem credenciais ou dados pessoais completos. | RNF04 |

Perfil é representado por enum, sem tabela de permissões. O cardápio é uma consulta dos produtos disponíveis da unidade, sem entidade própria. A autorização considera perfil, unidade, propriedade, status e pagamento.

Funcionalidades conceituais não geram tabelas de implementação no DER do MVP. Casos de uso, feature crítica, DER, classes e sequência do fluxo estão organizados na documentação de modelagem. O DER detalha PK/FK, cardinalidades e restrições compatíveis com PostgreSQL e as migrations Flyway.

[Voltar ao topo](#documentação)

## Arquitetura e contratos

| Documento | Conteúdo |
|---|---|
| [Arquitetura do backend](arquitetura/arquitetura.md) | Camadas, dependências e responsabilidades da API, aplicação, domínio e infraestrutura. |
| [Contratos da API](contratos/contratos.md) | Rotas, permissões, parâmetros, exemplos, respostas, códigos HTTP e padrão de erro do MVP. |
| [Utilização da API implementada](api/api.md) | Acesso ao Swagger, autenticação e exemplos das rotas disponíveis. |

[Voltar ao topo](#documentação)

## Testes

O [guia de testes do MVP](testes/testes.md) apresenta os cenários, a coleção do Insomnia, a execução e a verificação da auditoria no PostgreSQL.

O [plano de desempenho e recuperação](testes/desempenho-recuperacao.md) apresenta o cenário local, sua reprodução e os resultados dos RNF05 e RNF06.

[Voltar ao topo](#documentação)

## Privacidade

A [análise de privacidade](privacidade/privacidade.md) reúne os dados tratados, finalidades, controles e limitações de consentimento e retenção.

[Voltar ao topo](#documentação)

## Entrega acadêmica

O [documento acadêmico em PDF](entrega-academica/4591724_Projeto_Back_End.pdf) reúne requisitos, diagramas, contratos, segurança, resultados de testes e referências técnicas.

[Voltar ao topo](#documentação)

[Voltar à apresentação do projeto](../README.md)
