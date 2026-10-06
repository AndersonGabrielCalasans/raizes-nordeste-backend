# Requisitos não funcionais

[Voltar ao índice da documentação](../README.md)

## Índice

- [RNF01 — Proteger credenciais e autenticação](#rnf01--proteger-credenciais-e-autenticação)
- [RNF02 — Controlar acesso às operações e aos dados](#rnf02--controlar-acesso-às-operações-e-aos-dados)
- [RNF03 — Proteger dados pessoais](#rnf03--proteger-dados-pessoais)
- [RNF04 — Registrar logs e auditoria](#rnf04--registrar-logs-e-auditoria)

## RNF01 — Proteger credenciais e autenticação

**Descrição:** Proteger senhas e validar tokens de acesso.

**Regras:**

- Senha com mínimo de 8 caracteres, sem espaços.
- Armazenamento por hash próprio para senhas, com salt individual.
- JWT válido por 60 minutos, com assinatura e expiração verificadas.
- Não expor senhas, hashes ou tokens completos em respostas e logs.
- Mensagem genérica para credenciais inválidas.

**Aceitação:** cadastro recusa senha fora da política; senhas não são armazenadas em texto puro; tokens adulterados ou expirados são recusados; respostas e logs não expõem credenciais; credenciais inválidas retornam mensagem genérica.

**Cobertura:** implementação prevista no MVP, em apoio a RF01 e RF02.

[Voltar ao topo](#requisitos-não-funcionais)

## RNF02 — Controlar acesso às operações e aos dados

**Descrição:** Restringir acesso por perfil, unidade e propriedade do pedido.

**Regras:**

- Consultas públicas de unidades e cardápio, cadastro público de Cliente e login dispensam token.
- Operações protegidas exigem JWT válido.
- Cliente acessa somente seus pedidos.
- Atendente e Cozinha atuam apenas na unidade vinculada e nas operações permitidas ao perfil.
- Administrador acessa as operações administrativas entre unidades.
- Consulta direta e filtros respeitam as mesmas permissões.
- Aplicar as condições de pagamento e status definidas nos RFs.

**Aceitação:** consultas públicas, cadastro público e login funcionam sem token, respeitando suas validações; operação protegida sem token válido retorna 401; acesso autenticado sem permissão retorna 403, sem expor dados ou executar alterações; operações autorizadas são permitidas.

**Cobertura:** implementação prevista no MVP, conforme RF01–RF08. Permissões de funcionalidades conceituais permanecem conceituais.

[Voltar ao topo](#requisitos-não-funcionais)

## RNF03 — Proteger dados pessoais

**Descrição:** Limitar o uso e a exposição dos dados pessoais às finalidades definidas.

**Regras:**

- Nome identifica o cadastro; CPF evita duplicidade; e-mail permite autenticação; nascimento atende à segmentação por idade, apenas conceitual.
- Consultas públicas não expõem dados pessoais. Respostas protegidas retornam somente dados necessários à operação e ao perfil.
- Atendente e Cozinha consultam identificador do pedido, itens, canal e status, sem CPF, nascimento ou e-mail do Cliente.
- Uso em fidelidade e campanhas segmentadas exige consentimento registrado e revogável, conforme RF09 e RF10. O MVP não utiliza dados para essas funcionalidades.
- Conceitualmente, cadastros e pedidos são mantidos para operação e histórico. Não há prazo de retenção definido nem exclusão automática no MVP.

**Aceitação:** consultas públicas não retornam dados pessoais; perfis sem permissão não os acessam; consultas de Atendente e Cozinha não expõem CPF, nascimento ou e-mail do Cliente. Na cobertura conceitual, fidelidade e segmentação respeitam consentimento e revogação.

**Cobertura:** proteção e restrição de exposição no MVP. Consentimento específico e política de retenção apenas conceituais; encerramento de cadastro e anonimização não integram o recorte. Este requisito não representa conformidade integral com a LGPD.

[Voltar ao topo](#requisitos-não-funcionais)

## RNF04 — Registrar logs e auditoria

**Descrição:** Registrar eventos de acesso e operações sensíveis em tabela no banco de dados.

**Regras:**

- Registrar login, recusas de acesso e acesso a dados pessoais.
- Registrar criação, cancelamento e mudança de status de pedidos, movimentações de estoque e resultados de pagamento.
- Cada registro contém data/hora, responsável quando identificável, operação, identificador do recurso quando aplicável e resultado.
- Não registrar senhas, hashes, tokens completos ou dados pessoais completos; identificar responsáveis por identificador interno, quando disponível.
- Persistir os registros em tabela no banco, sem tela de consulta no MVP.

**Aceitação:** os eventos definidos geram registros consultáveis no banco com seus dados e resultado; tentativas sem responsável autenticado também são registradas; os registros não expõem credenciais ou dados pessoais completos.

**Cobertura:** implementação prevista no MVP para operações implementadas. Eventos de funcionalidades conceituais permanecem conceituais.

[Voltar ao topo](#requisitos-não-funcionais)
