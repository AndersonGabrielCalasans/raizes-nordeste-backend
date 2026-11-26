package br.com.raizesnordeste.infrastructure;

import br.com.raizesnordeste.domain.GatewayPagamento;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GatewayPagamentoMockTest {
    @Test
    void retornaCenariosConfiguradosSemPagamentoReal() throws Exception {
        var mock = new GatewayPagamentoMock();
        var dados = new GatewayPagamento.Solicitacao(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("25.00"));
        for (var resultado : GatewayPagamento.Resultado.values()) {
            mock.resultado = resultado.name();
            assertEquals(resultado, mock.solicitar(dados));
        }
        mock.resultado = "INVALIDO";
        assertThrows(IllegalStateException.class, () -> mock.solicitar(dados));
    }

    @Test
    void simulacaoDeTimeoutRespeitaInterrupcao() {
        var mock = new GatewayPagamentoMock();
        mock.resultado = "TIMEOUT";
        Thread.currentThread().interrupt();
        try {
            assertThrows(InterruptedException.class, () -> mock.solicitar(null));
        } finally {
            Thread.interrupted();
        }
    }
}
