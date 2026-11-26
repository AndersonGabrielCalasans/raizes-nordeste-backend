package br.com.raizesnordeste.api;

import br.com.raizesnordeste.application.UsuariosService;
import br.com.raizesnordeste.domain.Auditoria;
import org.eclipse.microprofile.jwt.JsonWebToken;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/usuarios")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMINISTRADOR")
@Tag(name = "Equipe", description = "Operações disponíveis ao Administrador")
public class UsuariosResource {
    @Inject
    UsuariosService usuarios;

    @Inject
    Auditoria auditoria;

    @Inject
    JsonWebToken jwt;

    @POST
    @Transactional
    @Operation(summary = "Cadastrar membro da equipe", description = "Exige token de Administrador. Todos os campos são obrigatórios; o perfil deve ser ATENDENTE ou COZINHA e a unidade deve existir.")
    @RequestBody(required = true, description = "Dados do funcionário e unidade", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuariosService.CadastroOperacional.class), examples = @ExampleObject(name = "Atendente fictício", value = "{\"nome\":\"Atendente Exemplo\",\"cpf\":\"11144477735\",\"dataNascimento\":\"1990-03-10\",\"email\":\"equipe@example.com\",\"senha\":\"Senha123\",\"perfil\":\"ATENDENTE\",\"unidadeId\":\"10000000-0000-0000-0000-000000000001\"}")))
    @APIResponse(responseCode = "201", description = "Usuário cadastrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuariosService.Usuario.class), examples = @ExampleObject(name = "Atendente cadastrado", value = "{\"id\":\"40000000-0000-0000-0000-000000000002\",\"nome\":\"Atendente Exemplo\",\"perfil\":\"ATENDENTE\",\"unidadeId\":\"10000000-0000-0000-0000-000000000001\"}")))
    @APIResponse(responseCode = "400", description = "Dados ou perfil inválidos", content = @Content(schema = @Schema(implementation = ApiErro.class)))
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Perfil sem permissão")
    @APIResponse(responseCode = "404", description = "Unidade não encontrada", content = @Content(schema = @Schema(implementation = ApiErro.class)))
    @APIResponse(responseCode = "409", description = "CPF ou e-mail já cadastrado", content = @Content(schema = @Schema(implementation = ApiErro.class)))
    public Response cadastrar(UsuariosService.CadastroOperacional dados) {
        if (dados == null || !("ATENDENTE".equals(dados.perfil()) || "COZINHA".equals(dados.perfil())))
            throw UsuariosService.erro(400, "DADOS_INVALIDOS", "Perfil operacional inválido.");
        var cadastro = new UsuariosService.Cadastro(dados.nome(), dados.cpf(), dados.dataNascimento(), dados.email(),
                dados.senha());
        var usuario = usuarios.cadastrar(cadastro, dados.perfil(), dados.unidadeId());
        auditoria.registrar(new Auditoria.Evento(UUID.fromString(jwt.getSubject()), "CADASTRAR_EQUIPE", "USUARIO", usuario.id(), "SUCESSO"));
        return Response.status(Response.Status.CREATED).entity(usuario).build();
    }

    @PATCH
    @Transactional
    @Path("/{id}/unidade")
    @Operation(summary = "Alterar unidade de membro da equipe", description = "Atualiza a unidade de Atendente ou Cozinha.")
    @RequestBody(required = true, description = "Identificador da nova unidade", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuariosService.Unidade.class), examples = @ExampleObject(name = "Unidade", value = "{\"unidadeId\":\"10000000-0000-0000-0000-000000000002\"}")))
    @APIResponse(responseCode = "200", description = "Unidade atualizada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuariosService.Usuario.class), examples = @ExampleObject(name = "Unidade atualizada", value = "{\"id\":\"40000000-0000-0000-0000-000000000002\",\"nome\":\"Atendente Exemplo\",\"perfil\":\"ATENDENTE\",\"unidadeId\":\"10000000-0000-0000-0000-000000000002\"}")))
    @APIResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ApiErro.class)))
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Perfil sem permissão")
    @APIResponse(responseCode = "404", description = "Unidade ou usuário não encontrado", content = @Content(schema = @Schema(implementation = ApiErro.class)))
    public UsuariosService.Usuario alterarUnidade(@Parameter(description = "UUID do Atendente ou Cozinha", required = true, example = "40000000-0000-0000-0000-000000000002") @PathParam("id") UUID id, UsuariosService.Unidade dados) {
        var usuario = usuarios.alterarUnidade(id, dados);
        auditoria.registrar(new Auditoria.Evento(UUID.fromString(jwt.getSubject()), "ALTERAR_UNIDADE", "USUARIO", id, "SUCESSO"));
        return usuario;
    }
}
