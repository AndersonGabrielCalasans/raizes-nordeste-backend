package br.com.raizesnordeste.api;

import br.com.raizesnordeste.application.UsuariosService;
import br.com.raizesnordeste.application.FalhaAplicacao;
import br.com.raizesnordeste.domain.Auditoria;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Autenticação", description = "Cadastro público e login")
public class AuthResource {
    @Inject
    UsuariosService usuarios;

    @Inject
    Auditoria auditoria;

    @POST
    @Path("/cadastro")
    @PermitAll
    @Transactional
    @Operation(summary = "Cadastrar cliente", description = "Cria uma conta com perfil Cliente. Exige CPF válido e único, e-mail único, data não futura e senha com pelo menos 8 caracteres, sem espaços. Os dados do exemplo são fictícios.")
    @RequestBody(required = true, description = "Dados obrigatórios do cliente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuariosService.Cadastro.class), examples = @ExampleObject(name = "Cliente fictício", value = "{\"nome\":\"Cliente Exemplo\",\"cpf\":\"52998224725\",\"dataNascimento\":\"1990-03-10\",\"email\":\"cliente@example.com\",\"senha\":\"Senha123\"}")))
    @APIResponse(responseCode = "201", description = "Cliente cadastrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuariosService.Usuario.class), examples = @ExampleObject(name = "Cliente cadastrado", value = "{\"id\":\"40000000-0000-0000-0000-000000000002\",\"nome\":\"Cliente Exemplo\",\"perfil\":\"CLIENTE\",\"unidadeId\":null}")))
    @APIResponse(responseCode = "400", description = "Dados inválidos", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErro.class), examples = @ExampleObject(name = "Dados inválidos", value = "{\"codigo\":\"DADOS_INVALIDOS\",\"mensagem\":\"CPF, e-mail ou senha inválidos.\",\"detalhes\":[]}")))
    @APIResponse(responseCode = "409", description = "CPF ou e-mail já cadastrado", content = @Content(schema = @Schema(implementation = ApiErro.class)))
    public Response cadastro(UsuariosService.Cadastro dados) {
        var usuario = usuarios.cadastrar(dados, "CLIENTE", null);
        auditoria.registrar(new Auditoria.Evento(usuario.id(), "CADASTRAR_CLIENTE", "USUARIO", usuario.id(), "SUCESSO"));
        return Response.status(Response.Status.CREATED).entity(usuario).build();
    }

    @POST
    @Path("/login")
    @PermitAll
    @Operation(summary = "Autenticar usuário", description = "Retorna um JWT com validade de 60 minutos.")
    @RequestBody(required = true, description = "E-mail e senha de usuário cadastrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuariosService.Login.class), examples = @ExampleObject(name = "Login", value = "{\"email\":\"cliente@example.com\",\"senha\":\"Senha123\"}")))
    @APIResponse(responseCode = "200", description = "Autenticado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuariosService.Token.class), examples = @ExampleObject(name = "Token", value = "{\"token\":\"<jwt>\",\"expiraEm\":\"2030-01-01T13:00:00Z\"}")))
    @APIResponse(responseCode = "400", description = "Dados ausentes", content = @Content(schema = @Schema(implementation = ApiErro.class)))
    @APIResponse(responseCode = "401", description = "Credenciais inválidas", content = @Content(schema = @Schema(implementation = ApiErro.class)))
    public UsuariosService.Token login(UsuariosService.Login dados) {
        try {
            var autenticacao = usuarios.autenticar(dados);
            auditoria.registrar(new Auditoria.Evento(autenticacao.usuarioId(), "LOGIN", "USUARIO", autenticacao.usuarioId(), "SUCESSO"));
            return autenticacao.token();
        } catch (FalhaAplicacao falha) {
            auditoria.registrar(new Auditoria.Evento(null, "LOGIN", "USUARIO", null, "RECUSADO"));
            throw falha;
        }
    }
}
