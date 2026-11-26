# Contratos da API

[Voltar ao índice da documentação](../README.md)

## Convenções

- Rotas usam recursos no plural; identificadores aparecem no caminho.
- Listagens usam `page` e `limit`; `page` inicia em 1, `limit` padrão é 20 e o máximo é 100.
- Resposta paginada: `{ "items": [], "page": 1, "limit": 20, "total": 0 }`.
- Operações protegidas exigem JWT. Ausência ou token inválido retorna `401`; token válido sem permissão retorna `403`.
- O domínio inclui `APP`, `WEB`, `BALCAO`, `TOTEM` e `PICKUP`. Na criação, a API aceita APP/WEB para Cliente e BALCAO para Atendente; TOTEM e PICKUP retornam `400`, pois permanecem conceituais. Os cinco valores são aceitos como filtro de listagem.

## Erro padrão

```json
{
  "codigo": "ESTOQUE_INSUFICIENTE",
  "mensagem": "Produto indisponível ou estoque insuficiente.",
  "detalhes": []
}
```

`codigo` identifica a regra ou validação recusada; `detalhes` contém informações adicionais quando aplicável. Erros de negócio, recusas de segurança e falhas internas representadas por `IllegalStateException` usam esse formato. A padronização de outras falhas internas não está comprovada. A resposta de login retorna o JWT completo ao solicitante; as demais respostas não retornam senha ou hash.

## Autenticação e equipe

| Método e rota | Permissão | Entrada | Sucesso | Erros |
|---|---|---|---|---|
| `POST /auth/cadastro` | Pública; cria Cliente | nome, CPF, nascimento, e-mail e senha | `201` | `400`, `409` |
| `POST /auth/login` | Pública | e-mail e senha | `200` com JWT | `400`, `401` |
| `POST /usuarios` | Administrador | dados de Atendente/Cozinha e unidade | `201` | `400`, `401`, `403`, `404`, `409` |
| `PATCH /usuarios/{id}/unidade` | Administrador | `unidadeId` | `200` | `400`, `401`, `403`, `404` |

```text
POST /auth/cadastro
{
  "nome": "Ana Souza",
  "cpf": "12345678909",
  "dataNascimento": "2000-05-12",
  "email": "ana@example.com",
  "senha": "Senha123"
}
```

```text
201 Created
{
  "id": "40000000-0000-0000-0000-000000000002",
  "nome": "Ana Souza",
  "perfil": "CLIENTE",
  "unidadeId": null
}
```

```text
POST /auth/login
{ "email": "ana@example.com", "senha": "Senha123" }
```

```text
200 OK
{ "token": "jwt", "expiraEm": "2030-01-01T13:00:00Z" }
```

```text
POST /usuarios
{ "nome": "João Lima", "cpf": "11144477735", "dataNascimento": "1990-03-10", "email": "joao@example.com", "perfil": "ATENDENTE", "unidadeId": "10000000-0000-0000-0000-000000000001", "senha": "Senha123" }

201 Created
{ "id": "40000000-0000-0000-0000-000000000003", "nome": "João Lima", "perfil": "ATENDENTE", "unidadeId": "10000000-0000-0000-0000-000000000001" }

PATCH /usuarios/40000000-0000-0000-0000-000000000003/unidade
{ "unidadeId": "10000000-0000-0000-0000-000000000002" }

200 OK
{ "id": "40000000-0000-0000-0000-000000000003", "nome": "João Lima", "perfil": "ATENDENTE", "unidadeId": "10000000-0000-0000-0000-000000000002" }
```

## Unidades e cardápio

| Método e rota | Permissão | Parâmetros | Sucesso | Erros |
|---|---|---|---|---|
| `GET /unidades` | Pública | `page`, `limit` | `200` | `400` |
| `GET /unidades/{id}` | Pública | `id` | `200` | `404` |
| `GET /unidades/{id}/cardapio` | Pública | `id`, `page`, `limit` | `200` | `400`, `404` |

```text
GET /unidades/10000000-0000-0000-0000-000000000001/cardapio?page=1&limit=20
```

```text
200 OK
{
  "items": [{ "id": "20000000-0000-0000-0000-000000000001", "nome": "Cuscuz", "descricao": "Cuscuz nordestino", "preco": 12.5, "disponivel": true }],
  "page": 1,
  "limit": 20,
  "total": 1
}
```

```text
GET /unidades?page=1&limit=20

200 OK
{ "items": [{ "id": "10000000-0000-0000-0000-000000000001", "nome": "Unidade Centro", "endereco": "Rua do Centro, 100" }], "page": 1, "limit": 20, "total": 1 }
```

Exemplo de consulta individual: `GET /unidades/10000000-0000-0000-0000-000000000001` retorna `200` com `id`, `nome` e `endereco`.

## Estoque

| Método e rota | Permissão | Entrada ou parâmetros | Sucesso | Erros |
|---|---|---|---|---|
| `GET /estoque` | Administrador, Atendente ou Cozinha | `page`, `limit`; `unidadeId` apenas para Administrador | `200` | `400`, `401`, `403`, `404` |
| `POST /estoque/movimentacoes` | Administrador | produtoUnidadeId, tipo, quantidade | `201` | `400`, `401`, `403`, `404`, `409` |

```text
POST /estoque/movimentacoes
{ "produtoUnidadeId": "30000000-0000-0000-0000-000000000001", "tipo": "ENTRADA", "quantidade": 10 }
```

