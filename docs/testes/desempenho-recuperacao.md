# Desempenho e recuperação

[Voltar ao índice da documentação](../README.md)

## Sumário

- [Cenário de carga](#cenário-de-carga)
- [Preparação e reprodução](#preparação-e-reprodução)
- [Consistência](#consistência)
- [Pagamento e recuperação](#pagamento-e-recuperação)
- [Resultado da execução](#resultado-da-execução)

## Cenário de carga

O RNF05 define um pico local de dez usuários simultâneos por cinco minutos: pelo menos 95% das respostas em até dois segundos, sem erros internos ou inconsistência em pedidos e estoque. O pagamento é avaliado separadamente. Esses parâmetros são metas do projeto, e não uma estimativa de capacidade real da rede.

O gerador [carga_mvp.py](../../scripts/testes/carga_mvp.py) usa dez contas Cliente distintas, autentica antes da medição e mantém dez trabalhadores por 300 segundos. Cada trabalhador inicia, quando possível, uma requisição por segundo e alterna entre consulta do cardápio, consulta de pedidos e criação de pedido APP com um item. Requisições que ultrapassam o intervalo atrasam as seguintes; não há usuários adicionais para compensação. Assim, a taxa efetiva pode ser inferior a dez requisições por segundo.

O relatório registra quantidades, tempos, status e falhas. O percentil 95 usa o método de posição ordinal (`ceil(0,95 × quantidade)`). O percentual de até dois segundos considera **todas as requisições iniciadas**, tratando erros de transporte como não atendidos; respostas HTTP de qualquer status entram no cálculo, e qualquer status diferente do esperado é contado como falha. A avaliação de consistência no PostgreSQL é separada e não consta automaticamente do JSON.

## Preparação e reprodução

A execução exige JDK 21, PostgreSQL, Python 3 e as chaves JWT locais descritas no [README](../../README.md). A aplicação e o gerador devem apontar para um banco exclusivo de testes: a carga cria pedidos, reduz estoque e mantém os dados gerados. Para executar o teste de indisponibilidade, o PostgreSQL também precisa ser uma **instância separada**, que não atenda outros bancos de uso corrente.

1. Um banco vazio é criado na instância de testes. `DB_JDBC_URL`, `DB_USERNAME` e `DB_PASSWORD` são configurados para esse banco; a API é iniciada, permitindo que o Flyway aplique as migrations. As chaves JWT são configuradas conforme o README.
2. Os dados iniciais são carregados conforme o README. No banco exclusivo de testes, o saldo do produto usado na carga é ajustado para 5.000 antes do experimento:

   ```sql
   UPDATE produtos_unidades SET saldo_estoque = 5000
   WHERE id = '30000000-0000-0000-0000-000000000001';
   ```

   O ajuste é apenas preparação do ambiente descartável, não uma operação da API. Para repetir a medição e comparar contagens desde zero, é necessário um banco novo ou um registro dos totais iniciais.
3. Com a API isolada iniciada na porta 8085, o PowerShell na pasta do repositório prepara dez clientes fictícios e executa a carga:

   ```powershell
   $env:RAIZES_TESTE_SENHA = 'SenhaCarga123'
   python scripts/testes/preparar_clientes.py --url http://127.0.0.1:8085
   $env:RAIZES_TESTE_EMAILS = ((1..10 | ForEach-Object { 'carga{0:D2}@example.com' -f $_ }) -join ',')
   python scripts/testes/carga_mvp.py --url http://127.0.0.1:8085 --usuarios 10 --duracao 300 --intervalo 1 --saida resultado-carga-local.json
   ```

   A preparação verifica o login das contas já existentes e interrompe a execução se houver conflito de CPF ou credenciais. As credenciais são fornecidas por variáveis de ambiente e não são gravadas no relatório de carga. O arquivo de saída deve ter nome distinto do resultado histórico versionado.
4. Os testes unitários do gerador são executados por `python -m unittest discover -s scripts/testes -p 'test_*.py' -v`.

## Consistência

Antes e depois da carga, é executada a seguinte consulta no banco de testes:

```sql
SELECT saldo_estoque,
       (SELECT count(*) FROM pedidos) AS pedidos,
       (SELECT count(*) FROM itens_pedido) AS itens,
       (SELECT count(*) FROM movimentacoes_estoque WHERE origem = 'PEDIDO') AS movimentos
FROM produtos_unidades
WHERE id = '30000000-0000-0000-0000-000000000001';
```

Em um banco sem outros acessos simultâneos e sem cancelamentos no intervalo, a diferença do estoque deve ser igual à quantidade de criações HTTP `201`. Os acréscimos de pedidos, itens e movimentos de saída também devem corresponder à mesma quantidade. A verificação adicional de pedidos sem item e movimentos duplicados detecta inconsistências que uma comparação apenas do saldo poderia ocultar.

```sql
SELECT
  (SELECT count(*) FROM pedidos p WHERE NOT EXISTS
    (SELECT 1 FROM itens_pedido i WHERE i.pedido_id = p.id)) AS pedidos_sem_item,
  (SELECT count(*) FROM (SELECT pedido_id FROM movimentacoes_estoque
    WHERE origem = 'PEDIDO' GROUP BY pedido_id HAVING count(*) <> 1) x) AS saidas_duplicadas,
  (SELECT count(*) FROM pedidos p WHERE NOT EXISTS
    (SELECT 1 FROM movimentacoes_estoque m WHERE m.pedido_id = p.id
     AND m.origem = 'PEDIDO' AND m.tipo = 'SAIDA' AND m.quantidade = 1)) AS pedidos_sem_saida_esperada;
```

Essa consulta adicional pressupõe um único item de quantidade 1 por pedido no banco isolado, como no cenário executado.

## Pagamento e recuperação

O pagamento é medido fora da carga de RNF05, usando pedido fictício e mock local `APROVADO`; o resultado é uma amostra funcional, não uma medição estatística de desempenho de gateway externo.

O RNF06 é verificado com uma instância exclusiva da API e outra exclusiva do PostgreSQL. Com dados confirmados, a API é reiniciada e os dados são consultados novamente. Em seguida, o PostgreSQL isolado é interrompido; uma operação dependente do banco não deve retornar sucesso. Por fim, o banco é reiniciado e a API é reiniciada; a consulta dos dados anteriores e uma nova operação confirmam a recuperação. Nenhum SLA percentual é inferido desse procedimento.

Na execução local em Windows, o cluster isolado foi criado com `initdb` em uma pasta própria, iniciado com `pg_ctl` na porta 55433 e acessível apenas em `127.0.0.1`. A API de teste usou a porta 8085. O procedimento equivalente, com os caminhos ajustados para um diretório **novo e exclusivo de testes**, é:

```powershell
$pgBin = 'C:\Program Files\PostgreSQL\17\bin'
$pgDados = 'C:\caminho\para\cluster-exclusivo-de-teste'
& "$pgBin\initdb.exe" -D $pgDados -U raizes -A trust -E UTF8 --no-locale
& "$pgBin\pg_ctl.exe" -D $pgDados -l "$pgDados.log" -o '-p 55433 -h 127.0.0.1' -w start
& "$pgBin\createdb.exe" -h 127.0.0.1 -p 55433 -U raizes raizes_teste_rnf
```

O modo `trust` é restrito a esse cluster local descartável. Com as chaves JWT configuradas no `.env` conforme o README, a API isolada é iniciada na pasta do repositório:

```powershell
$env:DB_JDBC_URL = 'jdbc:postgresql://127.0.0.1:55433/raizes_teste_rnf'
$env:DB_USERNAME = 'raizes'
$env:DB_PASSWORD = 'teste-local'
$env:RAIZES_PAGAMENTO_MOCK = 'APROVADO'
.\mvnw.cmd quarkus:dev '-Dquarkus.http.port=8085' '-Dquarkus.devservices.enabled=false'
```

Após a inicialização e aplicação das migrations, outro terminal PowerShell é usado na pasta do repositório para carregar o seed e ajustar o saldo apenas no banco de testes:

```powershell
$pgBin = 'C:\Program Files\PostgreSQL\17\bin'
& "$pgBin\psql.exe" -h 127.0.0.1 -p 55433 -U raizes -d raizes_teste_rnf -v ON_ERROR_STOP=1 -v 'admin_cpf=11144477735' -v 'admin_senha=TesteLocal123' -f migrations/dados-iniciais.sql
& "$pgBin\psql.exe" -h 127.0.0.1 -p 55433 -U raizes -d raizes_teste_rnf -v ON_ERROR_STOP=1 -c "UPDATE produtos_unidades SET saldo_estoque=5000 WHERE id='30000000-0000-0000-0000-000000000001';"
```

O pedido e o pagamento da recuperação são criados pela coleção do Insomnia, com `base_url` apontando para `http://127.0.0.1:8085`. O identificador do pedido e os totais de usuários, pedidos, tentativas e estoque são registrados antes da interrupção. Depois da carga, o estoque restante deve permitir pelo menos dois pedidos adicionais para esse teste.

Com a API de teste ainda em execução, somente o cluster indicado por `$pgDados` é interrompido com `& "$pgBin\pg_ctl.exe" -D $pgDados -m fast -w stop`. A consulta autenticada do pedido pela API deve falhar. O cluster é iniciado novamente com o mesmo comando `pg_ctl ... start`; a API de teste é reiniciada, e os totais e o pedido pago são conferidos antes de criar um novo pedido. Ao final, a API e o cluster de teste são encerrados. O serviço PostgreSQL principal não é interrompido.

## Resultado da execução

Em 26/11/2026, a execução ocorreu em Windows 11 Home, Intel Core i5-12450H (8 núcleos/12 threads), 15,7 GiB de memória, Java 21.0.4, Quarkus 3.40.1.2 em modo de desenvolvimento, PostgreSQL 17.2 e Python 3.12.14. A API e o gerador executaram na mesma máquina, com outros processos locais ativos, PostgreSQL e API isolados. O Flyway aplicou as migrations V1 e V2 no banco de testes vazio.

| Medida | Resultado |
|---|---|
| Duração e usuários | 300 s; 10 Clientes distintos e simultâneos |
| Requisições | 3.000: 1.000 cardápios, 1.000 consultas de pedidos e 1.000 criações |
| Respostas | 2.000 HTTP 200 e 1.000 HTTP 201; nenhuma falha de expectativa, 5xx ou transporte |
| Tempos | mínimo 2,05 ms; mediana 17,44 ms; p95 57,25 ms; máximo 166,14 ms |
| Limite de dois segundos | 3.000/3.000 (100%); meta local de 95% atingida |

Os resultados estão em [resultado-carga-rnf05.json](resultado-carga-rnf05.json). A primeira tentativa curta, usando `localhost`, apresentou latências próximas a dois segundos; ao usar `127.0.0.1` no ensaio seguinte, as respostas ficaram entre 4,82 e 34,39 ms. A medição formal utilizou o endereço IPv4 direto; a diferença observada é uma limitação do ambiente de medição, sem diagnóstico conclusivo da resolução de nomes.

Na verificação de consistência, o estoque passou de 4.997 para 3.997 e pedidos, itens e movimentos passaram de 3 para 1.003: mil acréscimos de cada, correspondentes aos mil HTTP 201. Não foram encontrados pedidos sem item, saídas duplicadas ou pedidos sem a saída esperada. Os três pedidos anteriores vieram de ensaios curtos e foram incluídos no valor inicial.

Fora da carga, uma solicitação ao mock local retornou HTTP 200, `APROVADO` e pedido `RECEBIDO` em 10,03 ms. Trata-se de uma única amostra, sem generalização sobre latência de integração externa.

Para RNF06, após reiniciar a API, a consulta do pedido pago retornou HTTP 200. Antes e depois do reinício da API e da interrupção do banco, o resumo permaneceu em 12 usuários, 1.004 pedidos, uma tentativa de pagamento, saldo 3.996 e uma aprovação para o pedido observado. Com o banco isolado interrompido, a consulta retornou HTTP 500, sem sucesso indevido. Após restabelecer o banco e reiniciar a API, a consulta retornou 200 e uma nova criação retornou 201; os totais posteriores foram 12 usuários, 1.005 pedidos, uma tentativa e saldo 3.995. O resultado estruturado está em [resultado-recuperacao-rnf06.json](resultado-recuperacao-rnf06.json).

A observação de erro HTTP 500 demonstra que a consulta falhou nesse teste; não comprova tratamento padronizado de todas as falhas de banco. A instância isolada foi encerrada ao final e o banco principal permaneceu em execução.

## Reexecução com auditoria de consultas

Em 26/11/2026, após a inclusão de auditoria nas consultas de pedidos, o cenário foi repetido na mesma configuração local. Foram 3.000 requisições em 300 segundos: 1.000 consultas de cardápio, 1.000 listagens de pedidos e 1.000 criações. Todas responderam em até dois segundos; não houve falhas, respostas 5xx ou erros de transporte. A mediana foi 16,64 ms, o p95 36,46 ms e o máximo 90,11 ms. O [resultado estruturado](resultado-carga-rnf05-auditoria.json) registra a execução.

Antes da carga havia 1.005 pedidos, itens e movimentos, com saldo 3.995. A conferência posterior incluiu um pedido adicional criado para o teste de recuperação: foram observados 2.006 pedidos, itens e movimentos e saldo 2.994. A diferença corresponde às 1.000 criações da carga e à criação adicional. Não foram encontrados pedidos sem item, saídas duplicadas ou pedidos sem a saída esperada. As 1.000 listagens geraram 1.000 eventos `CONSULTAR_PEDIDOS`; uma consulta individual gerou `CONSULTAR_PEDIDO`.

Com o PostgreSQL isolado interrompido, a consulta do pedido retornou HTTP 500 com `codigo` igual a `ERRO_INTERNO` e sem detalhes do banco. Após o restabelecimento do banco, a primeira consulta ainda retornou 500; após reiniciar também a API, a consulta retornou 200 e uma nova criação retornou 201. A recuperação observada, portanto, exigiu reinício da API nesse ensaio. A API e o cluster isolados foram encerrados ao final.

[Voltar ao topo](#desempenho-e-recuperação)
