# Raízes do Nordeste - Backend
Repositório destinado ao desenvolvimento do Projeto Multidisciplinar do curso Superior de Tecnologia em Análise e Desenvolvimento de Sistemas, contemplando a implementação e a documentação do sistema proposto para o estudo de caso da rede “Raízes do Nordeste”.

## Documentação

O [índice da documentação](docs/README.md) reúne os documentos técnicos por assunto e apresenta o conteúdo de cada entrega. A análise descreve as funcionalidades, regras e critérios de aceitação que orientam o desenvolvimento do backend.

## Execução local

Requer JDK 21 e PostgreSQL em execução, com o banco `raizes_nordeste` criado. No PowerShell, acesse a pasta onde o repositório foi clonado:

```powershell
cd "C:\caminho\para\raizes-nordeste-backend"
```

Se ainda não houver um `.env`, copie o arquivo de exemplo e ajuste os valores conforme o banco local:

```powershell
Copy-Item .env.example .env
```

O arquivo deve conter `DB_JDBC_URL`, `DB_USERNAME` e `DB_PASSWORD`. Como alternativa ao `.env`, essas variáveis podem ser definidas na sessão do PowerShell:

```powershell
$env:DB_JDBC_URL = "jdbc:postgresql://localhost:5432/raizes_nordeste"
$env:DB_USERNAME = "raizes"
$env:DB_PASSWORD = "altere_esta_senha"
```

Na pasta do projeto, inicie a aplicação:

```powershell
.\mvnw.cmd quarkus:dev
```

A aplicação estará disponível em `http://localhost:8080`; a saúde da aplicação e do banco pode ser consultada em `http://localhost:8080/q/health`. Para encerrar, pressione `Ctrl+C` no terminal.

Ao iniciar, o Flyway aplica as migrations em `src/main/resources/db/migration` em um banco vazio. O arquivo `.env` não é versionado.

## Referências

- [Quarkus — Using Flyway](https://quarkus.io/guides/flyway)
- [Quarkus — Getting Started](https://quarkus.io/guides/getting-started)

## Acompanhamento

- [Tarefas](https://github.com/AndersonGabrielCalasans/raizes-nordeste-backend/issues)
- [Quadro do projeto](https://github.com/users/AndersonGabrielCalasans/projects/2)
- [Pull requests](https://github.com/AndersonGabrielCalasans/raizes-nordeste-backend/pulls)