```text
201 Created
{ "id": "50000000-0000-0000-0000-000000000002", "saldoAtual": 25 }
```

```text
GET /estoque?page=1&limit=20

200 OK
{ "items": [{ "produtoUnidadeId": "30000000-0000-0000-0000-000000000001", "unidadeId": "10000000-0000-0000-0000-000000000001", "produtoId": "20000000-0000-0000-0000-000000000001", "nome": "Cuscuz", "saldo": 25 }], "page": 1, "limit": 20, "total": 1 }
```

## Pedidos

| Método e rota | Permissão | Entrada ou parâmetros | Sucesso | Erros |
|---|---|---|---|---|
| `POST /pedidos` | Cliente para APP/WEB; Atendente da unidade para BALCAO | unidadeId, canalPedido, itens | `201` | `400`, `401`, `403`, `404`, `409` |
| `GET /pedidos` | Cliente, Atendente, Cozinha ou Administrador | `canalPedido`, `status`, `page`, `limit` | `200` | `400`, `401`, `403` |
| `GET /pedidos/{id}` | Conforme propriedade, unidade e perfil | `id` | `200` | `401`, `403`, `404` |
| `PATCH /pedidos/{id}/status` | Cozinha ou Atendente da unidade | status solicitado | `200` | `400`, `401`, `403`, `404`, `409` |
| `POST /pedidos/{id}/cancelamento` | Cliente proprietário, Atendente da unidade ou Administrador | `id` | `200` | `401`, `403`, `404`, `409` |

```text
POST /pedidos
{
  "unidadeId": "10000000-0000-0000-0000-000000000001",
  "canalPedido": "APP",
  "itens": [{ "produtoUnidadeId": "30000000-0000-0000-0000-000000000001", "quantidade": 2 }]
}
```

```text
201 Created
{
  "id": "50000000-0000-0000-0000-000000000001",
  "unidadeId": "10000000-0000-0000-0000-000000000001",
  "clienteId": "40000000-0000-0000-0000-000000000002",
  "criadorId": "40000000-0000-0000-0000-000000000002",
  "canalPedido": "APP",
  "status": "AGUARDANDO_PAGAMENTO",
  "situacaoPagamento": "PENDENTE",
  "valorTotal": 25.0,
  "itens": [{ "produtoUnidadeId": "30000000-0000-0000-0000-000000000001", "quantidade": 2, "precoUnitario": 12.5, "subtotal": 25.0 }]
}
```

```text
PATCH /pedidos/50000000-0000-0000-0000-000000000001/status
{ "status": "EM_PREPARO" }
```

```text
200 OK
{ "id": "50000000-0000-0000-0000-000000000001", "status": "EM_PREPARO", "situacaoPagamento": "APROVADO" }
```

```text
GET /pedidos?canalPedido=APP&status=AGUARDANDO_PAGAMENTO&page=1&limit=20

200 OK
{ "items": [{ "id": "50000000-0000-0000-0000-000000000001", "canalPedido": "APP", "status": "AGUARDANDO_PAGAMENTO" }], "page": 1, "limit": 20, "total": 1 }

GET /pedidos/50000000-0000-0000-0000-000000000001

200 OK
{ "id": "50000000-0000-0000-0000-000000000001", "itens": [{ "produtoUnidadeId": "30000000-0000-0000-0000-000000000001", "quantidade": 2 }], "status": "AGUARDANDO_PAGAMENTO" }

POST /pedidos/50000000-0000-0000-0000-000000000001/cancelamento

200 OK
{ "id": "50000000-0000-0000-0000-000000000001", "status": "CANCELADO", "situacaoPagamento": "ESTORNADO" }
```

Os exemplos compactos de listagem e consulta mostram apenas campos selecionados; a estrutura completa do pedido aparece no exemplo de criação.

Na listagem e consulta, Cliente acessa somente os próprios pedidos; Atendente e Cozinha acessam apenas sua unidade, sendo que a Cozinha vê pedidos APP/WEB somente após aprovação; Administrador consulta a rede. O filtro não amplia o acesso do perfil.

## Pagamento mock

| Método e rota | Permissão | Entrada | Sucesso | Erros |
|---|---|---|---|---|
| `POST /pedidos/{id}/pagamentos` | Cliente proprietário em APP/WEB; Atendente da unidade em BALCAO | `id` | `200` | `401`, `403`, `404`, `409` |

```text
POST /pedidos/50000000-0000-0000-0000-000000000001/pagamentos
```

```text
200 OK
{
  "tentativaId": "60000000-0000-0000-0000-000000000001",
  "pedidoId": "50000000-0000-0000-0000-000000000001",
  "resultado": "APROVADO",
  "situacaoPagamento": "APROVADO",
  "statusPedido": "RECEBIDO"
}
```

O serviço passa ao adaptador local o identificador da tentativa, o identificador do pedido e o valor calculado. O adaptador retorna um resultado enum: aprovação, recusa ou falha. Recusa e falha preservam o status operacional e permitem nova tentativa; repetir a solicitação após aprovação não duplica o pagamento. Não há serviço HTTP externo, callback ou pagamento financeiro real.

## Cobertura conceitual

Não há endpoints implementados para TOTEM, PICKUP, fidelidade, campanhas, Owner, múltiplos perfis, gestão completa de produtos ou gestão completa de unidades. Esses módulos continuam documentados como conceituais.

Requisitos relacionados: RF01–RF10 e RNF01–RNF04, RNF07–RNF08.

[Voltar ao topo](#contratos-da-api)
