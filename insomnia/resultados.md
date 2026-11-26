# Resultados dos testes do MVP

Registro da execução manual da [coleção do Insomnia](raizes-insomnia.json), com resultados conferidos nas evidências. Para preparação, ordem e entradas completas, consulte o [guia de testes](../docs/testes/testes.md).

- Data da execução: 26/11/2026
- Ambiente: API local, PostgreSQL e dados iniciais conforme o README
- Mock: `APROVADO` para T06; `RECUSADO` para T10

As respostas dos cenários abaixo corresponderam aos resultados esperados. O caso de cadastro duplicado também retornou 409, conforme [evidência adicional](evidencias/T03-erro-409-cadastro-duplicado.png).

| Teste | Esperado | Obtido | Situação | Evidência |
|---|---|---|---|---|
| T01 — Consultar unidades | 200; unidade inicial | 200; unidades Centro e Litoral | Aprovado | [Ver](evidencias/T01-consultar-unidades.png) |
| T02 — Consultar cardápio | 200; produto inicial disponível | 200; Cuscuz e Tapioca | Aprovado | [Ver](evidencias/T02-consultar-cardapio.png) |
| T03 — Cadastrar cliente | 201; perfil `CLIENTE` | 201; perfil `CLIENTE` | Aprovado | [Ver](evidencias/T03-201-cadastrar-cliente.png) |
| T04 — Login válido | 200; token e validade | 200; token e expiração | Aprovado | [Ver](evidencias/T04-login-valido.png) |
| T05 — Criar pedido para aprovação | 201; `AGUARDANDO_PAGAMENTO`, `PENDENTE` | 201; `AGUARDANDO_PAGAMENTO`, `PENDENTE` | Aprovado | [Ver](evidencias/T05-criar-pedido-aprovacao.png) |
| T06 — Aprovar pagamento | 200; `APROVADO`, `RECEBIDO` | 200; `APROVADO`, `RECEBIDO` | Aprovado | [Ver](evidencias/T06-pagamento-aprovado.png) |
| T07 — Consultar pedido pago | 200; `RECEBIDO`, `APROVADO` | 200; `RECEBIDO`, `APROVADO` | Aprovado | [Ver](evidencias/T07-consultar-pedido-pago.png) |
| T08 — Cancelar pedido pago | 200; `CANCELADO`, `ESTORNADO` | 200; `CANCELADO`, `ESTORNADO` | Aprovado | [Ver](evidencias/T08-cancelar-pedido-pago.png) |
| T09 — Criar pedido para recusa | 201; `AGUARDANDO_PAGAMENTO`, `PENDENTE` | 201; `AGUARDANDO_PAGAMENTO`, `PENDENTE` | Aprovado | [Ver](evidencias/T09-criar-pedido-recusa.png) |
| T10 — Recusar pagamento | 200; `RECUSADO`, `AGUARDANDO_PAGAMENTO` | 200; `RECUSADO`, `AGUARDANDO_PAGAMENTO` | Aprovado | [Ver](evidencias/T10-pagamento-recusado.png) |
| T11 — Consultar pedido recusado | 200; `AGUARDANDO_PAGAMENTO`, `RECUSADO` | 200; `AGUARDANDO_PAGAMENTO`, `RECUSADO` | Aprovado | [Ver](evidencias/T11-consultar-pedido-recusado.png) |
| T12 — Acesso sem token | 401; `NAO_AUTENTICADO` | 401; `NAO_AUTENTICADO` | Aprovado | [Ver](evidencias/T12-acesso-sem-token.png) |
| T13 — Cliente sem permissão | 403; `ACESSO_NEGADO` | 403; `ACESSO_NEGADO` | Aprovado | [Ver](evidencias/T13-cliente-sem-permissao.png) |
| T14 — Canal obrigatório ausente | 400; `DADOS_INVALIDOS` | 400; `DADOS_INVALIDOS` | Aprovado | [Ver](evidencias/T14-canal-obrigatorio-ausente.png) |
| T15 — Quantidade inválida | 400; `DADOS_INVALIDOS` | 400; `DADOS_INVALIDOS` | Aprovado | [Ver](evidencias/T15-quantidade-invalida.png) |
| T16 — Produto inexistente | 404; `PRODUTO_NAO_ENCONTRADO` | 404; `PRODUTO_NAO_ENCONTRADO` | Aprovado | [Ver](evidencias/T16-produto-inexistente.png) |
| T17 — Estoque insuficiente | 409; `ESTOQUE_INSUFICIENTE` | 409; `ESTOQUE_INSUFICIENTE` | Aprovado | [Ver](evidencias/T17-estoque-insuficiente.png) |
| T18 — Repetir cancelamento | 409; `CANCELAMENTO_INVALIDO` | 409; `CANCELAMENTO_INVALIDO` | Aprovado | [Ver](evidencias/T18-repetir-cancelamento.png) |
| Auditoria do pedido de T05 | Eventos de criação, pagamento e cancelamento no PostgreSQL | Três eventos registrados | Aprovado | [Ver](evidencias/auditoria-pedido.md) |

As imagens complementares podem ser guardadas em [evidencias](evidencias/README.md). Não registre senhas, tokens ou dados pessoais reais neste arquivo ou nas imagens.
