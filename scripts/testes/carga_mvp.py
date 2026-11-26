"""Medição local de RNF05; requer banco de testes e Clientes já cadastrados."""
import argparse
from collections import Counter
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
import json
import math
import os
from pathlib import Path
import threading
import time
import urllib.error
import urllib.request

UNIDADE = '10000000-0000-0000-0000-000000000001'
PRODUTO_UNIDADE = '30000000-0000-0000-0000-000000000001'


def percentil(valores, proporcao):
    if not valores:
        return None
    ordenados = sorted(valores)
    return ordenados[max(0, math.ceil(proporcao * len(ordenados)) - 1)]


def resumir(amostras, duracao, usuarios, intervalo):
    tempos = [a['ms'] for a in amostras if a['status'] is not None]
    total = len(amostras)
    ate_limite = sum(a['ms'] <= 2000 for a in amostras if a['status'] is not None)
    return {
        'usuarios': usuarios,
        'duracao_configurada_s': duracao,
        'intervalo_por_usuario_s': intervalo,
        'total_requisicoes': total,
        'por_operacao': dict(Counter(a['operacao'] for a in amostras)),
        'por_status': dict(Counter(str(a['status']) if a['status'] is not None else 'erro_transporte' for a in amostras)),
        'falhas_de_expectativa': sum(not a['ok'] for a in amostras),
        'erros_internos_5xx': sum(a['status'] is not None and a['status'] >= 500 for a in amostras),
        'erros_transporte': sum(a['status'] is None for a in amostras),
        'tempo_min_ms': round(min(tempos), 2) if tempos else None,
        'tempo_mediano_ms': round(percentil(tempos, 0.5), 2) if tempos else None,
        'tempo_p95_ms': round(percentil(tempos, 0.95), 2) if tempos else None,
        'tempo_max_ms': round(max(tempos), 2) if tempos else None,
        'respostas_ate_2s': ate_limite,
        'percentual_ate_2s_sobre_requisicoes': round(100 * ate_limite / total, 2) if total else 0,
        'pedidos_criados': sum(a['operacao'] == 'criar_pedido' and a['status'] == 201 for a in amostras),
    }


def requisitar(base, metodo, caminho, token=None, corpo=None, timeout=15):
    dados = json.dumps(corpo).encode('utf-8') if corpo is not None else None
    headers = {'Accept': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    if dados is not None:
        headers['Content-Type'] = 'application/json'
    req = urllib.request.Request(base + caminho, data=dados, headers=headers, method=metodo)
    inicio = time.perf_counter()
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resposta:
            conteudo = resposta.read()
            status = resposta.status
    except urllib.error.HTTPError as erro:
        status, conteudo = erro.code, erro.read()
    except (urllib.error.URLError, TimeoutError, OSError):
        return None, None, (time.perf_counter() - inicio) * 1000
    try:
        payload = json.loads(conteudo)
    except (ValueError, UnicodeError):
        payload = None
    return status, payload, (time.perf_counter() - inicio) * 1000


def operacao(indice):
    if indice % 3 == 0:
        return 'cardapio', 'GET', f'/unidades/{UNIDADE}/cardapio', None, 200
    if indice % 3 == 1:
        return 'consultar_pedidos', 'GET', '/pedidos?page=1&limit=20', None, 200
    return 'criar_pedido', 'POST', '/pedidos', {
        'unidadeId': UNIDADE, 'canalPedido': 'APP',
        'itens': [{'produtoUnidadeId': PRODUTO_UNIDADE, 'quantidade': 1}],
    }, 201


def medir(base, tokens, duracao, intervalo):
    usuarios = len(tokens)
    barreira = threading.Barrier(usuarios + 1)
    inicio = [0.0]

    def usuario(numero):
        barreira.wait()
        amostras = []
        proximo = inicio[0]
        indice = numero
        while proximo < inicio[0] + duracao:
            espera = proximo - time.perf_counter()
            if espera > 0:
                time.sleep(espera)
            if time.perf_counter() >= inicio[0] + duracao:
                break
            nome, metodo, caminho, corpo, esperado = operacao(indice)
            status, _, ms = requisitar(base, metodo, caminho, tokens[numero], corpo)
            amostras.append({'operacao': nome, 'status': status, 'ms': ms, 'ok': status == esperado})
            indice += 1
            proximo += intervalo
        restante = inicio[0] + duracao - time.perf_counter()
        if restante > 0:
            time.sleep(restante)
        return amostras

    with ThreadPoolExecutor(max_workers=usuarios) as pool:
        tarefas = [pool.submit(usuario, numero) for numero in range(usuarios)]
        inicio[0] = time.perf_counter()
        barreira.wait()
        amostras = [item for tarefa in tarefas for item in tarefa.result()]
    resultado = resumir(amostras, duracao, usuarios, intervalo)
    resultado['duracao_real_s'] = round(time.perf_counter() - inicio[0], 2)
    return resultado


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--url', default='http://127.0.0.1:8085')
    parser.add_argument('--usuarios', type=int, default=10)
    parser.add_argument('--duracao', type=int, default=300)
    parser.add_argument('--intervalo', type=float, default=1.0)
    parser.add_argument('--saida', required=True)
    args = parser.parse_args()
    if args.usuarios < 1 or args.duracao < 1 or args.intervalo <= 0:
        parser.error('usuários, duração e intervalo devem ser positivos')
    emails = [email.strip() for email in os.environ.get('RAIZES_TESTE_EMAILS', '').split(',') if email.strip()]
    senha = os.environ.get('RAIZES_TESTE_SENHA')
    if len(emails) != args.usuarios or len(set(emails)) != args.usuarios or not senha:
        parser.error('definir RAIZES_TESTE_EMAILS com um e-mail distinto por usuário e RAIZES_TESTE_SENHA')
    tokens = []
    for email in emails:
        status, resposta, _ = requisitar(args.url, 'POST', '/auth/login', corpo={'email': email, 'senha': senha})
        if status != 200 or not resposta or not resposta.get('token'):
            raise SystemExit('Login de teste não foi concluído; verifique a API e as credenciais.')
        tokens.append(resposta['token'])
    inicio = datetime.now(timezone.utc).isoformat()
    resultado = medir(args.url.rstrip('/'), tokens, args.duracao, args.intervalo)
    resultado['inicio_utc'] = inicio
    resultado['fim_utc'] = datetime.now(timezone.utc).isoformat()
    Path(args.saida).write_text(json.dumps(resultado, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    print(json.dumps(resultado, indent=2, ensure_ascii=False))
    if resultado['falhas_de_expectativa'] or resultado['percentual_ate_2s_sobre_requisicoes'] < 95:
        raise SystemExit(1)


if __name__ == '__main__':
    main()
