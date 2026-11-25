package br.com.raizesnordeste.api;

import br.com.raizesnordeste.application.UsuariosService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.UUID;

@Path("/usuarios")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMINISTRADOR")
public class UsuariosResource {
    @Inject
    UsuariosService usuarios;

    @POST
    public Response cadastrar(UsuariosService.CadastroOperacional dados) {
        if (dados == null || !("ATENDENTE".equals(dados.perfil()) || "COZINHA".equals(dados.perfil())))
            throw UsuariosService.erro(400, "DADOS_INVALIDOS", "Perfil operacional inválido.");
        var cadastro = new UsuariosService.Cadastro(dados.nome(), dados.cpf(), dados.dataNascimento(), dados.email(),
                dados.senha());
        return Response.status(Response.Status.CREATED)
                .entity(usuarios.cadastrar(cadastro, dados.perfil(), dados.unidadeId())).build();
    }

    @PATCH
    @Path("/{id}/unidade")
    public UsuariosService.Usuario alterarUnidade(@PathParam("id") UUID id, UsuariosService.Unidade dados) {
        return usuarios.alterarUnidade(id, dados);
    }
}
