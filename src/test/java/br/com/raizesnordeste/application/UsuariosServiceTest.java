package br.com.raizesnordeste.application;

import br.com.raizesnordeste.domain.Cpf;
import br.com.raizesnordeste.domain.Credenciais;
import br.com.raizesnordeste.domain.CadastroDuplicado;
import br.com.raizesnordeste.domain.RepositorioUsuarios;
import br.com.raizesnordeste.infrastructure.CredenciaisQuarkus;
import io.quarkus.elytron.security.common.BcryptUtil;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class UsuariosServiceTest {
    private final UsuariosService service = new UsuariosService();

    @Test
    void validaDigitosDoCpf() {
        assertTrue(Cpf.valido("52998224725"));
        assertFalse(Cpf.valido("52998224724"));
        assertFalse(Cpf.valido("11111111111"));
        assertFalse(Cpf.valido("123"));
    }

    @Test
    void recusaCamposObrigatoriosAusentes() {
        var erro = assertThrows(FalhaAplicacao.class, () -> service.cadastrar(null, "CLIENTE", null));
        assertEquals(400, erro.status());
    }

    @Test
    void recusaDataFutura() {
        var cadastro = new UsuariosService.Cadastro("Pessoa", "52998224725", LocalDate.now().plusDays(1),
                "pessoa@example.com", "Senha123");
        assertEquals(400,
                assertThrows(FalhaAplicacao.class, () -> service.cadastrar(cadastro, "CLIENTE", null)).status());
    }

    @Test
    void recusaSenhaCurtaOuComEspacos() {
        for (String senha : new String[] { "curta", "senha 123" }) {
            var cadastro = new UsuariosService.Cadastro("Pessoa", "52998224725", LocalDate.of(2000, 1, 1),
                    "pessoa@example.com", senha);
            assertEquals(400,
                    assertThrows(FalhaAplicacao.class, () -> service.cadastrar(cadastro, "CLIENTE", null)).status());
        }
    }

    @Test
    void recusaOperacionalSemUnidade() {
        var cadastro = new UsuariosService.Cadastro("Pessoa", "52998224725", LocalDate.of(2000, 1, 1),
                "pessoa@example.com", "Senha123");
        assertEquals(400,
                assertThrows(FalhaAplicacao.class, () -> service.cadastrar(cadastro, "ATENDENTE", null)).status());
    }

    @Test
    void recusaLoginSemDados() {
        assertEquals(400,
                assertThrows(FalhaAplicacao.class, () -> service.login(new UsuariosService.Login("", ""))).status());
    }

    @Test
    void cadastroNormalizaEmailEProtegeSenha() {
        var repositorio = new RepositorioFake();
        service.repositorio = repositorio;
        service.credenciais = new CredenciaisQuarkus();
        var cadastro = new UsuariosService.Cadastro(" Pessoa ", "529.982.247-25", LocalDate.of(2000, 1, 1),
                " PESSOA@example.com ", "Senha123");
        var usuario = service.cadastrar(cadastro, "CLIENTE", null);
        assertEquals("CLIENTE", usuario.perfil());
        assertEquals("pessoa@example.com", repositorio.salvo.email());
        assertEquals("52998224725", repositorio.salvo.cpf());
        assertNotEquals("Senha123", repositorio.salvo.senhaHash());
        assertTrue(BcryptUtil.matches("Senha123", repositorio.salvo.senhaHash()));
        assertTrue(repositorio.salvo.senhaHash().startsWith("$2a$12$"));
    }

    @Test
    void loginInvalidoRetornaMensagemGenerica() {
        service.repositorio = new RepositorioFake();
        var falha = assertThrows(FalhaAplicacao.class,
                () -> service.login(new UsuariosService.Login("ausente@example.com", "Senha123")));
        assertEquals(401, falha.status());
        assertEquals("E-mail ou senha inválidos.", falha.getMessage());
    }

    @Test
    void loginValidoEmiteTokenComValidadeDeUmaHora() {
        var repositorio = new RepositorioFake();
        var id = UUID.randomUUID();
        repositorio.salvo = new RepositorioUsuarios.Registro(id, "Pessoa", null, null, "pessoa@example.com", "hash", "CLIENTE", null);
        service.repositorio = repositorio;
        service.credenciais = new CredenciaisFake();
        Instant inicio = Instant.now();
        var token = service.login(new UsuariosService.Login(" PESSOA@example.com ", "Senha123"));
        assertEquals("token-CLIENTE", token.token());
        assertTrue(token.expiraEm().isAfter(inicio.plusSeconds(3598)));
        assertFalse(token.expiraEm().isAfter(Instant.now().plusSeconds(3600)));
    }

    @Test
    void duplicidadeRetornaConflito() {
        var repositorio = new RepositorioFake();
        repositorio.duplicado = true;
        service.repositorio = repositorio;
        service.credenciais = new CredenciaisFake();
        var cadastro = new UsuariosService.Cadastro("Pessoa", "52998224725", LocalDate.of(2000, 1, 1), "pessoa@example.com", "Senha123");
        assertEquals(409, assertThrows(FalhaAplicacao.class, () -> service.cadastrar(cadastro, "CLIENTE", null)).status());
    }

    @Test
    void unidadeInexistenteRecusaCadastro() {
        var repositorio = new RepositorioFake();
        repositorio.unidadeExiste = false;
        service.repositorio = repositorio;
        var cadastro = new UsuariosService.Cadastro("Pessoa", "52998224725", LocalDate.of(2000, 1, 1), "pessoa@example.com", "Senha123");
        assertEquals(404, assertThrows(FalhaAplicacao.class, () -> service.cadastrar(cadastro, "COZINHA", UUID.randomUUID())).status());
        assertNull(repositorio.salvo);
    }

    private static class CredenciaisFake implements Credenciais {
        public String gerarHash(String senha) { return "hash"; }
        public boolean conferirSenha(String senha, String hash) { return "Senha123".equals(senha); }
        public String gerarToken(UUID id, String perfil, Instant expiracao) { return "token-" + perfil; }
    }

    private static class RepositorioFake implements RepositorioUsuarios {
        Registro salvo;
        boolean duplicado;
        boolean unidadeExiste = true;

        public boolean unidadeExiste(UUID id) {
            return unidadeExiste;
        }

        public void inserir(Registro usuario) {
            if (duplicado) throw new CadastroDuplicado();
            salvo = usuario;
        }

        public Optional<Registro> buscarPorEmail(String email) {
            return salvo != null && email.equals(salvo.email()) ? Optional.of(salvo) : Optional.empty();
        }

        public Optional<Registro> alterarUnidade(UUID id, UUID unidadeId) {
            return Optional.empty();
        }
    }
}
