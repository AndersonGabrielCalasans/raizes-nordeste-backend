"""Prepara dez Clientes fictícios no banco de testes, pela API."""
import argparse
import json
import os
from carga_mvp import requisitar


def cpf_ficticio(numero):
    digitos = [int(c) for c in f'{numero:09d}']
    for tamanho in (10, 11):
        resto = (sum(valor * (tamanho - i) for i, valor in enumerate(digitos)) * 10) % 11
        digitos.append(0 if resto == 10 else resto)
    return ''.join(str(d) for d in digitos)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--url', default='http://127.0.0.1:8085')
    args = parser.parse_args()
    senha = os.environ.get('RAIZES_TESTE_SENHA')
    if not senha:
        parser.error('definir RAIZES_TESTE_SENHA para os clientes fictícios')
    emails = []
    for numero in range(1, 11):
        email = f'carga{numero:02}@example.com'
        corpo = {'nome': f'Cliente Carga {numero:02}', 'cpf': cpf_ficticio(100000000 + numero),
                 'dataNascimento': '1990-03-10', 'email': email, 'senha': senha}
        status, _, _ = requisitar(args.url.rstrip('/'), 'POST', '/auth/cadastro', corpo=corpo)
        if status not in (201, 409):
            raise SystemExit(f'Cadastro do cliente {numero} falhou: HTTP {status}.')
        status_login, resposta, _ = requisitar(args.url.rstrip('/'), 'POST', '/auth/login',
                                              corpo={'email': email, 'senha': senha})
        if status_login != 200 or not resposta or not resposta.get('token'):
            raise SystemExit(f'Cliente {numero} não pôde autenticar; verifique conflitos de cadastro.')
        emails.append(email)
    print('Clientes fictícios prontos. RAIZES_TESTE_EMAILS:')
    print(','.join(emails))


if __name__ == '__main__':
    main()
