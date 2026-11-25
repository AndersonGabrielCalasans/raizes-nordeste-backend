package br.com.raizesnordeste.api;

import br.com.raizesnordeste.application.UsuariosService;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {
    @Inject
    UsuariosService usuarios;

    @POST
    @Path("/cadastro")
    @PermitAll
    public Response cadastro(UsuariosService.Cadastro dados) {
        return Response.status(Response.Status.CREATED).entity(usuarios.cadastrar(dados, "CLIENTE", null)).build();
    }

    @POST
    @Path("/login")
    @PermitAll
    public UsuariosService.Token login(UsuariosService.Login dados) {
        return usuarios.login(dados);
    }
}
