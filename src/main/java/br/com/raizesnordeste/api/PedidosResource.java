package br.com.raizesnordeste.api;

import br.com.raizesnordeste.application.FalhaAplicacao;
import br.com.raizesnordeste.application.FluxoPedidoService;
import br.com.raizesnordeste.application.PedidosService;
import br.com.raizesnordeste.domain.Auditoria;
import br.com.raizesnordeste.domain.RepositorioFluxoPedido;
import br.com.raizesnordeste.domain.RepositorioPedidos;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.UUID;
import java.util.function.Supplier;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/pedidos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({ "CLIENTE", "ATENDENTE", "COZINHA", "ADMINISTRADOR" })
@Tag(name = "Pedidos")
public class PedidosResource {
    @Inject
    PedidosService pedidos;
    @Inject
    FluxoPedidoService fluxo;
    @Inject
    JsonWebToken jwt;
    @Inject
    Auditoria auditoria;

    private UUID usuario() {
        return UUID.fromString(jwt.getSubject());
    }

    private <T> T auditarRecusa(UUID recursoId, Supplier<T> operacao) {
        try {
            return operacao.get();
        } catch (FalhaAplicacao falha) {
            if (falha.status() == 403)
                auditoria.registrar(new Auditoria.Evento(usuario(), "ACESSO_NEGADO", "PEDIDO", recursoId, "RECUSADO"));
            throw falha;
        }
    }

