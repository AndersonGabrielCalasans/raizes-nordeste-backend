# Privacidade e dados pessoais

[Voltar ao índice da documentação](../README.md)

## Sumário

- [Dados e finalidades](#dados-e-finalidades)
- [Bases de tratamento e consentimento](#bases-de-tratamento-e-consentimento)
- [Controles e limitações](#controles-e-limitações)
- [Base normativa](#base-normativa)

## Dados e finalidades

| Dado | Uso documentado | Situação no MVP |
|---|---|---|
| Nome | Identificação do cadastro. | Obrigatório no cadastro de Cliente e equipe. |
| CPF | Restrição de duplicidade de cadastro. | Obrigatório e validado matematicamente; a validação não confirma identidade. |
| Data de nascimento | Possível segmentação por idade em campanhas. | Obrigatória no cadastro, embora as campanhas sejam apenas conceituais; a necessidade da coleta no MVP requer revisão. |
| E-mail | Identificador para login. | Obrigatório e único, sem diferenciação de maiúsculas/minúsculas. |
| Senha | Autenticação. | Não armazenada em texto puro: o sistema armazena hash com salt. |
| Perfil e unidade da equipe | Restrições de acesso e atuação. | Armazenados para autorização; Cliente e Administrador não possuem unidade de atuação. |
| Identificadores e histórico de pedidos | Atendimento, consulta, pagamento simulado e histórico operacional. | Persistidos com unidade, itens, canal, status e tentativa de pagamento. |
| Eventos de auditoria | Rastreabilidade de operações registradas. | Data/hora, responsável quando identificável, operação, recurso e resultado, sem senha ou token completo. |

CPF e data de nascimento são escolhas obrigatórias do cadastro atual; isso não demonstra, por si só, que sejam necessários para todas as finalidades. A necessidade e proporcionalidade da coleta devem ser avaliadas antes de uso real.

## Bases de tratamento e consentimento

A hipótese legal deve ser associada à finalidade e às circunstâncias de cada tratamento. Como hipótese acadêmica sujeita a validação, a execução de contrato ou de procedimentos preliminares a pedido do titular pode fundamentar o tratamento dos dados estritamente necessários ao pedido, conforme o art. 7º, V, da LGPD. Essa hipótese não fundamenta automaticamente todos os dados cadastrais ou qualquer outra finalidade.

Nas regras conceituais aprovadas, a participação em fidelidade e a segmentação de campanhas dependem de consentimento específico. O MVP não implementa coleta, registro ou revogação desse consentimento e não executa essas funcionalidades. As bases de outros usos, incluindo segurança e auditoria, permanecem sem definição; CPF e nascimento obrigatórios exigem justificativa de necessidade antes de uso real.

## Controles e limitações

A aplicação utiliza JWT, hash e salt de senha, autorização por perfil, unidade e propriedade, e registra eventos de operações sensíveis. As consultas autorizadas de pedidos registram o responsável e o recurso consultado, sem conteúdo do pedido; a listagem registra a operação sem identificar cada pedido retornado. Não existe tela de consulta de auditoria no MVP e não há evento específico para toda leitura de dados pessoais. Consultas de pedido para Atendente e Cozinha não retornam CPF, nascimento ou e-mail do Cliente.

Não há prazo definido de retenção nem funcionalidade de exclusão ou anonimização automática. Cadastros e pedidos permanecem armazenados para operação e histórico, sem prazo aprovado; isso é uma limitação, não uma política concluída. Dados fictícios devem ser usados nos ambientes acadêmicos. O registro e a revogação de consentimento e a definição de retenção demandam análise antes de uma implantação real. Este documento não declara conformidade integral com a LGPD.

## Base normativa

A [Lei nº 13.709/2018 — LGPD](https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709compilado.htm) descreve princípios, hipóteses legais e requisitos para o consentimento. A aplicação da lei depende da finalidade e do contexto de cada tratamento.

[Voltar ao topo](#privacidade-e-dados-pessoais)
