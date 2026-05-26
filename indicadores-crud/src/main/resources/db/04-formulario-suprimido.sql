-- Formulários definidos no YAML que o usuário excluiu pela interface (não reaparecem no catálogo)

CREATE TABLE IF NOT EXISTS formulario_suprimido (
    slug VARCHAR(80) PRIMARY KEY,
    suprimido_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
