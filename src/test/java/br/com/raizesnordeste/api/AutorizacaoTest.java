package br.com.raizesnordeste.api;

import br.com.raizesnordeste.application.UsuariosService;
import br.com.raizesnordeste.application.CatalogoService;
import br.com.raizesnordeste.domain.Auditoria;
import br.com.raizesnordeste.domain.RepositorioCatalogo;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.security.KeyPairGenerator;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(AutorizacaoTest.Configuracao.class)
class AutorizacaoTest {
    private static final UUID USUARIO_ID = UUID.fromString("40000000-0000-0000-0000-000000000002");
    private static final UUID UNIDADE_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final String EQUIPE = """
            {"nome":"Pessoa Exemplo","cpf":"52998224725","dataNascimento":"2000-01-01",
             "email":"equipe@example.com","senha":"Senha123","perfil":"ATENDENTE",
             "unidadeId":"10000000-0000-0000-0000-000000000001"}
            """;
    private static final String UNIDADE = "{\"unidadeId\":\"10000000-0000-0000-0000-000000000001\"}";
    private static final HttpClient CLIENTE = HttpClient.newHttpClient();

    @TestHTTPResource URI base;

    public static class Configuracao implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            try {
                var publicaExistente = System.getProperty("raizes.teste.chave.publica");
                Path chavePublica;
                if (publicaExistente == null) {
                    var gerador = KeyPairGenerator.getInstance("RSA");
                    gerador.initialize(2048);
                    var chaves = gerador.generateKeyPair();
                    var chavePrivada = Files.createTempFile("raizes-autorizacao-privada-", ".der");
                    chavePrivada.toFile().deleteOnExit();
                    Files.write(chavePrivada, chaves.getPrivate().getEncoded());
                    System.setProperty("raizes.teste.chave.privada", chavePrivada.toString());
                    chavePublica = Files.createTempFile("raizes-autorizacao-", ".pem");
                    chavePublica.toFile().deleteOnExit();
                    Files.writeString(chavePublica, "-----BEGIN PUBLIC KEY-----\n"
                            + Base64.getMimeEncoder(64, new byte[] { '\n' }).encodeToString(chaves.getPublic().getEncoded())
                            + "\n-----END PUBLIC KEY-----\n");
                    System.setProperty("raizes.teste.chave.publica", chavePublica.toString());
                } else {
                    chavePublica = Path.of(publicaExistente);
                }
                return Map.of("quarkus.flyway.migrate-at-start", "false",
                        "quarkus.datasource.jdbc.url", "jdbc:postgresql://localhost:1/nao-utilizado",
                        "quarkus.datasource.username", "teste", "quarkus.datasource.password", "teste",
                        "quarkus.hibernate-orm.active", "false",
                        "mp.jwt.verify.publickey.location", chavePublica.toUri().toString(),
                        "smallrye.jwt.sign.key.location", "nao-utilizado.pem");
            } catch (Exception e) {
                throw new IllegalStateException("Falha ao preparar chaves de teste", e);
            }
        }

        @Override
        public Set<Class<?>> getEnabledAlternatives() { return Set.of(UsuariosFalsos.class, AuditoriaFalsa.class, CatalogoFalso.class); }
    }

    @Alternative
    @ApplicationScoped
    public static class CatalogoFalso extends CatalogoService {
        @Override public RepositorioCatalogo.Pagina<RepositorioCatalogo.Saldo> estoque(UUID id, boolean admin, UUID unidade, int p, int l) {
            return new RepositorioCatalogo.Pagina<>(java.util.List.of(), p, l, 0);
        }
        @Override public RepositorioCatalogo.Movimento movimentar(UUID id, Movimentacao dados) {
            return new RepositorioCatalogo.Movimento(USUARIO_ID, dados.quantidade());
        }
    }

    @Alternative
    @ApplicationScoped
    public static class AuditoriaFalsa implements Auditoria {
        static final java.util.Queue<Evento> EVENTOS = new java.util.concurrent.ConcurrentLinkedQueue<>();
        public void registrar(Evento evento) { EVENTOS.add(evento); }
    }

    @Alternative
    @ApplicationScoped
    public static class UsuariosFalsos extends UsuariosService {
        static final AtomicInteger ALTERACOES = new AtomicInteger();

        @Override
        public Usuario cadastrar(Cadastro dados, String perfil, UUID unidadeId) {
            ALTERACOES.incrementAndGet();
            return new Usuario(USUARIO_ID, dados.nome(), perfil, unidadeId);
        }

        @Override
        public Usuario alterarUnidade(UUID id, Unidade dados) {
            ALTERACOES.incrementAndGet();
            return new Usuario(id, "Pessoa Exemplo", "ATENDENTE", dados.unidadeId());
        }

        @Override
        public Autenticacao autenticar(Login dados) {
            if (dados.email().equals("invalido@example.com")) throw erro(401, "CREDENCIAIS_INVALIDAS", "Inválido");
            return new Autenticacao(new Token("token-de-teste", Instant.now().plusSeconds(3600)), USUARIO_ID);
        }
    }

    @BeforeEach
    void limparContador() { UsuariosFalsos.ALTERACOES.set(0); }

    private PrivateKey chaveDeTeste() throws Exception {
        var bytes = Files.readAllBytes(Path.of(System.getProperty("raizes.teste.chave.privada")));
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bytes));
    }

    private String token(String perfil, Instant expira, PrivateKey chave) {
        Instant emitido = expira.isBefore(Instant.now()) ? Instant.now().minusSeconds(7200) : Instant.now();
        return Jwt.issuer("raizes-nordeste").subject(USUARIO_ID.toString()).groups(Set.of(perfil))
                .issuedAt(emitido).expiresAt(expira).sign(chave);
    }

    private HttpResponse<String> enviar(String metodo, String rota, String corpo, String token) throws Exception {
        var builder = HttpRequest.newBuilder(base.resolve(rota)).header("Content-Type", "application/json")
                .method(metodo, HttpRequest.BodyPublishers.ofString(corpo));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        return CLIENTE.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void cadastroELoginSaoPublicos() throws Exception {
        var cadastro = enviar("POST", "/auth/cadastro", EQUIPE, null);
        assertEquals(201, cadastro.statusCode());
        assertEquals("CLIENTE", new ObjectMapper().readTree(cadastro.body()).get("perfil").asText());
        var login = enviar("POST", "/auth/login", "{\"email\":\"pessoa@example.com\",\"senha\":\"Senha123\"}", null);
        assertEquals(200, login.statusCode());
    }

    @Test
    void administradorNaoPodeCadastrarOutroAdministrador() throws Exception {
        String jwt = token("ADMINISTRADOR", Instant.now().plusSeconds(3600), chaveDeTeste());
        String cadastro = EQUIPE.replace("\"ATENDENTE\"", "\"ADMINISTRADOR\"");
        assertEquals(400, enviar("POST", "/usuarios", cadastro, jwt).statusCode());
        assertEquals(0, UsuariosFalsos.ALTERACOES.get());
    }

    @Test
    void equipeRecusaTokensAusenteInvalidoAdulteradoEExpirado() throws Exception {
        var outraChave = KeyPairGenerator.getInstance("RSA");
        outraChave.initialize(2048);
        var expirado = token("ADMINISTRADOR", Instant.now().minusSeconds(3600), chaveDeTeste());
        var adulterado = token("ADMINISTRADOR", Instant.now().plusSeconds(3600), outraChave.generateKeyPair().getPrivate());
        for (String jwt : new String[] { null, "invalido", expirado, adulterado }) {
            assertEquals(401, enviar("POST", "/usuarios", EQUIPE, jwt).statusCode());
            assertEquals(401, enviar("PATCH", "/usuarios/" + USUARIO_ID + "/unidade", UNIDADE, jwt).statusCode());
        }
        assertEquals(0, UsuariosFalsos.ALTERACOES.get());
    }

    @Test
    void equipeRecusaTodosOsPerfisExcetoAdministradorSemAlterarDados() throws Exception {
        for (String perfil : new String[] { "CLIENTE", "ATENDENTE", "COZINHA" }) {
            String jwt = token(perfil, Instant.now().plusSeconds(3600), chaveDeTeste());
            assertEquals(403, enviar("POST", "/usuarios", EQUIPE, jwt).statusCode());
            assertEquals(403, enviar("PATCH", "/usuarios/" + USUARIO_ID + "/unidade", UNIDADE, jwt).statusCode());
        }
        assertEquals(0, UsuariosFalsos.ALTERACOES.get());
    }

    @Test
    void administradorAcessaEquipeSemExporDadosDesnecessarios() throws Exception {
        String jwt = token("ADMINISTRADOR", Instant.now().plusSeconds(3600), chaveDeTeste());
        var cadastro = enviar("POST", "/usuarios", EQUIPE, jwt);
        assertEquals(201, cadastro.statusCode());
        var resposta = new ObjectMapper().readTree(cadastro.body());
        assertEquals(Set.of("id", "nome", "perfil", "unidadeId"),
                new java.util.HashSet<>(java.util.stream.StreamSupport.stream(
                        java.util.Spliterators.spliteratorUnknownSize(resposta.fieldNames(), 0), false).toList()));
        var alteracao = enviar("PATCH", "/usuarios/" + USUARIO_ID + "/unidade", UNIDADE, jwt);
        assertEquals(200, alteracao.statusCode());
        assertEquals(2, UsuariosFalsos.ALTERACOES.get());
    }

    @Test
    void registraLoginERecusasSemCredenciais() throws Exception {
        AuditoriaFalsa.EVENTOS.clear();
        assertEquals(200, enviar("POST", "/auth/login", "{\"email\":\"x@example.com\",\"senha\":\"Senha123\"}", null).statusCode());
        assertTrue(AuditoriaFalsa.EVENTOS.stream().anyMatch(e -> e.operacao().equals("LOGIN")
                && USUARIO_ID.equals(e.responsavelId()) && e.resultado().equals("SUCESSO")));
        assertEquals(401, enviar("POST", "/auth/login", "{\"email\":\"invalido@example.com\",\"senha\":\"Senha123\"}", null).statusCode());
        assertTrue(AuditoriaFalsa.EVENTOS.stream().anyMatch(e -> e.operacao().equals("LOGIN")
                && e.responsavelId() == null && e.resultado().equals("RECUSADO")));
        assertEquals(401, enviar("POST", "/usuarios", EQUIPE, null).statusCode());
        String cliente = token("CLIENTE", Instant.now().plusSeconds(3600), chaveDeTeste());
        assertEquals(403, enviar("POST", "/usuarios", EQUIPE, cliente).statusCode());
        long limite = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < limite && AuditoriaFalsa.EVENTOS.stream()
                .filter(e -> e.operacao().equals("ACESSO_NEGADO")).count() < 2) Thread.sleep(50);
        assertTrue(AuditoriaFalsa.EVENTOS.stream().anyMatch(e -> e.operacao().equals("ACESSO_NEGADO") && e.responsavelId() == null));
        assertTrue(AuditoriaFalsa.EVENTOS.stream().anyMatch(e -> e.operacao().equals("ACESSO_NEGADO") && USUARIO_ID.equals(e.responsavelId())));
    }

    @Test
    void estoqueRespeitaPerfisEExigeQuantidadeInteira() throws Exception {
        String cliente = token("CLIENTE", Instant.now().plusSeconds(3600), chaveDeTeste());
        String atendente = token("ATENDENTE", Instant.now().plusSeconds(3600), chaveDeTeste());
        String admin = token("ADMINISTRADOR", Instant.now().plusSeconds(3600), chaveDeTeste());
        String movimento = "{\"produtoUnidadeId\":\"30000000-0000-0000-0000-000000000001\",\"tipo\":\"ENTRADA\",\"quantidade\":1}";
        assertEquals(401, enviar("GET", "/estoque", "", null).statusCode());
        assertEquals(403, enviar("GET", "/estoque", "", cliente).statusCode());
        assertEquals(200, enviar("GET", "/estoque", "", atendente).statusCode());
        assertEquals(403, enviar("POST", "/estoque/movimentacoes", movimento, atendente).statusCode());
        assertEquals(201, enviar("POST", "/estoque/movimentacoes", movimento, admin).statusCode());
        assertEquals(400, enviar("POST", "/estoque/movimentacoes", movimento.replace(":1}", ":1.5}"), admin).statusCode());
    }

    @Test
    void errosDeSegurancaUsamJsonPadronizado() throws Exception {
        var semToken = enviar("GET", "/pedidos", "", null);
        assertEquals(401, semToken.statusCode());
        assertEquals("NAO_AUTENTICADO", new ObjectMapper().readTree(semToken.body()).path("codigo").asText());
        var tokenInvalido = enviar("GET", "/pedidos", "", "invalido");
        assertEquals(401, tokenInvalido.statusCode());
        assertEquals("NAO_AUTENTICADO", new ObjectMapper().readTree(tokenInvalido.body()).path("codigo").asText());
        String cliente = token("CLIENTE", Instant.now().plusSeconds(3600), chaveDeTeste());
        var proibido = enviar("POST", "/usuarios", EQUIPE, cliente);
        assertEquals(403, proibido.statusCode());
        assertEquals("ACESSO_NEGADO", new ObjectMapper().readTree(proibido.body()).path("codigo").asText());
    }
}

