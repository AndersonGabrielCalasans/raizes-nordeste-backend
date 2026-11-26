# Auditoria — pedido criado em T05

Consulta executada em 26/11/2026 no PostgreSQL local, usando o ID do pedido apresentado em T05:

```sql
SELECT operacao, recurso, resultado
FROM eventos_auditoria
WHERE recurso_id = 'b258b669-b9a7-4fb9-8c9b-0cc1c9b8954d'::uuid
ORDER BY criado_em;
```

Resultado observado:

```text
operacao          recurso  resultado
CRIAR_PEDIDO      PEDIDO   SUCESSO
PAGAR_PEDIDO      PEDIDO   APROVADO
CANCELAR_PEDIDO   PEDIDO   SUCESSO
```

Esse identificador pertence ao pedido fictício desta execução; a reprodução exige o ID do pedido criado no novo teste.