    @POST
    @Operation(summary = "Criar pedido", description = "Cliente cria APP/WEB; Atendente cria BALCAO na própria unidade. Preço e estoque são validados pelo servidor.")
    @RequestBody(required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = PedidosService.Criacao.class), examples = @ExampleObject(name = "Pedido APP", value = "{\"unidadeId\":\"10000000-0000-0000-0000-000000000001\",\"canalPedido\":\"APP\",\"itens\":[{\"produtoUnidadeId\":\"30000000-0000-0000-0000-000000000001\",\"quantidade\":2}]}")))
    @APIResponse(responseCode = "201", description = "Pedido criado", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "Pedido criado", value = "{\"id\":\"50000000-0000-0000-0000-000000000001\",\"unidadeId\":\"10000000-0000-0000-0000-000000000001\",\"clienteId\":\"40000000-0000-0000-0000-000000000001\",\"criadorId\":\"40000000-0000-0000-0000-000000000001\",\"canalPedido\":\"APP\",\"status\":\"AGUARDANDO_PAGAMENTO\",\"situacaoPagamento\":\"PENDENTE\",\"valorTotal\":50.00,\"itens\":[{\"produtoUnidadeId\":\"30000000-0000-0000-0000-000000000001\",\"quantidade\":2,\"precoUnitario\":25.00,\"subtotal\":50.00}]}")))
    @APIResponse(responseCode = "400", description = "Dados inválidos")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Canal ou unidade sem permissão")
    @APIResponse(responseCode = "404", description = "Unidade ou produto inexistente")
    @APIResponse(responseCode = "409", description = "Produto indisponível ou saldo insuficiente")
    public Response criar(PedidosService.Criacao dados) {
        return Response.status(201).entity(auditarRecusa(null, () -> pedidos.criar(usuario(), dados))).build();
    }

    @GET
    @Operation(summary = "Listar pedidos", description = "Lista paginada restrita ao perfil, propriedade e unidade. Cozinha vê BALCAO ou pedidos pagos. Filtros canalPedido e status são opcionais.")
    @APIResponse(responseCode = "200", description = "Pedidos", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "Lista", value = "{\"items\":[],\"page\":1,\"limit\":20,\"total\":0}")))
    @APIResponse(responseCode = "400", description = "Filtro ou paginação inválida")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Sem permissão")
    public RepositorioPedidos.Pagina listar(@Parameter(description = "Canal de origem", example = "APP") @QueryParam("canalPedido") String canal,
            @Parameter(description = "Status operacional", example = "AGUARDANDO_PAGAMENTO") @QueryParam("status") String status,
            @Parameter(description = "Página inicial 1", example = "1") @DefaultValue("1") @QueryParam("page") int pagina,
            @Parameter(description = "Itens por página, entre 1 e 100", example = "20") @DefaultValue("20") @QueryParam("limit") int limite) {
        return auditarRecusa(null, () -> pedidos.listar(usuario(), canal, status, pagina, limite));
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Consultar pedido", description = "Respeita propriedade, unidade e visibilidade da Cozinha.")
    @APIResponse(responseCode = "200", description = "Pedido", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "Pedido", value = "{\"id\":\"50000000-0000-0000-0000-000000000001\",\"unidadeId\":\"10000000-0000-0000-0000-000000000001\",\"clienteId\":null,\"criadorId\":\"40000000-0000-0000-0000-000000000001\",\"canalPedido\":\"BALCAO\",\"status\":\"AGUARDANDO_PAGAMENTO\",\"situacaoPagamento\":\"PENDENTE\",\"valorTotal\":25.00,\"itens\":[{\"produtoUnidadeId\":\"30000000-0000-0000-0000-000000000001\",\"quantidade\":1,\"precoUnitario\":25.00,\"subtotal\":25.00}]}")))
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Pedido sem permissão")
    @APIResponse(responseCode = "404", description = "Pedido inexistente")
    public RepositorioPedidos.Pedido buscar(@Parameter(description = "UUID do pedido", required = true, example = "50000000-0000-0000-0000-000000000001") @PathParam("id") UUID id) {
        return auditarRecusa(id, () -> pedidos.buscar(usuario(), id));
    }

    @POST
    @Path("/{id}/pagamentos")
    @Consumes(MediaType.WILDCARD)
    @Operation(summary = "Solicitar pagamento simulado", description = "Cliente proprietário em APP/WEB ou Atendente da unidade em BALCAO. Não realiza cobrança real; repetir uma aprovação retorna a mesma tentativa.")
    @APIResponse(responseCode = "200", description = "Resultado da tentativa mock", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "Pagamento aprovado", value = "{\"tentativaId\":\"60000000-0000-0000-0000-000000000001\",\"pedidoId\":\"50000000-0000-0000-0000-000000000001\",\"resultado\":\"APROVADO\",\"situacaoPagamento\":\"APROVADO\",\"statusPedido\":\"RECEBIDO\"}")))
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Pagamento não autorizado")
    @APIResponse(responseCode = "404", description = "Pedido inexistente")
    @APIResponse(responseCode = "409", description = "Pedido finalizado")
    public RepositorioFluxoPedido.Tentativa pagar(@Parameter(description = "UUID do pedido", required = true, example = "50000000-0000-0000-0000-000000000001") @PathParam("id") UUID id) {
        return auditarRecusa(id, () -> fluxo.pagar(usuario(), id));
    }

    @PATCH
    @Path("/{id}/status")
    @Operation(summary = "Atualizar status operacional", description = "Cozinha inicia preparo e marca pronto; Atendente conclui quando pronto e pago. Ambos atuam na própria unidade.")
    @RequestBody(required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = FluxoPedidoService.AlteracaoStatus.class), examples = @ExampleObject(name = "Iniciar preparo", value = "{\"status\":\"EM_PREPARO\"}")))
    @APIResponse(responseCode = "200", description = "Status atualizado", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "Em preparo", value = "{\"id\":\"50000000-0000-0000-0000-000000000001\",\"status\":\"EM_PREPARO\",\"situacaoPagamento\":\"APROVADO\"}")))
    @APIResponse(responseCode = "400", description = "Status inválido") @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Perfil ou unidade sem permissão") @APIResponse(responseCode = "404", description = "Pedido inexistente") @APIResponse(responseCode = "409", description = "Transição não permitida")
    public FluxoPedidoService.Estado atualizarStatus(@Parameter(description = "UUID do pedido", required = true, example = "50000000-0000-0000-0000-000000000001") @PathParam("id") UUID id, FluxoPedidoService.AlteracaoStatus dados) {
        return auditarRecusa(id, () -> fluxo.atualizarStatus(usuario(), id, dados));
    }

    @POST
    @Path("/{id}/cancelamento")
    @Consumes(MediaType.WILDCARD)
    @Operation(summary = "Cancelar pedido", description = "Cliente proprietário, Atendente da unidade ou Administrador pode cancelar antes do preparo; restitui estoque e simula estorno quando pago.")
    @APIResponse(responseCode = "200", description = "Pedido cancelado", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "Cancelamento", value = "{\"id\":\"50000000-0000-0000-0000-000000000001\",\"status\":\"CANCELADO\",\"situacaoPagamento\":\"ESTORNADO\"}")))
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido") @APIResponse(responseCode = "403", description = "Cancelamento não autorizado")
    @APIResponse(responseCode = "404", description = "Pedido inexistente") @APIResponse(responseCode = "409", description = "Cancelamento não permitido")
    public FluxoPedidoService.Estado cancelar(@Parameter(description = "UUID do pedido", required = true, example = "50000000-0000-0000-0000-000000000001") @PathParam("id") UUID id) {
        return auditarRecusa(id, () -> fluxo.cancelar(usuario(), id));
    }
}
