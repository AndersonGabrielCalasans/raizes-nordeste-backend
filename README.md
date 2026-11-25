# Raízes do Nordeste - Backend
Repositório destinado ao desenvolvimento do Projeto Multidisciplinar do curso Superior de Tecnologia em Análise e Desenvolvimento de Sistemas, contemplando a implementação e a documentação do sistema proposto para o estudo de caso da rede “Raízes do Nordeste”.

## Documentação

O [índice da documentação](docs/README.md) reúne os documentos técnicos por assunto e apresenta o conteúdo de cada entrega. A análise descreve as funcionalidades, regras e critérios de aceitação que orientam o desenvolvimento do backend.

## Execução local

Requer Java 21 e PostgreSQL. Defina as variáveis abaixo com os valores da conexão local (`.env.example` registra o formato) e execute:

```powershell
$env:DB_JDBC_URL = "jdbc:postgresql://localhost:5432/raizes_nordeste"
$env:DB_USERNAME = "raizes"
$env:DB_PASSWORD = "altere_esta_senha"
.\mvnw.cmd quarkus:dev
```

Ao iniciar, o Flyway aplica as migrations em `src/main/resources/db/migration` em um banco vazio. A configuração usa `DB_JDBC_URL`, `DB_USERNAME` e `DB_PASSWORD`; o arquivo `.env` não é versionado.

## Referências

- [Quarkus — Using Flyway](https://quarkus.io/guides/flyway)
- [Quarkus — Getting Started](https://quarkus.io/guides/getting-started)

## Acompanhamento

- [Tarefas](https://github.com/AndersonGabrielCalasans/raizes-nordeste-backend/issues)
- [Quadro do projeto](https://github.com/users/AndersonGabrielCalasans/projects/2)
- [Pull requests](https://github.com/AndersonGabrielCalasans/raizes-nordeste-backend/pulls)
