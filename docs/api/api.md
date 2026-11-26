# Utilização da API implementada

[Voltar ao índice da documentação](../README.md)

## Sumário

- [Swagger e autorização](#swagger-e-autorização)
- [Exemplos de requisições](#exemplos-de-requisições)
- [Respostas](#respostas)
- [Auditoria](#auditoria)

## Swagger e autorização

Com a aplicação iniciada conforme o [README](../../README.md), acesse `http://localhost:8080/q/swagger-ui/`. A especificação está em `http://localhost:8080/q/openapi`. O Swagger apresenta somente rotas já implementadas; o [contrato do MVP](../contratos/contratos.md) inclui também rotas planejadas.

Para executar uma operação, abra a rota, selecione **Try it out**, preencha os campos e clique em **Execute**. Para operações da equipe, faça login com uma conta Administrador, copie o campo `token` da resposta e informe-o em **Authorize**. A interface adiciona o prefixo `Bearer` automaticamente.

## Exemplos de requisições

Os exemplos são fictícios. Use CPF matematicamente válido e não cadastrado, e-mail ainda não utilizado e senha com pelo menos oito caracteres sem espaços. As unidades dos exemplos dependem do carregamento dos dados iniciais; substitua os identificadores quando necessário.

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

`POST /auth/login` — não exige token; utilize as credenciais de uma conta cadastrada:

```json
{ "email": "cliente@example.com", "senha": "Senha123" }
```

A resposta contém `token` e `expiraEm`. O token vale 60 minutos.

### Cadastro de equipe

`POST /usuarios` — exige Administrador. Informe `ATENDENTE` ou `COZINHA` e uma unidade existente:

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

`PATCH /usuarios/{id}/unidade` — exige Administrador. No caminho, informe o UUID do Atendente ou Cozinha retornado pelo cadastro; no corpo, informe o UUID da nova unidade:

```json
{ "unidadeId": "10000000-0000-0000-0000-000000000002" }
```

## Respostas

Cadastro retorna `201`; login e alteração de unidade retornam `200`. Dados inválidos retornam `400`, token ausente ou inválido `401`, perfil sem permissão `403`, usuário ou unidade inexistente `404` e CPF/e-mail duplicado `409`, conforme a operação. Erros de validação retornam `codigo`, `mensagem` e `detalhes`; a mensagem de CPF, e-mail ou senha inválidos não identifica o campo específico.

## Auditoria

Cadastro, login e alterações de equipe são registrados em `eventos_auditoria`. Recusas de autenticação e autorização nas rotas de equipe também são registradas; esses eventos são processados assincronamente e podem aparecer após a resposta. Os registros não incluem credenciais nem dados pessoais completos. Eventos de pedidos e estoque serão integrados às respectivas funcionalidades.

[Voltar ao topo](#utilização-da-api-implementada)
