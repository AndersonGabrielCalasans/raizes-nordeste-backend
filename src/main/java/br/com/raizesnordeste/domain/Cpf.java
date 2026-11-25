package br.com.raizesnordeste.domain;

public final class Cpf {

    private Cpf() {
    }

    public static boolean valido(String cpf) {
        if (!cpf.matches("[0-9]{11}") || cpf.chars().distinct().count() == 1)
            return false;
        for (int digito = 9; digito <= 10; digito++) {
            int soma = 0;
            for (int i = 0; i < digito; i++)
                soma += (cpf.charAt(i) - '0') * (digito + 1 - i);
            int resto = (soma * 10) % 11;
            if ((resto == 10 ? 0 : resto) != cpf.charAt(digito) - '0')
                return false;
        }
        return true;
    }
}
