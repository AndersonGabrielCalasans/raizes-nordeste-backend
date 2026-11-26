package br.com.raizesnordeste.api;

import br.com.raizesnordeste.application.CatalogoService;
import br.com.raizesnordeste.domain.RepositorioCatalogo;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/unidades")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Unidades e cardápio")
public class UnidadesResource {
    @Inject CatalogoService catalogo;

    @GET @PermitAll
    @Operation(summary = "Listar unidades", description = "Consulta pública paginada das unidades cadastradas. Página começa em 1; limite entre 1 e 100.")
    @APIResponse(responseCode = "200", description = "Unidades", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "Unidades", value = "{\"items\":[{\"id\":\"10000000-0000-0000-0000-000000000001\",\"nome\":\"Centro\",\"endereco\":\"Rua Exemplo, 1\"}],\"page\":1,\"limit\":20,\"total\":1}")))
    @APIResponse(responseCode = "400", description = "Paginação inválida", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErro.class), examples = @ExampleObject(name = "Exemplo", value = "{\"codigo\":\"PAGINACAO_INVALIDA\",\"mensagem\":\"Página ou limite inválido (limite máximo: 100).\",\"detalhes\":[]}")))
    public RepositorioCatalogo.Pagina<RepositorioCatalogo.Unidade> listar(@DefaultValue("1") @QueryParam("page") int pagina, @DefaultValue("20") @QueryParam("limit") int limite) {
        return catalogo.unidades(pagina, limite);
    }

    @GET @Path("/{id}") @PermitAll
    @Operation(summary = "Consultar unidade", description = "Consulta pública por identificador da unidade.")
    @APIResponse(responseCode = "200", description = "Unidade", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "Unidade", value = "{\"id\":\"10000000-0000-0000-0000-000000000001\",\"nome\":\"Centro\",\"endereco\":\"Rua Exemplo, 1\"}")))
    @APIResponse(responseCode = "404", description = "Unidade não encontrada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErro.class), examples = @ExampleObject(name = "Exemplo", value = "{\"codigo\":\"UNIDADE_NAO_ENCONTRADA\",\"mensagem\":\"Unidade não encontrada.\",\"detalhes\":[]}")))
    public RepositorioCatalogo.Unidade unidade(@PathParam("id") UUID id) { return catalogo.unidade(id); }

    @GET @Path("/{id}/cardapio") @PermitAll
    @Operation(summary = "Consultar cardápio da unidade", description = "Consulta pública de produtos disponíveis e com saldo positivo, com preço da unidade.")
    @APIResponse(responseCode = "200", description = "Cardápio", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "Cardapio", value = "{\"items\":[{\"id\":\"20000000-0000-0000-0000-000000000001\",\"nome\":\"Prato Exemplo\",\"descricao\":\"Descrição\",\"preco\":25.00,\"disponivel\":true}],\"page\":1,\"limit\":20,\"total\":1}")))
    @APIResponse(responseCode = "400", description = "Paginação inválida", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErro.class), examples = @ExampleObject(name = "Exemplo", value = "{\"codigo\":\"PAGINACAO_INVALIDA\",\"mensagem\":\"Página ou limite inválido (limite máximo: 100).\",\"detalhes\":[]}")))
    @APIResponse(responseCode = "404", description = "Unidade não encontrada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErro.class), examples = @ExampleObject(name = "Exemplo", value = "{\"codigo\":\"UNIDADE_NAO_ENCONTRADA\",\"mensagem\":\"Unidade não encontrada.\",\"detalhes\":[]}")))
    public RepositorioCatalogo.Pagina<RepositorioCatalogo.Produto> cardapio(@PathParam("id") UUID id, @DefaultValue("1") @QueryParam("page") int pagina, @DefaultValue("20") @QueryParam("limit") int limite) {
        return catalogo.cardapio(id, pagina, limite);
    }
}
