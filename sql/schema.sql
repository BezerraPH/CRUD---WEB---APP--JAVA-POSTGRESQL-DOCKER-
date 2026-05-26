-- Schema inicial — aplicado automaticamente na primeira subida do container PostgreSQL.
-- Chave primária: id. Várias entradas no mesmo dia são permitidas.

CREATE TABLE IF NOT EXISTS dados_indicadores (
    id SERIAL PRIMARY KEY,
    data_coleta DATE NOT NULL,
    sisgcorp_pf_pronto_p_analise NUMERIC DEFAULT 0,
    sisgcorp_pf_em_analise NUMERIC DEFAULT 0,
    sisgcorp_pf_restituido NUMERIC DEFAULT 0,
    sisgcorp_pf_acima_60 NUMERIC DEFAULT 0,
    sisgcorp_pj_pronto_p_analise NUMERIC DEFAULT 0,
    sisgcorp_pj_em_analise NUMERIC DEFAULT 0,
    sisgcorp_pj_restituido NUMERIC DEFAULT 0,
    sisgcorp_pj_acima_60 NUMERIC DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_dados_indicadores_data
    ON dados_indicadores (data_coleta DESC);

CREATE TABLE IF NOT EXISTS pce_controle_destruicao (
    id SERIAL PRIMARY KEY,
    infog_data DATE NOT NULL,
    infog_omosop VARCHAR(255) DEFAULT '',
    infog_nrtram VARCHAR(255) DEFAULT '',
    origem VARCHAR(255) DEFAULT '',
    pcerec_armas NUMERIC DEFAULT 0,
    pcerec_municao NUMERIC DEFAULT 0,
    pcerec_outrospce NUMERIC DEFAULT 0,
    pcerec_carregadores NUMERIC DEFAULT 0,
    saldoanosanteriorarmas NUMERIC DEFAULT 0,
    saldoanosanteriormunicao NUMERIC DEFAULT 0,
    saldoanosanterioroutrospce NUMERIC DEFAULT 0,
    saldoanosanteriorcarregadores NUMERIC DEFAULT 0,
    armasdestruidas NUMERIC DEFAULT 0,
    municaodestruidas NUMERIC DEFAULT 0,
    outrospcedestruidos NUMERIC DEFAULT 0,
    carregadoresdestruidos NUMERIC DEFAULT 0,
    armasdajusticasemordemdedestruicao NUMERIC DEFAULT 0,
    armasbrasonadas NUMERIC DEFAULT 0,
    armasrestituidas NUMERIC DEFAULT 0,
    saldoatualdearmas NUMERIC DEFAULT 0,
    saldoatualdemunicoes NUMERIC DEFAULT 0,
    saldoatualoutrospce NUMERIC DEFAULT 0,
    saldoatualcorregadores NUMERIC DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_pce_data ON pce_controle_destruicao (infog_data DESC);

-- Metadados do construtor de formulários (novos indicadores via interface web)
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
