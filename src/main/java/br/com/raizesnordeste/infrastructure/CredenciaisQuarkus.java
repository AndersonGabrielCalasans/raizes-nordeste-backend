package br.com.raizesnordeste.infrastructure;

import br.com.raizesnordeste.domain.Credenciais;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class CredenciaisQuarkus implements Credenciais {
    public String gerarHash(String senha) { return BcryptUtil.bcryptHash(senha, 12); }
    public boolean conferirSenha(String senha, String hash) { return BcryptUtil.matches(senha, hash); }
    public String gerarToken(UUID id, String perfil, Instant expiracao) {
        return Jwt.subject(id.toString()).groups(Set.of(perfil)).expiresAt(expiracao).sign();
    }
}
