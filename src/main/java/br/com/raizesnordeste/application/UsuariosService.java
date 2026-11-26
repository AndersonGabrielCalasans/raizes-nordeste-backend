package br.com.raizesnordeste.application;

import br.com.raizesnordeste.domain.CadastroDuplicado;
import br.com.raizesnordeste.domain.Cpf;
import br.com.raizesnordeste.domain.Credenciais;
import br.com.raizesnordeste.domain.RepositorioUsuarios;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@ApplicationScoped
public class UsuariosService {

    @Inject
    RepositorioUsuarios repositorio;

    @Inject
    Credenciais credenciais;

    @Schema(requiredProperties = { "nome", "cpf", "dataNascimento", "email", "senha" })
    public record Cadastro(String nome, String cpf, LocalDate dataNascimento, String email, String senha) {
    }

    @Schema(requiredProperties = { "nome", "cpf", "dataNascimento", "email", "senha", "perfil", "unidadeId" })
    public record CadastroOperacional(String nome, String cpf, LocalDate dataNascimento, String email, String senha,
            String perfil, UUID unidadeId) {
    }

    @Schema(requiredProperties = { "email", "senha" })
    public record Login(String email, String senha) {
    }

    @Schema(requiredProperties = { "unidadeId" })
    public record Unidade(UUID unidadeId) {
    }

    public record Usuario(UUID id, String nome, String perfil, UUID unidadeId) {
    }

    public record Token(String token, Instant expiraEm) {
    }

    public static FalhaAplicacao erro(int status, String codigo, String mensagem) {
        return new FalhaAplicacao(status, codigo, mensagem);
    }

    @Transactional
    public Usuario cadastrar(Cadastro dados, String perfil, UUID unidadeId) {
        if (dados == null || dados.nome() == null || dados.nome().isBlank() || dados.nome().length() > 150
                || dados.cpf() == null || dados.dataNascimento() == null
                || dados.dataNascimento().isAfter(LocalDate.now())
                || dados.email() == null || dados.senha() == null) {
            throw erro(400, "DADOS_INVALIDOS", "Preencha os dados obrigatórios corretamente.");
        }
        String cpf = dados.cpf().replaceAll("[.\\-]", "");
        String email = dados.email().trim().toLowerCase(Locale.ROOT);
        if (!Cpf.valido(cpf) || email.length() > 254 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                || dados.senha().length() < 8 || dados.senha().codePoints().anyMatch(Character::isWhitespace)
                || dados.senha().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw erro(400, "DADOS_INVALIDOS", "CPF, e-mail ou senha inválidos.");
        }
        if (!perfil.equals("CLIENTE") && unidadeId == null) {
            throw erro(400, "DADOS_INVALIDOS", "Informe a unidade de atuação.");
        }
        if (unidadeId != null && !repositorio.unidadeExiste(unidadeId)) {
            throw erro(404, "UNIDADE_NAO_ENCONTRADA", "Unidade não encontrada.");
        }
        UUID id = UUID.randomUUID();
        try {
            repositorio.inserir(
                    new RepositorioUsuarios.Registro(id, dados.nome().trim(), cpf, dados.dataNascimento(), email,
                            credenciais.gerarHash(dados.senha()), perfil, unidadeId));
        } catch (CadastroDuplicado e) {
            throw erro(409, "CADASTRO_DUPLICADO", "CPF ou e-mail já cadastrado.");
        }
        return new Usuario(id, dados.nome().trim(), perfil, unidadeId);
    }

    public Token login(Login dados) {
        if (dados == null || dados.email() == null || dados.email().isBlank() || dados.senha() == null
                || dados.senha().isEmpty()) {
            throw erro(400, "DADOS_INVALIDOS", "Informe e-mail e senha.");
        }
        var usuario = repositorio.buscarPorEmail(dados.email().trim().toLowerCase(Locale.ROOT));
        if (usuario.isEmpty() || !credenciais.conferirSenha(dados.senha(), usuario.get().senhaHash())) {
            throw erro(401, "CREDENCIAIS_INVALIDAS", "E-mail ou senha inválidos.");
        }
        var registro = usuario.get();
        Instant expira = Instant.now().plusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        return new Token(credenciais.gerarToken(registro.id(), registro.perfil(), expira), expira);
    }

    @Transactional
    public Usuario alterarUnidade(UUID id, Unidade dados) {
        if (dados == null || dados.unidadeId() == null) {
            throw erro(400, "DADOS_INVALIDOS", "Informe a unidade.");
        }
        if (!repositorio.unidadeExiste(dados.unidadeId())) {
            throw erro(404, "UNIDADE_NAO_ENCONTRADA", "Unidade não encontrada.");
        }
        var usuario = repositorio.alterarUnidade(id, dados.unidadeId())
                .orElseThrow(() -> erro(404, "USUARIO_NAO_ENCONTRADO", "Usuário operacional não encontrado."));
        return new Usuario(usuario.id(), usuario.nome(), usuario.perfil(), usuario.unidadeId());
    }
}
