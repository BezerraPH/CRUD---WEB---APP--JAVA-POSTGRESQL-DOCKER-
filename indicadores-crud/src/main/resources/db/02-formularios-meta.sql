-- Metadados dos formulários criados pela aplicação (construtor visual)

CREATE TABLE IF NOT EXISTS formulario_def (
    id SERIAL PRIMARY KEY,
    slug VARCHAR(80) NOT NULL UNIQUE,
    titulo VARCHAR(255) NOT NULL,
    descricao TEXT DEFAULT '',
    tabela VARCHAR(63) NOT NULL UNIQUE,
    coluna_ordem VARCHAR(63) NOT NULL,
    processador VARCHAR(32),
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS formulario_grupo (
    id SERIAL PRIMARY KEY,
    formulario_id INT NOT NULL REFERENCES formulario_def(id) ON DELETE CASCADE,
    titulo VARCHAR(255) NOT NULL,
    estilo VARCHAR(32) DEFAULT 'primary',
    colunas INT DEFAULT 2,
    ordem INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS formulario_campo (
    id SERIAL PRIMARY KEY,
    grupo_id INT NOT NULL REFERENCES formulario_grupo(id) ON DELETE CASCADE,
    nome VARCHAR(63) NOT NULL,
    label VARCHAR(255) NOT NULL,
    tipo VARCHAR(16) NOT NULL DEFAULT 'number',
    obrigatorio BOOLEAN DEFAULT FALSE,
    ordem INT NOT NULL DEFAULT 0,
    UNIQUE (grupo_id, nome)
);

CREATE TABLE IF NOT EXISTS formulario_listagem (
    id SERIAL PRIMARY KEY,
    formulario_id INT NOT NULL REFERENCES formulario_def(id) ON DELETE CASCADE,
    nome_coluna VARCHAR(63) NOT NULL,
    label VARCHAR(255) NOT NULL,
    ordem INT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_formulario_def_slug ON formulario_def (slug);
