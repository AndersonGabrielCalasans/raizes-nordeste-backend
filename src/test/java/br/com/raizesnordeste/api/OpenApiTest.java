package br.com.raizesnordeste.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(OpenApiTest.Configuracao.class)
class OpenApiTest {
    @TestHTTPResource
    URI base;

    public static class Configuracao implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            String chavePublica;
            try {
                var par = KeyPairGenerator.getInstance("RSA");
                par.initialize(2048);
                var chave = par.generateKeyPair().getPublic().getEncoded();
                var arquivo = Files.createTempFile("raizes-openapi-test-", ".pem");
                arquivo.toFile().deleteOnExit();
                Files.writeString(arquivo, "-----BEGIN PUBLIC KEY-----\n"
                        + Base64.getMimeEncoder(64, new byte[] { '\n' }).encodeToString(chave)
                        + "\n-----END PUBLIC KEY-----\n");
                chavePublica = arquivo.toUri().toString();
            } catch (Exception e) {
                throw new IllegalStateException("Falha ao criar chave de teste", e);
            }
            return Map.of("quarkus.flyway.migrate-at-start", "false",
                    "quarkus.datasource.jdbc.url", "jdbc:postgresql://localhost:1/nao-utilizado",
                    "quarkus.datasource.username", "teste",
                    "quarkus.datasource.password", "teste",
                    "quarkus.hibernate-orm.active", "false",
                    "mp.jwt.verify.publickey.location", chavePublica,
                    "smallrye.jwt.sign.key.location", "nao-utilizado.pem");
        }
    }

    @Test
    void documentaApenasRotasImplementadasEExigeTokenNasAdministrativas() throws Exception {
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder(base.resolve("/q/openapi?format=json")).GET().build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        JsonNode documento = new ObjectMapper().readTree(response.body());
        JsonNode paths = documento.path("paths");
        assertEquals(11, paths.size());
        assertTrue(paths.path("/pedidos").path("post").has("security"));
        assertTrue(paths.path("/pedidos").path("get").has("security"));
        assertTrue(paths.path("/pedidos/{id}").path("get").has("security"));
        assertTrue(paths.path("/unidades/{id}").has("get"));
        assertTrue(paths.path("/unidades").has("get"));
        assertTrue(paths.path("/unidades/{id}/cardapio").has("get"));
        assertTrue(paths.path("/estoque").path("get").has("security"));
        assertTrue(paths.path("/estoque/movimentacoes").path("post").has("security"));
        assertTrue(paths.path("/auth/cadastro").has("post"));
        assertTrue(paths.path("/auth/login").has("post"));
        assertTrue(paths.path("/usuarios").has("post"));
        assertTrue(paths.path("/usuarios/{id}/unidade").has("patch"));
        assertFalse(paths.path("/auth/login").path("post").has("security"));
        assertTrue(paths.path("/usuarios").path("post").has("security"));
        assertTrue(paths.path("/usuarios/{id}/unidade").path("patch").has("security"));
        assertTrue(paths.path("/auth/cadastro").path("post").path("responses").has("201"));
        assertTrue(paths.path("/auth/login").path("post").path("responses").has("401"));
        assertTrue(paths.path("/usuarios").path("post").path("responses").has("403"));
        for (var rota : new String[][] { { "/auth/cadastro", "post", "201" }, { "/auth/login", "post", "200" },
                { "/usuarios", "post", "201" }, { "/usuarios/{id}/unidade", "patch", "200" } }) {
            JsonNode json = paths.path(rota[0]).path(rota[1]).path("responses").path(rota[2])
                    .path("content").path("application/json");
            assertTrue(json.path("examples").size() > 0 || json.has("example"), rota[0]);
        }
        for (var rota : new String[][] { { "/auth/cadastro", "post" }, { "/auth/login", "post" },
                { "/usuarios", "post" }, { "/usuarios/{id}/unidade", "patch" } }) {
            JsonNode operacao = paths.path(rota[0]).path(rota[1]);
            assertFalse(operacao.path("summary").asText().isBlank(), rota[0]);
            JsonNode corpo = operacao.path("requestBody");
            assertTrue(corpo.path("required").asBoolean(), rota[0]);
            JsonNode json = corpo.path("content").path("application/json");
            assertTrue(json.path("examples").size() > 0 || json.has("example"), rota[0]);
            assertTrue(json.has("schema"), rota[0]);
        }
        assertTrue(documento.path("components").path("securitySchemes").size() > 0);
        for (var rota : new String[][] { { "/unidades", "get", "200" }, { "/unidades/{id}", "get", "200" }, { "/unidades/{id}/cardapio", "get", "200" },
                { "/estoque", "get", "200" }, { "/estoque/movimentacoes", "post", "201" },
                { "/pedidos", "get", "200" }, { "/pedidos", "post", "201" }, { "/pedidos/{id}", "get", "200" } }) {
            var operacao = paths.path(rota[0]).path(rota[1]);
            assertFalse(operacao.path("summary").asText().isBlank(), rota[0]);
            var json = operacao.path("responses").path(rota[2]).path("content").path("application/json");
            assertTrue(json.path("examples").size() > 0 || json.has("example"), rota[0]);
        }
        JsonNode schemas = documento.path("components").path("schemas");
        assertTrue(schemas.path("Cadastro").path("required").toString().contains("cpf"));
        assertTrue(schemas.path("CadastroOperacional").path("required").toString().contains("unidadeId"));
        assertTrue(schemas.path("Login").path("required").toString().contains("senha"));
        assertTrue(schemas.path("Unidade").path("required").toString().contains("unidadeId"));
        var swagger = client.send(HttpRequest.newBuilder(base.resolve("/q/swagger-ui/")).GET().build(), HttpResponse.BodyHandlers.discarding());
        assertEquals(200, swagger.statusCode());
    }
}
