import unittest
from contextlib import redirect_stdout
from io import StringIO
from unittest.mock import patch
from carga_mvp import medir, operacao, percentil, resumir
from preparar_clientes import cpf_ficticio, main as preparar_clientes


class MedicaoTest(unittest.TestCase):
    def test_percentil_usa_nearest_rank_e_amostra_vazia(self):
        self.assertIsNone(percentil([], .95))
        self.assertEqual(19, percentil(list(range(1, 21)), .95))

    def test_resumo_conta_erro_transporte_e_status_incorreto(self):
        amostras = [
            {'operacao': 'criar_pedido', 'status': 201, 'ms': 40, 'ok': True},
            {'operacao': 'cardapio', 'status': 500, 'ms': 2100, 'ok': False},
            {'operacao': 'consultar_pedidos', 'status': None, 'ms': 30, 'ok': False},
        ]
        r = resumir(amostras, 5, 2, 1)
        self.assertEqual((1, 1, 2), (r['erros_internos_5xx'], r['erros_transporte'], r['falhas_de_expectativa']))
        self.assertEqual(33.33, r['percentual_ate_2s_sobre_requisicoes'])
        self.assertEqual(1, r['pedidos_criados'])

    def test_ciclo_cobre_consultas_e_criacao(self):
        ops = [operacao(i) for i in range(3)]
        self.assertEqual(['cardapio', 'consultar_pedidos', 'criar_pedido'], [op[0] for op in ops])
        self.assertEqual([200, 200, 201], [op[4] for op in ops])
        self.assertEqual(1, ops[2][3]['itens'][0]['quantidade'])

    def test_cpfs_de_clientes_sao_distintos_e_tem_digitos_validos(self):
        cpfs = [cpf_ficticio(100000000 + i) for i in range(1, 11)]
        self.assertEqual(10, len(set(cpfs)))
        for cpf in cpfs:
            self.assertEqual(11, len(cpf))
            for length in (9, 10):
                remainder = sum(int(cpf[i]) * (length + 1 - i) for i in range(length)) % 11
                expected = 0 if remainder < 2 else 11 - remainder
                self.assertEqual(expected, int(cpf[length]))

    def test_medicao_mantem_usuarios_e_duracao_configurada(self):
        vistos = set()

        def resposta_simulada(base, metodo, caminho, token, corpo):
            vistos.add(token)
            return (201 if metodo == 'POST' else 200), {}, 10

        with patch('carga_mvp.requisitar', side_effect=resposta_simulada):
            resultado = medir('http://teste', ['token1', 'token2'], 1, .5)
        self.assertEqual({'token1', 'token2'}, vistos)
        self.assertEqual(4, resultado['total_requisicoes'])
        self.assertGreaterEqual(resultado['duracao_real_s'], 1)
        self.assertEqual(0, resultado['falhas_de_expectativa'])

    def test_preparacao_confere_login_de_todas_as_contas(self):
        def resposta_simulada(base, metodo, caminho, corpo):
            if caminho == '/auth/cadastro':
                return 409, {}, 1
            return 200, {'token': 'exemplo'}, 1

        with patch.dict('os.environ', {'RAIZES_TESTE_SENHA': 'senha-teste'}), \
                patch('sys.argv', ['preparar_clientes.py']), \
                patch('preparar_clientes.requisitar', side_effect=resposta_simulada) as chamadas, \
                redirect_stdout(StringIO()):
            preparar_clientes()
        self.assertEqual(20, chamadas.call_count)

    def test_preparacao_interrompe_quando_conta_existente_nao_autentica(self):
        with patch.dict('os.environ', {'RAIZES_TESTE_SENHA': 'senha-teste'}), \
                patch('sys.argv', ['preparar_clientes.py']), \
                patch('preparar_clientes.requisitar', side_effect=[(409, {}, 1), (401, {}, 1)]), \
                self.assertRaises(SystemExit):
            preparar_clientes()


if __name__ == '__main__':
    unittest.main()
