# Raízes do Nordeste - Backend
Repositório destinado ao desenvolvimento do Projeto Multidisciplinar do curso Superior de Tecnologia em Análise e Desenvolvimento de Sistemas, contemplando a implementação e a documentação do sistema proposto para o estudo de caso da rede “Raízes do Nordeste”.

## Documentação

O [índice da documentação](docs/README.md) reúne os documentos técnicos por assunto e apresenta o conteúdo de cada entrega. A análise descreve as funcionalidades, regras e critérios de aceitação que orientam o desenvolvimento do backend.

## Execução local

São necessários JDK 21 e PostgreSQL em execução, com `psql` disponível. O Maven Wrapper obtém o Maven e as dependências na primeira execução. A pasta do repositório é acessada pelo PowerShell:

```powershell
cd "C:\caminho\para\raizes-nordeste-backend"
```

No `psql`, conectado como administrador PostgreSQL, o usuário e o banco são criados caso ainda não existam. A senha do exemplo deve ser substituída pelo valor escolhido para o ambiente local:

```sql
CREATE ROLE raizes LOGIN PASSWORD 'senha_local_exemplo';
CREATE DATABASE raizes_nordeste OWNER raizes;
```

Se ainda não houver um `.env`, o arquivo de exemplo é copiado e seus valores são ajustados conforme o banco local:

```powershell
Copy-Item .env.example .env
```

O arquivo deve conter `DB_JDBC_URL`, `DB_USERNAME` e `DB_PASSWORD`. Como alternativa ao `.env`, essas variáveis podem ser definidas na sessão do PowerShell:

```powershell
$env:DB_JDBC_URL = "jdbc:postgresql://localhost:5432/raizes_nordeste"
$env:DB_USERNAME = "raizes"
$env:DB_PASSWORD = "senha_local_exemplo"
```

Para habilitar a assinatura dos tokens, as chaves locais são geradas uma única vez com PowerShell 7:

```powershell
pwsh -File .\scripts\gerar-chaves.ps1
```

No `.env`, são configurados `JWT_PRIVATE_KEY_PATH=keys/privateKey.pem` e `JWT_PUBLIC_KEY_PATH=keys/publicKey.pem`. As chaves são locais e não são versionadas.

Na pasta do projeto, a aplicação é iniciada com:

```powershell
.\mvnw.cmd quarkus:dev
```

A aplicação estará disponível em `http://localhost:8080`; a saúde da aplicação e do banco pode ser consultada em `http://localhost:8080/q/health`. Para encerrar, utiliza-se `Ctrl+C` no terminal.

A documentação interativa das rotas implementadas está em [Swagger UI](http://localhost:8080/q/swagger-ui/); a especificação OpenAPI está em [OpenAPI](http://localhost:8080/q/openapi). O [guia de utilização](docs/api/api.md) apresenta autenticação e exemplos de preenchimento.

Ao iniciar, o Flyway aplica as migrations em `src/main/resources/db/migration` em um banco vazio. O arquivo `.env` não é versionado.

Após a primeira inicialização, os dados fictícios são carregados em outro terminal, na pasta do projeto. São necessários um CPF fictício válido e uma senha local com pelo menos oito caracteres, sem espaços:

```powershell
$cpfAdmin = Read-Host "CPF fictício do administrador (11 dígitos)"
$senhaAdmin = Read-Host "Senha local do administrador"
psql -h localhost -p 5432 -U raizes -d raizes_nordeste -v ON_ERROR_STOP=1 -v "admin_cpf=$cpfAdmin" -v "admin_senha=$senhaAdmin" -f migrations/dados-iniciais.sql
```

O script usa a extensão PostgreSQL `pgcrypto` e pode ser repetido sem sobrescrever os registros existentes. A conta inicial usa `admin@raizes.local`, com senha protegida por bcrypt, custo 12 e salt individual. No Windows, se `psql` não estiver no PATH, pode ser usado o caminho do executável, por exemplo `& "C:\Program Files\PostgreSQL\17\bin\psql.exe"`.

## Testes do MVP

Com o banco local preparado, migrations e seed aplicados, a API é iniciada e a [coleção Insomnia](insomnia/raizes-insomnia.json) é importada. O [guia de testes](docs/testes/testes.md) explica o ambiente, o token, a ordem das requisições, a simulação de pagamento e a conferência da auditoria. Os resultados estão registrados em [insomnia/resultados.md](insomnia/resultados.md). Os cenários exigem dados fictícios e um banco de testes, pois criam pedidos e movimentam estoque.

O [plano de desempenho e recuperação](docs/testes/desempenho-recuperacao.md) documenta a carga local, a preparação isolada e os resultados de RNF05 e RNF06.

Para executar os testes automatizados do projeto:

```powershell
.\mvnw.cmd -B -ntp '-Dquarkus.http.test-port=0' verify
```

## Referências

- [Quarkus — Using Flyway](https://quarkus.io/guides/flyway)
- [Quarkus — Getting Started](https://quarkus.io/guides/getting-started)
- [Quarkus — Using JWT RBAC](https://quarkus.io/guides/security-jwt)
- [Quarkus — Using OpenAPI and Swagger UI](https://quarkus.io/guides/openapi-swaggerui)

## Acompanhamento

- [Tarefas](https://github.com/AndersonGabrielCalasans/raizes-nordeste-backend/issues)
- [Quadro do projeto](https://github.com/users/AndersonGabrielCalasans/projects/2)
- [Pull requests](https://github.com/AndersonGabrielCalasans/raizes-nordeste-backend/pulls)
