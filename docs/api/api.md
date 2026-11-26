# Utilização da API implementada

[Voltar ao índice da documentação](../README.md)

## Sumário

- [Swagger e autorização](#swagger-e-autorização)
- [Exemplos de requisições](#exemplos-de-requisições)
- [Respostas](#respostas)
- [Auditoria](#auditoria)

## Swagger e autorização

Com a aplicação iniciada conforme o [README](../../README.md), o Swagger está disponível em `http://localhost:8080/q/swagger-ui/` e a especificação em `http://localhost:8080/q/openapi`. O Swagger e o [contrato do MVP](../contratos/contratos.md) descrevem as rotas implementadas; funcionalidades conceituais são identificadas separadamente.

Para executar uma operação, a rota é aberta, a opção **Try it out** é selecionada e os campos são preenchidos antes de acionar **Execute**. Para operações da equipe, o login é feito com uma conta Administrador e o campo `token` da resposta é informado em **Authorize**. A interface adiciona o prefixo `Bearer` automaticamente.

## Exemplos de requisições

Os exemplos são fictícios. O cadastro exige CPF matematicamente válido e não cadastrado, e-mail ainda não utilizado e senha com pelo menos oito caracteres, sem espaços e com no máximo 72 bytes em UTF-8. As unidades dos exemplos dependem do carregamento dos dados iniciais; os identificadores podem ser substituídos quando necessário.

### Cadastro público de Cliente

`POST /auth/cadastro` — não exige token:

```json
{
  "nome": "Cliente Exemplo",
  "cpf": "52998224725",
  "dataNascimento": "1990-03-10",
  "email": "cliente@example.com",
  "senha": "Senha123"
}
```

### Login

`POST /auth/login` — não exige token e recebe as credenciais de uma conta cadastrada:

```json
{ "email": "cliente@example.com", "senha": "Senha123" }
```

A resposta contém `token` e `expiraEm`. O token vale 60 minutos.

### Cadastro de equipe

`POST /usuarios` — exige Administrador e recebe `ATENDENTE` ou `COZINHA` e uma unidade existente:

```json
{
  "nome": "Atendente Exemplo",
  "cpf": "11144477735",
  "dataNascimento": "1990-03-10",
  "email": "equipe@example.com",
  "senha": "Senha123",
  "perfil": "ATENDENTE",
  "unidadeId": "10000000-0000-0000-0000-000000000001"
}
```

### Alteração de unidade

`PATCH /usuarios/{id}/unidade` — exige Administrador. O caminho recebe o UUID do Atendente ou Cozinha retornado pelo cadastro; o corpo recebe o UUID da nova unidade:

```json
{ "unidadeId": "10000000-0000-0000-0000-000000000002" }
```

### Unidades, cardápio e estoque

`GET /unidades?page=1&limit=20`, `GET /unidades/10000000-0000-0000-0000-000000000001` e `GET /unidades/10000000-0000-0000-0000-000000000001/cardapio?page=1&limit=20` são públicos. O cardápio retorna produtos disponíveis com saldo positivo.

`GET /estoque?page=1&limit=20` exige Administrador, Atendente ou Cozinha. Atendente e Cozinha consultam a unidade vinculada; somente Administrador pode informar o filtro `unidadeId`. A paginação começa em 1 e aceita limite de 1 a 100.

`POST /estoque/movimentacoes` exige Administrador. O identificador é o da associação do produto à unidade, retornado na consulta de estoque:

```json
{ "produtoUnidadeId": "30000000-0000-0000-0000-000000000001", "tipo": "ENTRADA", "quantidade": 10 }
```

O tipo pode ser `ENTRADA` ou `SAIDA`; quantidade deve ser inteira e positiva. A resposta retorna o identificador do movimento e `saldoAtual`. Saída superior ao saldo retorna `409` sem alteração.

### Criação e consulta de pedidos

`POST /pedidos` exige token de Cliente para `APP` ou `WEB`, ou de Atendente para `BALCAO` em sua unidade:

```json
{
  "unidadeId": "10000000-0000-0000-0000-000000000001",
  "canalPedido": "APP",
  "itens": [{ "produtoUnidadeId": "30000000-0000-0000-0000-000000000001", "quantidade": 2 }]
}
```

A API calcula os valores e baixa o estoque na criação. `GET /pedidos?page=1&limit=20&canalPedido=APP` e `GET /pedidos/{id}` exigem token e respeitam as regras de perfil, propriedade, unidade e visibilidade da Cozinha. Produtos indisponíveis ou saldo insuficiente retornam `409`.

### Pagamento, status e cancelamento

`POST /pedidos/{id}/pagamentos` solicita o pagamento simulado sem corpo. Cliente paga o próprio pedido APP/WEB; Atendente paga pedido BALCAO de sua unidade. A resposta inclui `tentativaId`, `pedidoId`, `resultado`, `situacaoPagamento` e `statusPedido`. Repetir uma solicitação após aprovação retorna a mesma tentativa, sem novo pagamento. Recusa e falha permitem nova tentativa.

O gateway é um adaptador mock local, sem cobrança real. Para simular o retorno, `RAIZES_PAGAMENTO_MOCK` é configurado no ambiente como `APROVADO` (padrão), `RECUSADO`, `FALHA` ou `TIMEOUT`, seguido do reinício da API. O limite de espera é de cinco segundos; timeout gera resultado `FALHA`.

`PATCH /pedidos/{id}/status` recebe, por exemplo:

```json
{ "status": "EM_PREPARO" }
```

Cozinha da unidade pode iniciar o preparo e depois marcar `PRONTO`; Atendente da unidade pode marcar `CONCLUIDO` somente quando o pedido estiver pronto e pago. Em BALCAO, o preparo pode iniciar antes do pagamento.

`POST /pedidos/{id}/cancelamento` não exige corpo. Cliente proprietário, Atendente da unidade ou Administrador pode cancelar antes do preparo. O cancelamento devolve o estoque e registra estorno simulado quando houver aprovação; tentativa repetida retorna `409`.

## Respostas

Cadastro retorna `201`; login e alteração de unidade retornam `200`. Dados inválidos retornam `400`, token ausente ou inválido `401`, perfil sem permissão `403`, usuário ou unidade inexistente `404` e CPF/e-mail duplicado `409`, conforme a operação. Erros de regras de negócio e as recusas de segurança retornam `codigo`, `mensagem` e `detalhes`; a mensagem de CPF, e-mail ou senha inválidos não identifica o campo específico.

## Auditoria

Cadastro, login, alterações de equipe, pedidos, pagamento e movimentações de estoque geram registros em `eventos_auditoria`. Consultas autorizadas de pedidos registram `CONSULTAR_PEDIDOS` para listagens e `CONSULTAR_PEDIDO` para consultas por identificador. Algumas recusas de autenticação e autorização são processadas assincronamente e podem aparecer após a resposta. Os registros não incluem credenciais nem dados pessoais completos. O [guia de testes](../testes/testes.md) apresenta uma consulta para verificar eventos de pedido.

[Voltar ao topo](#utilização-da-api-implementada)
