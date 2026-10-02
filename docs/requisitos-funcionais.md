# Requisitos funcionais

O fluxo principal escolhido para o desenvolvimento será Pedido → Pagamento mock → Status, dado que desta forma cobriremos boa parte dos requisitos obrigatórios do case proposto.
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
- O atendente não cadastra clientes no balcão. O pedido pode ser anônimo ou vinculado a um cliente existente mediante autenticação do próprio cliente.

**Critérios de aceitação:**

1. Com dados válidos e identificadores disponíveis, o cadastro público cria um Cliente, sem expor sua senha na resposta.
2. Um Administrador autenticado consegue cadastrar Atendente ou Cozinha com unidade existente; solicitantes sem permissão têm a operação recusada.
3. Dados obrigatórios ausentes ou inválidos, CPF/e-mail duplicados ou perfil não autorizado impedem o cadastro, sem registro parcial ou alteração de conta existente.
4. Cadastro operacional sem unidade ou com unidade inexistente é recusado. Alteração autorizada para unidade válida mantém somente um vínculo vigente; alteração inválida preserva o vínculo anterior.
5. Associação de múltiplos perfis é recusada no MVP.

**Cobertura:** cadastro, validações, perfil único e vínculo por unidade serão implementados. Owner e múltiplos perfis são conceituais: somente Owner poderá atribuir Administrador; Owner e Administrador poderão associar perfis operacionais a um cadastro existente, sem duplicar CPF/e-mail e preservando as restrições por unidade. A aceitação dessa extensão requer permitir associações autorizadas e recusar atribuição de Administrador por outro perfil; sua execução não integra os testes do MVP.

**Fora do MVP:** recuperação de senha, confirmação por e-mail e notificações.

**Fontes:** Roteiro de Atividade Prática — Projeto Back-End, páginas 5–6 e 9 do PDF; Estudo de Caso Raízes do Nordeste, página 5 do PDF. Dados obrigatórios, unicidade, provisionamento inicial e limites dos perfis são decisões de escopo.
