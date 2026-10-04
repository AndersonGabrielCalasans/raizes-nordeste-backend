# Requisitos funcionais

[Voltar ao índice da documentação](../README.md)

## Índice

- [Fluxo principal e justificativa](#fluxo-principal-e-justificativa)
- [RF01 — Cadastrar usuários e definir perfil e unidade](#rf01--cadastrar-usuários-e-definir-perfil-e-unidade)
- [RF02 — Autenticar usuários e controlar acesso](#rf02--autenticar-usuários-e-controlar-acesso)
- [RF03 — Cadastrar e consultar unidades da rede](#rf03--cadastrar-e-consultar-unidades-da-rede)
- [RF04 — Gerir produtos e consultar cardápio por unidade](#rf04--gerir-produtos-e-consultar-cardápio-por-unidade)
- [RF05 — Criar e consultar pedidos por canal](#rf05--criar-e-consultar-pedidos-por-canal)
- [RF06 — Atualizar status e cancelar pedidos](#rf06--atualizar-status-e-cancelar-pedidos)
- [RF07 — Controlar estoque por unidade e impedir vendas indisponíveis](#rf07--controlar-estoque-por-unidade-e-impedir-vendas-indisponíveis)
- [RF08 — Solicitar pagamento simulado e registrar retorno](#rf08--solicitar-pagamento-simulado-e-registrar-retorno)
- [RF09 — Acumular e resgatar pontos com consentimento](#rf09--acumular-e-resgatar-pontos-com-consentimento)
- [RF10 — Aplicar promoções e campanhas](#rf10--aplicar-promoções-e-campanhas)

## Fluxo principal e justificativa

O fluxo principal é Pedido → Pagamento mock → Status. Sua escolha decorre de representar a operação central da aplicação e permitir a cobertura de boa parte dos requisitos obrigatórios do caso: criação de pedidos, integração de pagamento simulado e acompanhamento de sua evolução até a entrega. Esse fluxo conecta os comportamentos em uma operação completa, em vez de limitar a implementação a cadastros isolados.

A cobertura de cada requisito distingue implementação prevista no MVP e descrição conceitual.

## RF01 — Cadastrar usuários e definir perfil e unidade

**Descrição:** Permitir o cadastro público de clientes e o cadastro de funcionários por um Administrador, com atribuição de perfil e unidade de atuação quando aplicável.

**Atores:** qualquer solicitante no cadastro público de Cliente; Administrador no cadastro de Atendente e Cozinha e na alteração da unidade de atuação; Owner na atribuição conceitual de Administrador.

**Dados:** nome completo, CPF, data de nascimento, e-mail e senha, todos obrigatórios. Atendente e Cozinha também devem possuir uma unidade de atuação.

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

**Cobertura:** cadastro, validações, perfil único e vínculo por unidade serão implementados. Owner e múltiplos perfis são conceituais: somente Owner poderá atribuir Administrador; Owner e Administrador poderão associar perfis operacionais a um cadastro existente, sem duplicar CPF/e-mail e preservando as restrições por unidade. A aceitação dessa extensão requer permitir associações autorizadas e recusar atribuição de Administrador por outro perfil; sua execução não integra os testes do MVP. Recuperação de senha, confirmação por e-mail e notificações ficam fora do MVP.

[Voltar ao topo](#requisitos-funcionais)

## RF02 — Autenticar usuários e controlar acesso

**Descrição:** Permitir a autenticação por e-mail e senha e restringir operações protegidas conforme a identidade, o perfil e a unidade de atuação.

**Atores:** Cliente, Atendente, Cozinha e Administrador.

**Regras:**

- Credenciais válidas permitem a emissão de um token JWT. As operações protegidas exigem token válido, com assinatura e prazo de validade verificados.
- Credenciais inválidas produzem a mensagem “E-mail ou senha inválidos”, sem revelar se o e-mail está cadastrado.
- A consulta do cardápio é pública. Os canais APP e WEB exigem Cliente autenticado para criar pedidos; o Cliente consulta somente os próprios pedidos.
- Atendente e Cozinha acessam apenas operações permitidas ao seu perfil e dados da sua unidade de atuação. O Administrador acessa as operações administrativas das unidades da rede.
- A ausência de token ou apresentação de token inválido ou expirado impede o acesso às operações protegidas. Autenticação válida não autoriza operações incompatíveis com o perfil, acesso a pedidos de outros clientes ou operações fora da unidade permitida.
- Pedidos anônimos no balcão dispensam conta de Cliente, mas exigem Atendente autenticado. A vinculação ao Cliente autenticado é conceitual.
- Os canais de origem são APP, TOTEM, BALCAO e WEB. BALCAO representa o atendimento no balcão. Na cobertura conceitual, TOTEM utiliza credencial do perfil Atendente, respeitando sua unidade de atuação.

**Critérios de aceitação:**

1. Credenciais válidas retornam um JWT com prazo de validade, sem expor a senha. Credenciais inválidas não emitem token e retornam a mensagem definida.
2. A consulta pública do cardápio funciona sem token.
3. Uma operação protegida sem token, com assinatura inválida ou com token expirado é recusada, sem executar a operação.
4. Com token válido, as operações autorizadas são permitidas; operações incompatíveis com o perfil são recusadas.
5. Um Cliente não consegue consultar pedidos de outro Cliente. Atendente e Cozinha não conseguem acessar operações ou dados de outra unidade.
6. Um Atendente autenticado consegue registrar pedido de balcão sem cadastro de Cliente; a mesma operação sem autenticação é recusada.
7. Nos canais APP e WEB, a criação de pedido exige Cliente autenticado; sem autenticação ou com perfil incompatível, a operação é recusada.
8. Na cobertura conceitual de TOTEM, a criação de pedido exige credencial válida de Atendente e respeita o vínculo com a unidade; credencial inválida ou operação em outra unidade é recusada.

**Cobertura:** autenticação por JWT, autorização por perfil, restrições por unidade e propriedade de pedidos terão implementação no MVP, abrangendo APP, WEB e BALCAO no backend. TOTEM possui cobertura apenas conceitual; seu critério não integra os testes executáveis do MVP. A vinculação de pedido de balcão a Cliente autenticado permanece conceitual: somente uma autenticação bem-sucedida do próprio Cliente pode autorizar o vínculo; informar apenas seu CPF não deve permitir essa associação.

[Voltar ao topo](#requisitos-funcionais)

## RF03 — Cadastrar e consultar unidades da rede

**Descrição:** Permitir a consulta das unidades da rede e descrever o cadastro e a alteração de seus dados pelo Administrador.

**Atores:** Qualquer solicitante na consulta pública; Administrador na gestão conceitual.

**Dados:** Código identificador da unidade, nome e endereço.

**Regras:**

- As unidades iniciais são previamente cadastradas na preparação do ambiente e possuem identificadores únicos.
- A consulta da lista de unidades e dos dados de uma unidade é pública, sem exigir autenticação.
- A consulta apresenta identificador, nome e endereço; uma unidade inexistente não deve ser apresentada como válida.
- Na gestão conceitual, somente o Administrador pode cadastrar unidades ou alterar nome e endereço. Ambos os campos são obrigatórios e não podem estar vazios; a alteração preserva o identificador e os vínculos existentes.

**Critérios de aceitação:**

1. Sem autenticação, a consulta da lista retorna as unidades previamente cadastradas com código identificador, nome e endereço.
2. A consulta por identificador existente retorna os dados da unidade correspondente; por código identificador inexistente, informa que a unidade não foi encontrada.
3. Na cobertura conceitual, um Administrador autenticado cadastra uma unidade com nome e endereço válidos, o código identificador é auto gerado e único, ou altera seus dados preservando código identificador e vínculos.
4. Na cobertura conceitual, cadastro ou alteração sem permissão ou com nome/endereço ausente ou vazio é recusado, sem criar registro parcial ou modificar os dados anteriores.

**Cobertura:** Consulta pública e provisionamento das unidades iniciais terão implementação no MVP. Cadastro e alteração pelo Administrador possuem cobertura conceitual; seus critérios não integram os testes executáveis do MVP. Esse recorte fornece as unidades necessárias ao fluxo principal sem incluir sua gestão administrativa na implementação.

[Voltar ao topo](#requisitos-funcionais)

## RF04 — Gerir produtos e consultar cardápio por unidade

**Descrição:** Permitir a consulta pública dos produtos disponíveis no cardápio de uma unidade e descrever a gestão de produtos, preços e associações ao cardápio pelo Administrador.

**Atores:** qualquer solicitante na consulta pública; Administrador na gestão conceitual.

**Dados:** Identificador do produto, nome, descrição, preço e disponibilidade na unidade consultada.

**Regras:**

- A consulta deve indicar uma unidade existente e não exige autenticação.
- O cardápio apresenta somente produtos associados à unidade consultada e disponíveis nela, com os respectivos preços dessa unidade.
- Produtos não associados à unidade e produtos indisponíveis nela são omitidos. A presença ou disponibilidade de um produto em outra unidade não determina sua exibição na unidade consultada.
- No MVP, produtos, preços e associações iniciais ao cardápio são previamente cadastrados na preparação do ambiente.
- Na gestão conceitual, somente o Administrador cadastra ou altera produtos e configura preços e associações ao cardápio de unidades existentes. A operação exige dados válidos e mantém a identificação dos produtos e das unidades nas alterações.

**Critérios de aceitação:**

1. Sem autenticação, a consulta de uma unidade existente retorna seus produtos disponíveis, com identificador, nome, descrição, preço e disponibilidade.
2. A consulta de uma unidade inexistente informa que a unidade não foi encontrada.
3. Uma unidade sem produtos associados ou sem produtos disponíveis retorna um cardápio vazio.
4. Um produto associado e disponível em uma unidade é exibido no cardápio dela; em outra unidade, se não associado ou indisponível, é omitido.
5. Quando um mesmo produto está disponível em duas unidades, cada consulta apresenta o preço correspondente à unidade consultada.
6. Na cobertura conceitual, um Administrador autenticado cadastra ou altera um produto e configura seu preço e associação a uma unidade existente; a consulta dessa unidade reflete a configuração quando o produto está disponível.
7. Na cobertura conceitual, gestão sem permissão, com dados inválidos ou referência a produto ou unidade inexistente é recusada, sem alteração parcial. A exigência de produto existente aplica-se à alteração e à configuração, não ao cadastro inicial.

**Cobertura:** consulta pública do cardápio por unidade, filtro de disponibilidade e provisionamento dos produtos, preços e associações iniciais terão implementação no MVP. Cadastro e alteração de produtos e configuração do cardápio pelo Administrador permanecem conceituais, conforme o recorte de gestão aprovado.

[Voltar ao topo](#requisitos-funcionais)

## RF05 — Criar e consultar pedidos por canal

**Descrição:** Permitir a criação e consulta de pedidos, registrando unidade, itens, quantidades, valores, canal de origem e status, com filtro por canal e restrições de acesso.

**Atores:** Cliente e Atendente na criação e consulta; Cozinha e Administrador na consulta. TOTEM possui cobertura conceitual com credencial de Atendente.

**Dados:** Identificador do pedido, unidade, canal de origem, itens com identificador do produto, quantidade, preço unitário e subtotal, valor total e status. Em APP e WEB, o pedido também possui vínculo com o Cliente autenticado.

**Regras:**

- Cada pedido pertence a uma única unidade existente. Em APP e WEB, o Cliente escolhe a unidade; em BALCAO e, conceitualmente, TOTEM, o Atendente atua somente em sua unidade vinculada.
- O campo canalPedido é obrigatório e possui os valores APP, TOTEM, BALCAO e WEB, sujeitos à cobertura e às permissões de cada canal.
- APP e WEB exigem Cliente autenticado e vinculam o pedido a ele. BALCAO exige Atendente autenticado e registra pedido anônimo, sem cadastro de Cliente. TOTEM utiliza credencial de Atendente, com cobertura apenas conceitual.
- O pedido deve conter ao menos um item. Cada quantidade deve ser inteira e positiva; os produtos devem estar associados e disponíveis na unidade escolhida, conforme suas restrições de estoque.
- O sistema calcula o preço unitário a partir do cardápio da unidade, o subtotal pela multiplicação do preço pela quantidade e o total pela soma dos subtotais. Valores informados pelo solicitante não substituem esse cálculo.
- Um pedido criado com sucesso recebe o status AGUARDANDO_PAGAMENTO.
- A recusa por dados inválidos, falta de disponibilidade ou ausência de permissão não deve gerar pedido parcial.
- A consulta por identificador e a listagem com filtro por canal apresentam os dados registrados e o status atual. Canal não previsto é recusado; ausência de resultados permitidos retorna lista vazia.
- Cliente consulta somente seus pedidos. Atendente consulta os pedidos de sua unidade. Administrador pode consultar pedidos das unidades da rede. O filtro por canal não amplia as permissões.
- Cozinha consulta somente pedidos de sua unidade: pedidos BALCAO são visíveis mesmo sem pagamento aprovado e podem ter o preparo iniciado nessa condição; pedidos APP, WEB e, conceitualmente, TOTEM somente são visíveis e podem ter o preparo iniciado após aprovação do pagamento. As transições de status e suas permissões são detalhadas no RF06.

**Critérios de aceitação:**

1. Um Cliente autenticado em APP ou WEB, ao informar unidade existente e itens válidos e disponíveis, cria um pedido vinculado a si e à unidade escolhida, com o canal informado, os valores calculados e status AGUARDANDO_PAGAMENTO.
2. Um Atendente autenticado em BALCAO cria um pedido anônimo com itens válidos em sua unidade; a tentativa para outra unidade é recusada.
3. A criação sem autenticação, com perfil incompatível com o canal ou com canal ausente ou não previsto é recusada, sem registrar pedido.
4. Unidade inexistente, ausência de itens, quantidade zero, negativa ou não inteira, produto inexistente, não associado ou indisponível na unidade impedem a criação, sem registro parcial.
5. Os preços registrados correspondem ao cardápio da unidade; os subtotais correspondem ao preço unitário multiplicado pela quantidade e o total à soma dos subtotais, sem aceitar alteração desses valores pelo solicitante.
6. Na cobertura conceitual de TOTEM, credencial válida de Atendente e itens válidos permitem criar pedido somente na unidade de atuação, com valores calculados e status AGUARDANDO_PAGAMENTO; credencial inválida ou unidade não autorizada impedem a criação.
7. A consulta de pedido existente e permitido retorna seus dados e status atual; pedido inexistente ou fora do acesso permitido não tem seus dados expostos.
8. A listagem filtrada por canal retorna somente pedidos desse canal dentro do acesso permitido. Filtro inválido é recusado; sem resultados permitidos, retorna lista vazia.
9. Cliente não consulta pedidos de outro Cliente; Atendente e Cozinha não consultam pedidos de outra unidade, inclusive por identificador ou filtro. Administrador consegue consultar pedidos de diferentes unidades.
10. Cozinha consegue consultar um pedido BALCAO de sua unidade sem pagamento aprovado. Pedidos APP e WEB não são exibidos nem acessíveis diretamente antes da aprovação, e passam a ser consultáveis após aprovação. A mesma restrição aplica-se conceitualmente a TOTEM.

**Cobertura:** Criação e consulta de pedidos em APP, WEB e BALCAO, filtro por canal, validações, cálculo dos valores, vínculo por unidade, status inicial e restrições de consulta terão implementação no backend do MVP. TOTEM possui cobertura apenas conceitual; seus critérios não integram os testes executáveis do MVP. Interfaces dos canais não fazem parte deste requisito de backend.

[Voltar ao topo](#requisitos-funcionais)

## RF06 — Atualizar status e cancelar pedidos

**Descrição:** Permitir a evolução do pedido até sua conclusão ou cancelamento, respeitando status atual, canal, aprovação do pagamento, perfil e unidade de atuação.

**Atores:** Cozinha no preparo; Atendente na conclusão e no cancelamento; Cliente no cancelamento dos próprios pedidos; Administrador no cancelamento entre unidades; sistema na atualização decorrente de pagamento aprovado.

**Dados:** identificador do pedido, canal, unidade, status atual, status solicitado e situação do pagamento.

**Regras:**

- Os status são AGUARDANDO_PAGAMENTO, RECEBIDO, EM_PREPARO, PRONTO, CONCLUIDO e CANCELADO. O status inicial é AGUARDANDO_PAGAMENTO.
- A aprovação do pagamento altera AGUARDANDO_PAGAMENTO para RECEBIDO. A Cozinha da unidade altera RECEBIDO para EM_PREPARO e EM_PREPARO para PRONTO. O Atendente da unidade altera PRONTO para CONCLUIDO somente com pagamento aprovado.
- Em BALCAO, a Cozinha pode também alterar AGUARDANDO_PAGAMENTO diretamente para EM_PREPARO, antes do pagamento. Em APP, WEB e, conceitualmente, TOTEM, iniciar preparo exige pagamento aprovado.
- O pagamento aprovado de BALCAO já EM_PREPARO ou PRONTO é registrado sem retroceder o status para RECEBIDO. Status operacional e situação do pagamento são verificados separadamente.
- Somente pedidos AGUARDANDO_PAGAMENTO ou RECEBIDO podem ser cancelados. O Cliente cancela somente seus pedidos; Atendente cancela pedidos de sua unidade; Administrador cancela pedidos das unidades da rede.
- CONCLUIDO e CANCELADO são finais. Transições não previstas, inclusive regressões, são recusadas; aprovação de pagamento não reabre pedido cancelado.
- Operações sem autenticação, sem permissão, fora da unidade autorizada ou para pedido inexistente são recusadas, preservando os dados anteriores.

**Critérios de aceitação:**

1. Um pedido AGUARDANDO_PAGAMENTO passa para RECEBIDO após aprovação do pagamento; a Cozinha da unidade pode iniciar o preparo e marcar o pedido como PRONTO, nessa ordem.
2. A Cozinha da unidade consegue iniciar preparo de BALCAO ainda AGUARDANDO_PAGAMENTO. A mesma tentativa para APP ou WEB sem pagamento aprovado é recusada; a restrição aplica-se conceitualmente a TOTEM.
3. Quando o pagamento de BALCAO é aprovado com pedido EM_PREPARO ou PRONTO, a situação do pagamento é atualizada e o status operacional é preservado.
4. Um Atendente da unidade conclui um pedido PRONTO com pagamento aprovado. Pedido não pronto ou sem pagamento aprovado não pode ser concluído.
5. Cliente proprietário, Atendente da unidade ou Administrador consegue cancelar pedido AGUARDANDO_PAGAMENTO ou RECEBIDO, alterando seu status para CANCELADO. Cancelamento a partir de EM_PREPARO, PRONTO ou CONCLUIDO é recusado.
6. Tentativas de cancelar pedido de outro Cliente, atuar em outra unidade sem permissão, executar transição por perfil incompatível ou alterar pedido inexistente são recusadas, sem modificar dados.
7. Tentativas de regressão, salto não previsto ou alteração de status CONCLUIDO ou CANCELADO são recusadas. Um retorno de pagamento aprovado não reabre pedido CANCELADO.

**Cobertura:** transições, permissões, exceção de preparo antecipado em BALCAO, conclusão condicionada ao pagamento e cancelamento terão implementação no MVP. As regras correspondentes a TOTEM possuem cobertura apenas conceitual e não integram os testes executáveis do MVP. Efeitos de cancelamento sobre estoque e pagamento são tratados nos requisitos próprios.

[Voltar ao topo](#requisitos-funcionais)

## RF07 — Controlar estoque por unidade e impedir vendas indisponíveis

**Descrição:** Permitir a consulta e movimentação do estoque de produtos por unidade, realizando baixa na criação de pedidos e devolução em cancelamentos permitidos, sem vender quantidade superior ao saldo disponível.

**Atores:** Administrador na consulta e nas movimentações manuais entre unidades; Atendente e Cozinha na consulta da própria unidade; sistema nas movimentações decorrentes dos pedidos.

**Dados:** unidade, produto, saldo, tipo de movimentação (entrada ou saída), quantidade e referência ao pedido nas movimentações automáticas.

**Regras:**

- O saldo é controlado por produto e unidade, sem compartilhamento automático entre lojas. O controle do MVP utiliza produtos vendidos, sem gestão de ingredientes ou receitas.
- Somente Administrador registra entradas e saídas manuais. Atendente e Cozinha consultam exclusivamente o saldo da unidade de atuação.
- Movimentações exigem produto associado à unidade existente e quantidade inteira positiva. Saídas não podem tornar o saldo negativo; operações inválidas são recusadas sem alterar o saldo.
- Na criação de pedido, o sistema verifica o saldo para a quantidade total solicitada de cada produto e registra a baixa na unidade do pedido. Todos os itens devem ser atendidos; se algum saldo for insuficiente, nenhum pedido ou movimento parcial é registrado.
- A verificação e a baixa devem impedir que pedidos simultâneos consumam o mesmo saldo além do disponível. Pagamento e evolução do preparo não geram nova baixa para o mesmo pedido.
- O cancelamento permitido pelo RF06 devolve à unidade as quantidades baixadas pelo pedido, uma única vez. Tentativa de cancelamento recusada não altera o estoque.
- Produto sem saldo é indisponível para venda e omitido do cardápio, conforme RF04. Um saldo positivo não dispensa a associação ao cardápio e as demais regras de disponibilidade da unidade.

**Critérios de aceitação:**

1. Um Administrador autenticado registra entrada válida e aumenta o saldo do produto na unidade indicada; saída manual válida reduz esse saldo sem torná-lo negativo.
2. Atendente e Cozinha consultam o saldo da própria unidade, mas não registram movimentações manuais nem consultam estoque de outra unidade. Administrador consegue consultar e movimentar estoque entre unidades.
3. Movimentação sem permissão, com referência inválida, quantidade zero, negativa ou não inteira, ou saída superior ao saldo é recusada, preservando o estoque.
4. A criação válida de pedido reduz os saldos pelas quantidades solicitadas somente na unidade correspondente. Saldo insuficiente para qualquer item impede a criação e preserva todos os saldos.
5. Pedidos simultâneos cuja soma das quantidades exceda o saldo não são todos aceitos; o saldo permanece não negativo e corresponde somente aos pedidos criados.
6. Cancelamento autorizado antes do preparo devolve exatamente as quantidades baixadas. Repetição da tentativa, cancelamento não permitido ou evolução de pagamento/preparo não duplica devoluções ou baixas.
7. Com saldo zero, o produto não aparece no cardápio da unidade e sua inclusão em novo pedido é recusada; o saldo e o cardápio de outra unidade permanecem independentes.

**Cobertura:** saldo por produto e unidade, consulta com restrições, entradas e saídas manuais pelo Administrador, baixa na criação, devolução no cancelamento e bloqueio por saldo insuficiente terão implementação no MVP. A aplicação dessas regras a pedidos TOTEM permanece conceitual, conforme a cobertura desse canal.

[Voltar ao topo](#requisitos-funcionais)

## RF08 — Solicitar pagamento simulado e registrar retorno

**Descrição:** Solicitar o pagamento do valor calculado do pedido a um mock de API de pagamentos e registrar a tentativa e seu retorno, sem movimentação financeira real.

**Atores:** Cliente nos próprios pedidos APP e WEB; Atendente nos pedidos BALCAO de sua unidade e, conceitualmente, TOTEM; mock de API de pagamentos no retorno; sistema no registro e na atualização decorrente do retorno e do cancelamento autorizado.

**Dados:** identificador do pedido, identificador da tentativa, valor solicitado, retorno de aprovação ou recusa, situação do pagamento, falha de comunicação quando ocorrer e registro de estorno simulado quando aplicável.

**Regras:**

- Cliente solicita pagamento somente dos próprios pedidos APP e WEB. Atendente solicita pagamento de BALCAO somente em sua unidade; essa permissão aplica-se conceitualmente a TOTEM.
- A solicitação utiliza o valor calculado do pedido e integra exclusivamente um mock de API de pagamentos, sem cobrança real.
- Cada tentativa e seu retorno são registrados. Aprovação registra o pagamento e aplica RF06: AGUARDANDO_PAGAMENTO passa para RECEBIDO; BALCAO já EM_PREPARO ou PRONTO mantém seu status operacional.
- Recusa registra a tentativa, preserva o status operacional e permite nova tentativa, inclusive para BALCAO em preparo.
- Falha de comunicação é registrada sem considerar o pagamento aprovado e permite nova tentativa. A repetição não deve duplicar um pagamento aprovado.
- Retorno repetido da mesma tentativa não duplica pagamento nem atualização de status.
- Cancelamento autorizado de pedido pago registra estorno simulado, respeitando as restrições do RF06, sem movimentação financeira real. Retorno de pagamento não reabre pedido CANCELADO.
- Operações sem autenticação ou sem permissão são recusadas, sem solicitar pagamento ou alterar o pedido.

**Critérios de aceitação:**

1. Cliente proprietário em APP ou WEB, ou Atendente da unidade em BALCAO, solicita pagamento e a API mock recebe o valor calculado do pedido; tentativa e retorno são registrados.
2. Retorno aprovado registra o pagamento e altera AGUARDANDO_PAGAMENTO para RECEBIDO. Em BALCAO já EM_PREPARO ou PRONTO, registra a aprovação sem retroceder o status.
3. Retorno recusado fica registrado, não altera o status operacional e permite nova tentativa.
4. Falha de comunicação fica registrada, não aprova pagamento nem avança o pedido e permite nova tentativa sem duplicar aprovação já registrada.
5. Receber novamente o mesmo retorno não registra pagamento em duplicidade nem repete a transição de status.
6. Cancelamento permitido de pedido pago registra estorno simulado. Cancelamento recusado pelo RF06 não altera o pedido nem gera estorno; retorno de pagamento não reabre pedido cancelado.
7. Solicitante sem autenticação, Cliente tentando pagar pedido de outro Cliente ou Atendente tentando atuar em outra unidade tem a solicitação recusada, sem chamada ao mock ou alteração do pedido.
8. Na cobertura conceitual de TOTEM, credencial de Atendente permite solicitar pagamento somente de pedido da sua unidade, aplicando os mesmos registros e tratamento de retornos.

**Cobertura:** integração com mock de API, registro das tentativas e retornos, aprovação, recusa, falha de comunicação, proteção contra duplicação e estorno simulado terão implementação no MVP para APP, WEB e BALCAO. TOTEM permanece conceitual; seu critério não integra os testes executáveis do MVP. Não há pagamento ou estorno financeiro real.

[Voltar ao topo](#requisitos-funcionais)

## RF09 — Acumular e resgatar pontos com consentimento

**Descrição:** Descrever o acúmulo e resgate de pontos de fidelidade para Cliente autenticado que tenha consentido em participar do programa.

**Atores:** Cliente na adesão, revogação e resgate; sistema no registro de consentimento, créditos, resgates e saldo.

**Dados:** Cliente, consentimento e sua revogação, pedido de origem, créditos e resgates de pontos, saldo e desconto concedido.

**Regras:**

- A participação é opcional e exige consentimento registrado do Cliente. Não participar não impede comprar. A revogação interrompe novos acúmulos e resgates.
- Somente pedidos vinculados ao Cliente participante, com pagamento aprovado e status CONCLUIDO, geram pontos, uma única vez por pedido. Pedidos anônimos de BALCAO não geram pontos.
- Cada R$ 1 inteiro do valor final do pedido gera 1 ponto; frações são desconsideradas.
- O Cliente pode resgatar 100 pontos para obter R$ 5 de desconto em um novo pedido, limitado a um resgate por pedido e condicionado a saldo suficiente e consentimento vigente.
- Pedido com valor inferior a R$ 5 antes do desconto de fidelidade não permite resgate. O benefício mantém a proporção de 100 pontos por R$ 5, sem resgate parcial.
- Cancelamento autorizado pelo RF06 de pedido com resgate devolve os pontos debitados uma única vez. Cancelamento recusado não restitui pontos; a restituição não é novo acúmulo por compra.
- Créditos e resgates são registrados, mantendo o saldo sem valores negativos e impedindo pontuação ou resgate duplicado para o mesmo pedido.

**Critérios de aceitação conceituais:**

1. Cliente autenticado pode aderir ao programa ou revogar sua participação, com registro da escolha. Cliente sem adesão continua podendo comprar, mas não acumula ou resgata pontos.
2. Pedido vinculado a participante, pago e concluído, com valor final de R$ 42,90 gera 42 pontos. Repetição do processamento não gera novo crédito.
3. Pedido não concluído, sem pagamento aprovado, sem vínculo com Cliente ou sem consentimento vigente não gera pontos.
4. Cliente participante com 100 pontos ou mais consegue resgatar 100 pontos por R$ 5 de desconto em um novo pedido, reduzindo o saldo uma única vez.
5. Saldo inferior a 100 pontos, consentimento revogado ou tentativa de segundo resgate no mesmo pedido impede o resgate, sem desconto ou débito indevido.
6. Pedido inferior a R$ 5 antes do desconto tem o resgate recusado, sem débito de pontos; pedido de pelo menos R$ 5 permite o benefício quando as demais condições são atendidas.
7. Cancelamento permitido de pedido com resgate devolve os 100 pontos debitados. Repetição do processamento não duplica a restituição; cancelamento recusado preserva o saldo.

**Cobertura:** apenas conceitual. Programa, consentimento específico, acúmulo, resgate e saldo não terão implementação nem testes executáveis no MVP. As regras numéricas são escolhas de projeto.

[Voltar ao topo](#requisitos-funcionais)

## RF10 — Aplicar promoções e campanhas

**Descrição:** Descrever campanhas configuradas pelo Administrador, com vigência, unidades participantes, desconto e limites de utilização, verificando as condições antes de solicitar pagamento.

**Atores:** Administrador na configuração; Cliente autenticado na utilização de campanhas limitadas por CPF; sistema no cálculo, reserva, confirmação e liberação de utilizações.

**Dados:** identificador da campanha ou cupom, período de vigência, unidades participantes, percentual de desconto, subtotal mínimo, teto do desconto em reais, limite global de pedidos beneficiados, limite por CPF, utilizações reservadas e efetivas, pedido e desconto aplicado.

**Regras:**

- O Administrador configura vigência, unidades participantes, teto de desconto em reais e limites global e por CPF. A regra de aplicação concede 10% sobre o subtotal de pedidos a partir de R$ 50, respeitando o teto configurado.
- O desconto corresponde ao menor valor entre 10% do subtotal e o teto da campanha. Exemplo de configuração: no máximo 500 pedidos beneficiados durante a campanha e um pedido com desconto por CPF.
- Campanhas limitadas por CPF exigem Cliente autenticado; informar apenas CPF não autoriza o benefício. Campanhas gerais não exigem adesão à fidelidade. Segmentação por idade ou perfil exige consentimento específico para uso desses dados.
- A aplicação exige campanha vigente, unidade participante, subtotal mínimo e capacidade disponível nos limites global e por CPF. Cada reserva também ocupa capacidade, impedindo que pedidos simultâneos excedam os limites.
- Ao aplicar o cupom, o sistema reserva uma utilização global e por CPF. Pagamento aprovado converte a reserva em utilização efetiva, uma única vez.
- Cancelamento antes da aprovação libera a reserva. Recusa de pagamento não consome o benefício definitivamente: enquanto o pedido aguarda nova tentativa, a utilização permanece reservada, podendo ser liberada pelo cancelamento permitido.
- Cancelamento após aprovação do pagamento mantém a utilização contabilizada, mesmo com estorno simulado. A campanha não libera benefício já efetivamente usado.
- Somente uma campanha é aplicada por pedido, sem acumular com resgate de pontos. O pedido registra campanha, desconto e valor final antes da solicitação do pagamento.

**Critérios de aceitação conceituais:**

1. Cliente autenticado elegível, com pedido em unidade participante durante a vigência e subtotal de pelo menos R$ 50, obtém desconto de 10%, limitado ao teto configurado. Com subtotal de R$ 200 e teto de R$ 12, o desconto é R$ 12 e o valor final é R$ 188.
2. Campanha vencida, unidade não participante, subtotal insuficiente, limite global ou por CPF atingido, ou ausência de autenticação em campanha limitada por CPF impede a concessão do desconto.
3. Em campanha limitada a 500 pedidos e um por CPF, reservas e utilizações efetivas não excedem 500 no total nem uma por CPF, inclusive sob solicitações simultâneas.
4. Aplicar o cupom reserva capacidade; pagamento aprovado confirma seu uso. Repetir o retorno não contabiliza nova utilização.
5. Cancelamento permitido antes da aprovação libera a reserva global e por CPF. Recusa seguida de nova tentativa usa a reserva existente, sem duplicá-la; cancelamento após aprovação e estorno não liberam a utilização efetiva.
6. Tentativa de aplicar segunda campanha ou acumular desconto de campanha com resgate de pontos é recusada, sem conceder benefícios cumulativos.
7. Configuração por perfil sem permissão é recusada; campanha segmentada por idade ou perfil não utiliza os dados do Cliente sem consentimento específico.

**Cobertura:** apenas conceitual. Configuração de campanhas, cálculo de descontos, segmentação e controle de limites não terão implementação nem testes executáveis no MVP. Percentual, subtotal mínimo e exemplos de limites são escolhas de projeto; teto em reais e limites de utilização são configuráveis por campanha.

[Voltar ao topo](#requisitos-funcionais)
