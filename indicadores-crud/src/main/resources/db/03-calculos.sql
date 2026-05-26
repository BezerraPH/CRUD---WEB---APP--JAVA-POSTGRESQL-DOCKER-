-- Configuração de cálculos automáticos por formulário (slug do módulo)

CREATE TABLE IF NOT EXISTS formulario_calculo_config (
    slug VARCHAR(80) PRIMARY KEY,
    calculos_ativos BOOLEAN NOT NULL DEFAULT TRUE,
    herdar_saldo_anterior BOOLEAN NOT NULL DEFAULT TRUE,
    atualizado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS formulario_calculo_regra (
    id SERIAL PRIMARY KEY,
    slug VARCHAR(80) NOT NULL REFERENCES formulario_calculo_config(slug) ON DELETE CASCADE,
    campo_destino VARCHAR(63) NOT NULL,
    label VARCHAR(255) NOT NULL DEFAULT '',
    expressao TEXT NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    ordem INT NOT NULL DEFAULT 0,
    UNIQUE (slug, campo_destino)
);

CREATE INDEX IF NOT EXISTS idx_calculo_regra_slug ON formulario_calculo_regra (slug);
