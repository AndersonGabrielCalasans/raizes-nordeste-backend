# Contratos da API

[Voltar ao índice da documentação](../README.md)

## Convenções

- Rotas usam recursos no plural; identificadores aparecem no caminho.
- Listagens usam `page` e `limit`; `page` inicia em 1, `limit` padrão é 20 e o máximo é 100.
- Resposta paginada: `{ "items": [], "page": 1, "limit": 20, "total": 0 }`.
- Operações protegidas exigem JWT. Ausência ou token inválido retorna `401`; token válido sem permissão retorna `403`.
- O campo obrigatório `canalPedido` admite `APP`, `WEB`, `BALCAO`, `TOTEM` e `PICKUP`. TOTEM e PICKUP retornam `422` no MVP, pois sua operação permanece conceitual.

## Erro padrão

```json
{
  "codigo": "ESTOQUE_INSUFICIENTE",
  "mensagem": "Saldo indisponível para um ou mais itens.",
  "detalhes": []
}
```

`codigo` identifica a regra ou validação recusada; `detalhes` contém campos inválidos quando aplicável. Respostas não incluem senha, hash, token completo ou dados pessoais fora da permissão.

## Autenticação e equipe

| Método e rota | Permissão | Entrada | Sucesso | Erros |
|---|---|---|---|---|
| `POST /auth/cadastro` | Pública; cria Cliente | nome, CPF, nascimento, e-mail e senha | `201` | `400`, `409` |
| `POST /auth/login` | Pública | e-mail e senha | `200` com JWT | `400`, `401` |
| `POST /usuarios` | Administrador | dados de Atendente/Cozinha e unidade | `201` | `400`, `401`, `403`, `409` |
| `PATCH /usuarios/{id}/unidade` | Administrador | `unidadeId` | `200` | `400`, `401`, `403`, `404` |

```json
POST /auth/cadastro
{
  "nome": "Ana Souza",
  "cpf": "12345678909",
  "dataNascimento": "2000-05-12",
  "email": "ana@example.com",
  "senha": "Senha123"
}
```

```json
201 Created
{
  "id": "usuario-1",
  "nome": "Ana Souza",
  "perfil": "CLIENTE"
}
```

```json
POST /auth/login
{ "email": "ana@example.com", "senha": "Senha123" }
```

```json
200 OK
{ "token": "jwt", "expiraEm": "data/hora de expiração" }
```

```json
POST /usuarios
{ "nome": "João Lima", "email": "joao@example.com", "perfil": "ATENDENTE", "unidadeId": "unidade-1", "senha": "Senha123" }

201 Created
{ "id": "usuario-2", "perfil": "ATENDENTE", "unidadeId": "unidade-1" }

PATCH /usuarios/usuario-2/unidade
{ "unidadeId": "unidade-2" }

200 OK
{ "id": "usuario-2", "unidadeId": "unidade-2" }
```

## Unidades e cardápio

| Método e rota | Permissão | Parâmetros | Sucesso | Erros |
|---|---|---|---|---|
| `GET /unidades` | Pública | `page`, `limit` | `200` | `400` |
| `GET /unidades/{id}/cardapio` | Pública | `id`, `page`, `limit` | `200` | `400`, `404` |

```json
GET /unidades/1/cardapio?page=1&limit=20
```

```json
200 OK
{
  "items": [{ "produtoId": "produto-1", "nome": "Cuscuz", "preco": 12.5 }],
  "page": 1,
  "limit": 20,
  "total": 1
}
```

```json
GET /unidades?page=1&limit=20

200 OK
{ "items": [{ "id": "unidade-1", "nome": "Centro" }], "page": 1, "limit": 20, "total": 1 }
```

## Estoque

| Método e rota | Permissão | Entrada ou parâmetros | Sucesso | Erros |
|---|---|---|---|---|
| `GET /estoque` | Administrador, Atendente ou Cozinha | `page`, `limit`; `unidadeId` apenas para Administrador | `200` | `400`, `401`, `403` |
| `POST /estoque/movimentacoes` | Administrador | produtoUnidadeId, tipo, quantidade | `201` | `400`, `401`, `403`, `404`, `409` |

```json
POST /estoque/movimentacoes
{ "produtoUnidadeId": "produto-unidade-1", "tipo": "ENTRADA", "quantidade": 10 }
```

```json
201 Created
{ "id": "movimento-1", "saldoAtual": 25 }
```

