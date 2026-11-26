# Testes do MVP

[Voltar ao índice da documentação](../README.md)

## Sumário

- [Preparação](#preparação)
- [Execução no Insomnia](#execução-no-insomnia)
- [Cenários e resultados esperados](#cenários-e-resultados-esperados)
- [Auditoria](#auditoria)
- [Registro de execução](#registro-de-execução)

## Preparação

1. A preparação do PostgreSQL, `.env`, chaves JWT, migrations e dados iniciais segue o [README](../../README.md). É necessário um banco local de testes: as requisições criam usuário e pedidos e alteram o estoque. As migrations são aplicadas pela aplicação no primeiro início; o seed é carregado em seguida.
2. A API deve ser iniciada com `RAIZES_PAGAMENTO_MOCK=APROVADO`, usando PowerShell na pasta do repositório:

   ```powershell
   $env:RAIZES_PAGAMENTO_MOCK = 'APROVADO'
   .\mvnw.cmd quarkus:dev
   ```

3. No Insomnia, a importação de [raizes-insomnia.json](../../insomnia/raizes-insomnia.json) é feita em um projeto local pela opção **Import**. O ambiente base da coleção **Raízes do Nordeste — testes do MVP** fica em **Manage Environments**.
4. Devem ser conferidos `base_url` e os IDs `unidade_id` e `produto_unidade_id`. Os valores iniciais são os do seed da Unidade Centro. O produto precisa estar disponível e ter saldo positivo. `produto_unidade_id` é a associação no estoque, não o ID do produto no cardápio.
5. O ambiente deve receber em `cliente_cpf` um CPF fictício matematicamente válido e ainda não cadastrado, em `cliente_email` um e-mail inédito e em `cliente_senha` uma senha local de pelo menos oito caracteres sem espaços. O exemplo `52998224725` é válido, mas não pode já estar cadastrado. Para repetir o cadastro, são necessários outro CPF válido e outro e-mail. Se já houver um Cliente, suas credenciais podem ser utilizadas e T03 pode ser ignorado.

## Execução no Insomnia

As requisições devem ser executadas pelo nome e na ordem T01–T18, com conferência do status HTTP e dos campos indicados abaixo. As descrições de cada requisição também apresentam o resultado esperado.

- Após T04, apenas o valor de `token` da resposta deve ser copiado para `token_cliente`, sem o prefixo `Bearer`; as requisições protegidas já o acrescentam. O token expira em 60 minutos; se necessário, T04 pode ser executado novamente.
- Após T05, `id` deve ser copiado para `pedido_aprovado_id`. T06–T08 e T18 usam esse pedido. T08 o cancela e devolve seu estoque; T18 deve ser executado somente depois de T08.
- Após T09, `id` deve ser copiado para `pedido_recusado_id`. A API deve ser encerrada com `Ctrl+C` e reiniciada no mesmo terminal, com o mesmo banco e chaves:

  ```powershell
  $env:RAIZES_PAGAMENTO_MOCK = 'RECUSADO'
  .\mvnw.cmd quarkus:dev
  ```

  Em seguida, são executados T10 e T11. A recusa retorna HTTP 200 por ser um resultado processado; os campos `resultado` e `statusPedido` também devem ser conferidos. Para voltar ao comportamento padrão, a API deve ser encerrada e reiniciada após definir `$env:RAIZES_PAGAMENTO_MOCK = 'APROVADO'`. O pedido recusado continua no banco e sua criação já baixou estoque; o cancelamento pode ser feito pelo Swagger com o token do Cliente antes de reiniciar os testes.
- Em seguida, são executados T12–T18. T12 não possui autenticação; os demais usam `token_cliente`. T17 pressupõe saldo inferior a 1.000.000 unidades. A auditoria é verificada conforme a seção seguinte.

A coleção usa variáveis preenchidas manualmente para tornar as dependências explícitas. O resultado observado de cada cenário é registrado em [resultados.md](../../insomnia/resultados.md); capturas complementares podem ser guardadas em [evidencias](../../insomnia/evidencias/README.md). Tokens e IDs ficam no ambiente local do Insomnia; dados reais ou credenciais não devem ser exportados. O pagamento é um mock local síncrono e não realiza cobrança nem chamada HTTP externa.

## Cenários e resultados esperados

Os nomes T01–T18 identificam as requisições correspondentes na coleção. Entradas são os corpos, caminhos e variáveis exibidos em cada requisição. Valores variáveis como IDs e token devem ser preenchidos conforme os passos anteriores.

| ID | Método e rota | Pré-condição e entrada | Resultado esperado |
|---|---|---|---|
| T01 | GET `/unidades` | API e seed disponíveis | 200; `items` inclui a unidade inicial. |
| T02 | GET `/unidades/{unidade_id}/cardapio` | Produto disponível com saldo positivo | 200; `items` inclui o produto inicial. |
| T03 | POST `/auth/cadastro` | Nome, CPF válido inédito, nascimento, e-mail inédito e senha | 201; `perfil` igual a `CLIENTE`. |
| T04 | POST `/auth/login` | Credenciais do Cliente | 200; `token` e `expiraEm` preenchidos. |
| T05 | POST `/pedidos` | Token de Cliente, APP, unidade e item com quantidade 1 | 201; `status` `AGUARDANDO_PAGAMENTO`, `situacaoPagamento` `PENDENTE`. |
| T06 | POST `/pedidos/{pedido_aprovado_id}/pagamentos` | T05; mock `APROVADO` | 200; `resultado` `APROVADO`, `statusPedido` `RECEBIDO`. |
| T07 | GET `/pedidos/{pedido_aprovado_id}` | T06; token do proprietário | 200; `status` `RECEBIDO`, `situacaoPagamento` `APROVADO`. |
| T08 | POST `/pedidos/{pedido_aprovado_id}/cancelamento` | T06; pedido ainda não preparado | 200; `status` `CANCELADO`, `situacaoPagamento` `ESTORNADO`. |
| T09 | POST `/pedidos` | Mesmo corpo válido de T05 | 201; novo pedido `AGUARDANDO_PAGAMENTO`. |
| T10 | POST `/pedidos/{pedido_recusado_id}/pagamentos` | T09; reiniciar com mock `RECUSADO` | 200; `resultado` `RECUSADO`, `statusPedido` `AGUARDANDO_PAGAMENTO`. |
| T11 | GET `/pedidos/{pedido_recusado_id}` | T10; token do proprietário | 200; `status` `AGUARDANDO_PAGAMENTO`, `situacaoPagamento` `RECUSADO`. |
| T12 | GET `/pedidos` | Sem token | 401; `codigo` `NAO_AUTENTICADO`. |
| T13 | GET `/estoque` | Token de Cliente | 403; `codigo` `ACESSO_NEGADO`. |
| T14 | POST `/pedidos` | Corpo sem `canalPedido` | 400; `codigo` `DADOS_INVALIDOS`. |
| T15 | POST `/pedidos` | Quantidade `-1` | 400; `codigo` `DADOS_INVALIDOS`. |
| T16 | POST `/pedidos` | Associação de produto inexistente | 404; `codigo` `PRODUTO_NAO_ENCONTRADO`. |
| T17 | POST `/pedidos` | Quantidade 1.000.000, superior ao saldo | 409; `codigo` `ESTOQUE_INSUFICIENTE`. |
| T18 | POST `/pedidos/{pedido_aprovado_id}/cancelamento` | Pedido já cancelado em T08 | 409; `codigo` `CANCELAMENTO_INVALIDO`. |

## Auditoria

Depois de T08, a conexão ao mesmo banco é feita pelo `psql`, conforme o README:

```powershell
psql -h localhost -p 5432 -U raizes -d raizes_nordeste
```

Usuário, porta e banco devem corresponder ao ambiente configurado. No prompt do `psql`, a consulta usa o UUID de `pedido_aprovado_id`:

```sql
SELECT operacao, recurso, resultado
FROM eventos_auditoria
WHERE recurso_id = 'COLE-O-UUID-DO-PEDIDO'::uuid
ORDER BY criado_em;
```

Devem aparecer `CRIAR_PEDIDO / PEDIDO / SUCESSO`, `PAGAR_PEDIDO / PEDIDO / APROVADO` e `CANCELAR_PEDIDO / PEDIDO / SUCESSO`. A consulta acessa diretamente o banco: o MVP não possui endpoint de consulta de auditoria. Pode haver outros eventos relacionados, conforme operações adicionais executadas.

## Registro de execução

Em 26/11/2026, T01–T18 foram executados por cliente HTTP usando método, URL, cabeçalhos e corpo do JSON da coleção, contra API Quarkus local em `localhost:8085` e PostgreSQL 17, com dados fictícios. A recusa foi executada após reiniciar a API com o mock em `RECUSADO`. Todos os status corresponderam à tabela; aprovação, cancelamento e recusa tiveram seus campos de estado conferidos. A consulta SQL encontrou os três eventos descritos acima. A coleção foi posteriormente importada e executada no Insomnia; os resultados e evidências estão em [resultados.md](../../insomnia/resultados.md).

O banco de validação já tinha o esquema e, por isso, a aplicação de migrations foi desativada apenas nessa execução. Para reprodução em banco vazio, o README deve ser seguido com o Flyway habilitado. Dados fictícios permaneceram no banco de validação.

[Voltar ao topo](#testes-do-mvp)
