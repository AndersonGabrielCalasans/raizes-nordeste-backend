CREATE TYPE perfil_usuario AS ENUM ('CLIENTE', 'ATENDENTE', 'COZINHA', 'ADMINISTRADOR');
CREATE TYPE canal_pedido AS ENUM ('APP', 'WEB', 'BALCAO', 'TOTEM', 'PICKUP');
CREATE TYPE status_pedido AS ENUM ('AGUARDANDO_PAGAMENTO', 'RECEBIDO', 'EM_PREPARO', 'PRONTO', 'CONCLUIDO', 'CANCELADO');
CREATE TYPE situacao_pagamento AS ENUM ('PENDENTE', 'APROVADO', 'RECUSADO', 'FALHA', 'ESTORNADO');
CREATE TYPE tipo_movimentacao_estoque AS ENUM ('ENTRADA', 'SAIDA');

CREATE TABLE unidades (
    id UUID PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    endereco VARCHAR(255) NOT NULL
);

CREATE TABLE usuarios (
    id UUID PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    cpf VARCHAR(11) UNIQUE,
    data_nascimento DATE,
    email VARCHAR(254) NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    perfil perfil_usuario NOT NULL,
    unidade_id UUID REFERENCES unidades (id),
    CONSTRAINT usuarios_perfil_unidade_chk CHECK (
        (perfil IN ('ATENDENTE', 'COZINHA') AND unidade_id IS NOT NULL)
        OR (perfil IN ('CLIENTE', 'ADMINISTRADOR') AND unidade_id IS NULL)
    )
);

CREATE TABLE produtos (
    id UUID PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    descricao VARCHAR(500)
);

CREATE TABLE produtos_unidades (
    id UUID PRIMARY KEY,
    unidade_id UUID NOT NULL REFERENCES unidades (id),
    produto_id UUID NOT NULL REFERENCES produtos (id),
    preco NUMERIC(12, 2) NOT NULL CHECK (preco >= 0),
    disponivel BOOLEAN NOT NULL DEFAULT TRUE,
    saldo_estoque INTEGER NOT NULL DEFAULT 0 CHECK (saldo_estoque >= 0),
    CONSTRAINT produtos_unidades_unidade_produto_uk UNIQUE (unidade_id, produto_id)
);

CREATE TABLE pedidos (
    id UUID PRIMARY KEY,
    unidade_id UUID NOT NULL REFERENCES unidades (id),
    cliente_id UUID REFERENCES usuarios (id),
    criador_id UUID NOT NULL REFERENCES usuarios (id),
    canal_pedido canal_pedido NOT NULL,
    status status_pedido NOT NULL DEFAULT 'AGUARDANDO_PAGAMENTO',
    situacao_pagamento situacao_pagamento NOT NULL DEFAULT 'PENDENTE',
    valor_total NUMERIC(12, 2) NOT NULL CHECK (valor_total >= 0),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pedidos_cliente_por_canal_chk CHECK (
        (canal_pedido IN ('APP', 'WEB') AND cliente_id IS NOT NULL)
        OR (canal_pedido = 'BALCAO' AND cliente_id IS NULL)
        OR canal_pedido IN ('TOTEM', 'PICKUP')
    )
);

CREATE TABLE itens_pedido (
    id UUID PRIMARY KEY,
    pedido_id UUID NOT NULL REFERENCES pedidos (id),
    produto_unidade_id UUID NOT NULL REFERENCES produtos_unidades (id),
    quantidade INTEGER NOT NULL CHECK (quantidade > 0),
    preco_unitario NUMERIC(12, 2) NOT NULL CHECK (preco_unitario >= 0),
    subtotal NUMERIC(12, 2) NOT NULL CHECK (subtotal >= 0)
);

CREATE TABLE movimentacoes_estoque (
    id UUID PRIMARY KEY,
    produto_unidade_id UUID NOT NULL REFERENCES produtos_unidades (id),
    pedido_id UUID REFERENCES pedidos (id),
    tipo tipo_movimentacao_estoque NOT NULL,
    quantidade INTEGER NOT NULL CHECK (quantidade > 0),
    origem VARCHAR(40) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tentativas_pagamento (
    id UUID PRIMARY KEY,
    pedido_id UUID NOT NULL REFERENCES pedidos (id),
    valor_solicitado NUMERIC(12, 2) NOT NULL CHECK (valor_solicitado >= 0),
    resultado situacao_pagamento NOT NULL,
    falha VARCHAR(255),
    estorno_simulado BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE eventos_auditoria (
    id UUID PRIMARY KEY,
    responsavel_id UUID REFERENCES usuarios (id),
    operacao VARCHAR(80) NOT NULL,
    recurso VARCHAR(80) NOT NULL,
    recurso_id UUID,
    resultado VARCHAR(80) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX pedidos_cliente_id_idx ON pedidos (cliente_id);
CREATE INDEX pedidos_unidade_id_idx ON pedidos (unidade_id);
CREATE INDEX pedidos_canal_status_idx ON pedidos (canal_pedido, status);
CREATE INDEX movimentacoes_estoque_produto_unidade_id_idx ON movimentacoes_estoque (produto_unidade_id);
CREATE INDEX tentativas_pagamento_pedido_id_idx ON tentativas_pagamento (pedido_id);
CREATE INDEX eventos_auditoria_responsavel_id_idx ON eventos_auditoria (responsavel_id);