```json
GET /estoque?page=1&limit=20

200 OK
{ "items": [{ "produtoUnidadeId": "produto-unidade-1", "saldo": 25 }], "page": 1, "limit": 20, "total": 1 }
```

## Pedidos

| Método e rota | Permissão | Entrada ou parâmetros | Sucesso | Erros |
|---|---|---|---|---|
| `POST /pedidos` | Cliente para APP/WEB; Atendente da unidade para BALCAO | unidadeId, canalPedido, itens | `201` | `400`, `401`, `403`, `404`, `409`, `422` |
| `GET /pedidos` | Cliente, Atendente, Cozinha ou Administrador | `canalPedido`, `status`, `page`, `limit` | `200` | `400`, `401`, `403` |
| `GET /pedidos/{id}` | Conforme propriedade, unidade e perfil | `id` | `200` | `401`, `403`, `404` |
| `PATCH /pedidos/{id}/status` | Cozinha ou Atendente da unidade | status solicitado | `200` | `400`, `401`, `403`, `404`, `409` |
| `POST /pedidos/{id}/cancelamento` | Cliente proprietário, Atendente da unidade ou Administrador | `id` | `200` | `401`, `403`, `404`, `409` |

```json
POST /pedidos
{
  "unidadeId": "unidade-1",
  "canalPedido": "APP",
  "itens": [{ "produtoUnidadeId": "produto-unidade-1", "quantidade": 2 }]
}
```

```json
201 Created
{
  "id": "pedido-1",
  "canalPedido": "APP",
  "status": "AGUARDANDO_PAGAMENTO",
  "situacaoPagamento": "PENDENTE",
  "valorTotal": 25.0
}
```

```json
PATCH /pedidos/pedido-1/status
{ "status": "EM_PREPARO" }
```

```json
200 OK
{ "id": "pedido-1", "status": "EM_PREPARO", "situacaoPagamento": "APROVADO" }
```

```json
GET /pedidos?canalPedido=APP&status=AGUARDANDO_PAGAMENTO&page=1&limit=20

200 OK
{ "items": [{ "id": "pedido-1", "canalPedido": "APP", "status": "AGUARDANDO_PAGAMENTO" }], "page": 1, "limit": 20, "total": 1 }

GET /pedidos/pedido-1

200 OK
{ "id": "pedido-1", "itens": [{ "produtoUnidadeId": "produto-unidade-1", "quantidade": 2 }], "status": "AGUARDANDO_PAGAMENTO" }

POST /pedidos/pedido-1/cancelamento

200 OK
{ "id": "pedido-1", "status": "CANCELADO" }
```

Na listagem e consulta, Cliente acessa somente os próprios pedidos; Atendente e Cozinha acessam apenas sua unidade, aplicando a restrição de pagamento para pedidos APP/WEB; Administrador consulta a rede. O filtro não amplia o acesso do perfil.

## Pagamento mock

| Método e rota | Permissão | Entrada | Sucesso | Erros |
|---|---|---|---|---|
| `POST /pedidos/{id}/pagamentos` | Cliente proprietário em APP/WEB; Atendente da unidade em BALCAO | `id` | `200` | `401`, `403`, `404`, `409` |

```json
POST /pedidos/pedido-1/pagamentos
```

```json
200 OK
{
  "tentativaId": "tentativa-1",
  "resultado": "APROVADO",
  "situacaoPagamento": "APROVADO",
  "statusPedido": "RECEBIDO"
}
```

O adaptador envia ao gateway mock o identificador da tentativa, o identificador do pedido e o valor calculado. O retorno do mock registra aprovação, recusa ou falha. Recusa e falha preservam o status operacional e permitem nova tentativa; retorno repetido não duplica pagamento ou transição. Não há pagamento financeiro real.

```json
Payload enviado ao gateway mock
{ "tentativaId": "tentativa-1", "pedidoId": "pedido-1", "valor": 25.0 }

Retorno do gateway mock
{ "tentativaId": "tentativa-1", "resultado": "APROVADO" }
```

## Cobertura conceitual

Não há endpoints implementados para TOTEM, PICKUP, fidelidade, campanhas, Owner, múltiplos perfis, gestão completa de produtos ou gestão completa de unidades. Esses módulos continuam documentados como conceituais.

Requisitos relacionados: RF01–RF10 e RNF01–RNF04, RNF07–RNF08.

[Voltar ao topo](#contratos-da-api)
