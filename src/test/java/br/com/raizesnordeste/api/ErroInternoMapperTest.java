package br.com.raizesnordeste.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.Test;

class ErroInternoMapperTest {
    @Test
    void retornaErroGenericoSemExporDetalhesInternos() {
        Response resposta = new ErroInternoMapper()
                .toResponse(new IllegalStateException("detalhe interno do banco"));

        assertEquals(500, resposta.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, resposta.getMediaType());
        assertEquals(new ApiErro("ERRO_INTERNO", "Não foi possível processar a solicitação.", List.of()),
                resposta.getEntity());
    }
}
