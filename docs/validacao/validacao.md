# Conferência da documentação e dos entregáveis

[Voltar ao índice da documentação](../README.md)

## Sumário

- [Escopo](#escopo)
- [Cobertura](#cobertura)
- [Base do projeto](#base-do-projeto)

## Escopo

A conferência relaciona o roteiro Back-End, a implementação e as evidências disponíveis. **Atendido** indica que o artefato ou comportamento foi identificado e possui evidência adequada ao recorte; **parcial** indica limitação relevante; **pendente** indica trabalho ainda não concluído. O atendimento integral permanece condicionado aos pontos parciais e pendentes abaixo.

## Cobertura

| Ponto do roteiro | Situação | Evidência ou limitação |
|---|---|---|
| Cadastro, autenticação, perfis e permissões | Atendido no MVP | RF01–RF02; [guia da API](../api/api.md) e cenários T03, T04, T12 e T13. |
| Unidades, cardápio, pedido, status e estoque | Atendido no MVP | RF03–RF07; [contratos](../contratos/contratos.md), migrations, testes e fluxo crítico. |
| Multicanalidade | Parcial por recorte | APP, WEB e BALCAO são implementados, com registro e filtro por canal. TOTEM e PICKUP constam no domínio e como filtro, mas não são aceitos na criação. |
| Fidelidade e campanhas | Conceitual | RF09–RF10 descrevem regras, aceite e consentimento; não há endpoints, persistência ou testes dessas funcionalidades. |
| Pagamento externo simulado | Parcial | Solicitação, resultado, registro e transições funcionam com um adaptador mock local síncrono. Não há serviço externo HTTP nem callback; o roteiro menciona serviço externo mock. |
| Proteção e auditoria | Parcial | JWT, hash de senha, restrição por perfil/unidade/propriedade e eventos sensíveis foram implementados. Consultas autorizadas de pedidos geram eventos; não há evento específico para toda leitura de dados pessoais e a retenção não tem prazo definido. |
| Privacidade e bases de tratamento | Parcial | A [análise de privacidade](../privacidade/privacidade.md) registra hipóteses para o pedido e consentimento conceitual. A base dos demais usos, a necessidade dos campos e a retenção seguem pendentes. |
| Erro uniforme | Parcial | Erros de negócio, recusas de segurança e falhas internas representadas por `IllegalStateException` usam `codigo`, `mensagem` e `detalhes`. O formato uniforme para outras falhas internas não está comprovado. |
| Diagramas e arquitetura | Parcial | Casos de uso, DER, classes, sequência e arquitetura estão em [modelagem](../modelagem/modelagem.md) e [arquitetura](../arquitetura/arquitetura.md). O roteiro exige DER em imagem/PDF, ainda não exportado. O diagrama de classes é uma visão conceitual; a aplicação usa serviços, portas e registros. |
| Execução e contratos | Parcial | README, `.env.example`, Flyway, seed, Swagger e [coleção Insomnia](../../insomnia/raizes-insomnia.json) estão disponíveis. O teste OpenAPI verifica rotas e exemplos de sucesso, mas nem todas as respostas de erro têm exemplos e esquema documentados no Swagger. |
| Testes funcionais | Atendido no recorte testado | [18 cenários e evidências](../../insomnia/resultados.md), cobrindo autenticação, permissão, validação, estoque e pagamento aprovado/recusado; auditoria do pedido verificada no banco. |
| Desempenho e recuperação | Atendido no cenário local | [Medição de 10 clientes por cinco minutos e recuperação](../testes/desempenho-recuperacao.md), com limites do ambiente e pagamento avaliado separadamente. Não comprova capacidade real da rede. |
| Entrega acadêmica | Pendente | PDF único, capa, sumário, seções, referências e formatação conforme orientações do AVA; inclusão do DER exportado e conferência dos links. |

## Base do projeto

A análise confronta o **Roteiro de Atividade Prática — Projeto Back-End**, páginas 5–13 e tabela consolidada na página 18, e o **Projeto Multidisciplinar — Estudo de Caso**, com as decisões registradas nos requisitos funcionais e não funcionais. A paginação inclui a capa dos arquivos. O roteiro exige declaração do uso de IA; o [registro de assistência](assistencia.md) descreve os usos identificados e sua limitação frente à regra institucional.

[Voltar ao topo](#conferência-da-documentação-e-dos-entregáveis)
