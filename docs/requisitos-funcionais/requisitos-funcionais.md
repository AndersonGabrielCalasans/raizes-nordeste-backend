# Requisitos funcionais

[Voltar ao índice da documentação](../README.md)

## Índice

- [Fluxo principal e justificativa](#fluxo-principal-e-justificativa)
- [RF01 — Cadastrar usuários e definir perfil e unidade](#rf01--cadastrar-usuários-e-definir-perfil-e-unidade)
- [RF02 — Autenticar usuários e controlar acesso](#rf02--autenticar-usuários-e-controlar-acesso)
- [Fontes](#fontes)

## Fluxo principal e justificativa

O fluxo principal é Pedido → Pagamento mock → Status. Sua escolha decorre de representar a operação central da aplicação e permitir a cobertura de boa parte dos requisitos obrigatórios do caso: criação de pedidos, integração de pagamento simulado e acompanhamento de sua evolução até a entrega. Esse fluxo conecta os comportamentos em uma operação completa, em vez de limitar a implementação a cadastros isolados.

A cobertura de cada requisito distingue implementação prevista no MVP e descrição conceitual.

## RF01 — Cadastrar usuários e definir perfil e unidade

**Descrição:** Permitir o cadastro público de clientes e o cadastro de funcionários por um Administrador, com atribuição de perfil e unidade de atuação quando aplicável.

**Dados obrigatórios:** nome completo, CPF, data de nascimento, e-mail e senha. Atendente e Cozinha também devem possuir uma unidade de atuação.

**Regras:**

- O cadastro público atribui somente o perfil Cliente. O Administrador pode cadastrar Atendente e Cozinha.
- No MVP, cada cadastro possui um perfil, sem herança automática de permissões. As contas administrativas iniciais são provisionadas na preparação do ambiente.
- Atendente e Cozinha atuam em uma unidade por vez. O Administrador pode alterar esse vínculo para outra unidade existente.
- CPF e e-mail são únicos. O CPF é comparado sem pontuação; o e-mail, sem espaços externos e sem diferenciação de maiúsculas/minúsculas.
- O nome não pode estar vazio; CPF e e-mail devem ser válidos; nascimento deve ser uma data válida e não futura; senha deve atender à política de segurança.
- CPF evita duplicidade, mas sua validação não comprova identidade. Nascimento subsidia campanhas por idade.
- O atendente não cadastra clientes no balcão. No MVP, os pedidos de balcão são anônimos e registrados por um Atendente autenticado. A vinculação a um cliente existente mediante autenticação do próprio cliente possui cobertura conceitual.

**Critérios de aceitação:**

1. Com dados válidos e identificadores disponíveis, o cadastro público cria um Cliente, sem expor sua senha na resposta.
2. Um Administrador autenticado consegue cadastrar Atendente ou Cozinha com unidade existente; solicitantes sem permissão têm a operação recusada.
3. Dados obrigatórios ausentes ou inválidos, CPF/e-mail duplicados ou perfil não autorizado impedem o cadastro, sem registro parcial ou alteração de conta existente.
4. Cadastro operacional sem unidade ou com unidade inexistente é recusado. Alteração autorizada para unidade válida mantém somente um vínculo vigente; alteração inválida preserva o vínculo anterior.
5. Associação de múltiplos perfis é recusada no MVP.

**Cobertura:** cadastro, validações, perfil único e vínculo por unidade serão implementados. Owner e múltiplos perfis são conceituais: somente Owner poderá atribuir Administrador; Owner e Administrador poderão associar perfis operacionais a um cadastro existente, sem duplicar CPF/e-mail e preservando as restrições por unidade. A aceitação dessa extensão requer permitir associações autorizadas e recusar atribuição de Administrador por outro perfil; sua execução não integra os testes do MVP.

**Fora do MVP:** recuperação de senha, confirmação por e-mail e notificações.

[Voltar ao topo](#requisitos-funcionais)

## RF02 — Autenticar usuários e controlar acesso

**Descrição:** Permitir a autenticação por e-mail e senha e restringir operações protegidas conforme a identidade, o perfil e a unidade de atuação.

**Atores:** Cliente, Atendente, Cozinha e Administrador.

**Regras:**

- Credenciais válidas permitem a emissão de um token JWT. As operações protegidas exigem token válido, com assinatura e prazo de validade verificados.
- Credenciais inválidas produzem a mensagem “E-mail ou senha inválidos”, sem revelar se o e-mail está cadastrado.
- A consulta do cardápio é pública. O Cliente deve se autenticar para criar pedidos pelo aplicativo e consultar somente os próprios pedidos.
- Atendente e Cozinha acessam apenas operações permitidas ao seu perfil e dados da sua unidade de atuação. O Administrador acessa as operações administrativas das unidades da rede.
- A ausência de token ou apresentação de token inválido ou expirado impede o acesso às operações protegidas. Autenticação válida não autoriza operações incompatíveis com o perfil, acesso a pedidos de outros clientes ou operações fora da unidade permitida.
- Pedidos anônimos no balcão dispensam conta de Cliente, mas exigem Atendente autenticado. A vinculação ao Cliente autenticado é conceitual.

**Critérios de aceitação:**

1. Credenciais válidas retornam um JWT com prazo de validade, sem expor a senha. Credenciais inválidas não emitem token e retornam a mensagem definida.
2. A consulta pública do cardápio funciona sem token.
3. Uma operação protegida sem token, com assinatura inválida ou com token expirado é recusada, sem executar a operação.
4. Com token válido, as operações autorizadas são permitidas; operações incompatíveis com o perfil são recusadas.
5. Um Cliente não consegue consultar pedidos de outro Cliente. Atendente e Cozinha não conseguem acessar operações ou dados de outra unidade.
6. Um Atendente autenticado consegue registrar pedido de balcão sem cadastro de Cliente; a mesma operação sem autenticação é recusada.

**Cobertura:** autenticação por JWT, autorização por perfil, restrições por unidade e propriedade de pedidos terão implementação no MVP. A vinculação de pedido de balcão a Cliente autenticado permanece conceitual: somente uma autenticação bem-sucedida do próprio Cliente pode autorizar o vínculo; informar apenas seu CPF não deve permitir essa associação.

[Voltar ao topo](#requisitos-funcionais)

## Fontes

- **Roteiro de Atividade Prática — Projeto Back-End:** páginas 5–6 do PDF, cadastro, autenticação, perfis e controle de acesso; página 9, privacidade e segurança; página 11, cenários de autenticação e autorização.
- **Projeto Multidisciplinar — Estudo de Caso Raízes do Nordeste:** página 5 do PDF, equipes por unidade e campanhas segmentadas por idade.

As páginas consideram a posição no PDF, incluindo a capa. Dados obrigatórios, unicidade, provisionamento inicial, limites dos perfis, JWT, consulta pública do cardápio e divisão entre implementação e cobertura conceitual são decisões de escopo do projeto.

[Voltar ao topo](#requisitos-funcionais)
