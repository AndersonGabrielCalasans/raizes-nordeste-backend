package br.com.raizesnordeste.api;

import java.util.List;

public record ApiErro(String codigo, String mensagem, List<String> detalhes) {
    public ApiErro(String codigo, String mensagem) {
        this(codigo, mensagem, List.of());
    }
}
