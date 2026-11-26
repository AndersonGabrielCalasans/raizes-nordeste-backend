package br.com.raizesnordeste.infrastructure;

import br.com.raizesnordeste.domain.GatewayPagamento;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class GatewayPagamentoMock implements GatewayPagamento {
    @ConfigProperty(name = "raizes.pagamento.mock", defaultValue = "APROVADO")
    String resultado;

    @Override
    public Resultado solicitar(Solicitacao solicitacao) throws Exception {
        return switch (resultado) {
            case "APROVADO" -> Resultado.APROVADO;
            case "RECUSADO" -> Resultado.RECUSADO;
            case "FALHA" -> Resultado.FALHA;
            case "TIMEOUT" -> { Thread.sleep(6000); yield Resultado.FALHA; }
            default -> throw new IllegalStateException("Resultado do mock inválido");
        };
    }
}
