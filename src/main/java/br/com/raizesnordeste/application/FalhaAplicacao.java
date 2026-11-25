package br.com.raizesnordeste.application;

public class FalhaAplicacao extends RuntimeException {
    private final int status;
    private final String codigo;

    public FalhaAplicacao(int status, String codigo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
    }

    public int status() {
        return status;
    }

    public String codigo() {
        return codigo;
    }
}
