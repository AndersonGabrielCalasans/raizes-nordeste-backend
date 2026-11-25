package br.com.raizesnordeste.api;

import br.com.raizesnordeste.application.FalhaAplicacao;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class FalhaMapper implements ExceptionMapper<FalhaAplicacao> {
    public Response toResponse(FalhaAplicacao falha) {
        return Response.status(falha.status()).entity(new ApiErro(falha.codigo(), falha.getMessage())).build();
    }
}
