package br.com.raizesnordeste.api;

import br.com.raizesnordeste.application.CatalogoService;
import br.com.raizesnordeste.application.FalhaAplicacao;
import br.com.raizesnordeste.domain.Auditoria;
import br.com.raizesnordeste.domain.RepositorioCatalogo;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.UUID;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/estoque")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Estoque")
public class EstoqueResource {
    @Inject CatalogoService catalogo;
    @Inject JsonWebToken jwt;
    @Inject Auditoria auditoria;

    @GET @RolesAllowed({"ADMINISTRADOR", "ATENDENTE", "COZINHA"})
    @Operation(summary = "Consultar estoque", description = "Administrador pode filtrar por unidade; Atendente e Cozinha consultam somente sua unidade, sem informar o filtro.")
    @APIResponse(responseCode = "200", description = "Saldos", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "Saldos", value = "{\"items\":[{\"produtoUnidadeId\":\"30000000-0000-0000-0000-000000000001\",\"unidadeId\":\"10000000-0000-0000-0000-000000000001\",\"produtoId\":\"20000000-0000-0000-0000-000000000001\",\"nome\":\"Prato Exemplo\",\"saldo\":10}],\"page\":1,\"limit\":20,\"total\":1}")))
    @APIResponse(responseCode = "400", description = "Paginação inválida") @APIResponse(responseCode = "401", description = "Token inválido ou ausente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErro.class), examples = @ExampleObject(name = "Exemplo", value = "{\"codigo\":\"NAO_AUTENTICADO\",\"mensagem\":\"Token ausente ou inválido.\",\"detalhes\":[]}"))) @APIResponse(responseCode = "403", description = "Sem permissão") @APIResponse(responseCode = "404", description = "Unidade inexistente")
    public RepositorioCatalogo.Pagina<RepositorioCatalogo.Saldo> consultar(@QueryParam("unidadeId") UUID unidadeId, @DefaultValue("1") @QueryParam("page") int pagina, @DefaultValue("20") @QueryParam("limit") int limite) {
        UUID responsavel = UUID.fromString(jwt.getSubject());
        try {
            return catalogo.estoque(responsavel, jwt.getGroups().contains("ADMINISTRADOR"), unidadeId, pagina, limite);
        } catch (FalhaAplicacao falha) {
            if (falha.status() == 403)
                auditoria.registrar(new Auditoria.Evento(responsavel, "ACESSO_NEGADO", "ESTOQUE", unidadeId, "RECUSADO"));
            throw falha;
        }
    }

    @POST @Path("/movimentacoes") @RolesAllowed("ADMINISTRADOR")
    @Operation(summary = "Movimentar estoque", description = "Administrador registra entrada ou saída manual com quantidade inteira positiva.")
    @RequestBody(required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = CatalogoService.Movimentacao.class), examples = @ExampleObject(name = "Entrada", value = "{\"produtoUnidadeId\":\"30000000-0000-0000-0000-000000000001\",\"tipo\":\"ENTRADA\",\"quantidade\":10}")))
    @APIResponse(responseCode = "201", description = "Movimento registrado", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "Movimento", value = "{\"id\":\"50000000-0000-0000-0000-000000000001\",\"saldoAtual\":25}")))
    @APIResponse(responseCode = "400", description = "Dados inválidos") @APIResponse(responseCode = "401", description = "Token inválido ou ausente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErro.class), examples = @ExampleObject(name = "Exemplo", value = "{\"codigo\":\"NAO_AUTENTICADO\",\"mensagem\":\"Token ausente ou inválido.\",\"detalhes\":[]}"))) @APIResponse(responseCode = "403", description = "Sem permissão") @APIResponse(responseCode = "404", description = "Produto da unidade inexistente") @APIResponse(responseCode = "409", description = "Saldo insuficiente ou acima do limite")
    public Response movimentar(CatalogoService.Movimentacao dados) {
        return Response.status(201).entity(catalogo.movimentar(UUID.fromString(jwt.getSubject()), dados)).build();
    }
}
