-- Dados de demonstração local. admin_cpf e admin_senha são informados via psql.
BEGIN;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO unidades (id, nome, endereco) VALUES
    ('10000000-0000-0000-0000-000000000001', 'Unidade Centro', 'Rua do Centro, 100'),
    ('10000000-0000-0000-0000-000000000002', 'Unidade Litoral', 'Rua do Litoral, 200')
ON CONFLICT DO NOTHING;

INSERT INTO produtos (id, nome, descricao) VALUES
    ('20000000-0000-0000-0000-000000000001', 'Cuscuz', 'Cuscuz nordestino'),
    ('20000000-0000-0000-0000-000000000002', 'Tapioca', 'Tapioca tradicional'),
    ('20000000-0000-0000-0000-000000000003', 'Baião de dois', 'Arroz e feijão com temperos')
ON CONFLICT DO NOTHING;

INSERT INTO produtos_unidades (id, unidade_id, produto_id, preco, disponivel, saldo_estoque) VALUES
    ('30000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 12.50, TRUE, 20),
    ('30000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', 10.00, TRUE, 15),
    ('30000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001', 13.00, TRUE, 15),
    ('30000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000003', 22.00, TRUE, 10)
ON CONFLICT DO NOTHING;

INSERT INTO usuarios (id, nome, cpf, data_nascimento, email, senha_hash, perfil)
VALUES ('40000000-0000-0000-0000-000000000001', 'Administrador Local', :'admin_cpf', '1990-01-01',
        'admin@raizes.local', crypt(:'admin_senha', gen_salt('bf', 12)), 'ADMINISTRADOR')
ON CONFLICT DO NOTHING;
COMMIT;
