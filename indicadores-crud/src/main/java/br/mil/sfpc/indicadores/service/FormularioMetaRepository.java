package br.mil.sfpc.indicadores.service;

import br.mil.sfpc.indicadores.config.CampoConfig;
import br.mil.sfpc.indicadores.config.ColunaListagem;
import br.mil.sfpc.indicadores.config.GrupoConfig;
import br.mil.sfpc.indicadores.config.ModuloConfig;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Acesso JDBC às tabelas de metadados ({@code formulario_def}, {@code formulario_grupo},
 * {@code formulario_campo}, {@code formulario_listagem}) e montagem de {@link ModuloConfig}.
 */
@Repository
public class FormularioMetaRepository {

    private final JdbcTemplate jdbc;

    public FormularioMetaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<ModuloConfig> carregarTodos() {
        List<Map<String, Object>> defs = jdbc.queryForList(
                "SELECT * FROM formulario_def ORDER BY titulo");
        List<ModuloConfig> modulos = new ArrayList<>();
        for (Map<String, Object> def : defs) {
            modulos.add(montarModulo(def));
        }
        return modulos;
    }

    public Optional<ModuloConfig> carregarPorSlug(String slug) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM formulario_def WHERE slug = ?", slug);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(montarModulo(rows.get(0)));
    }

    public boolean existeSlug(String slug) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM formulario_def WHERE slug = ?", Integer.class, slug);
        return n != null && n > 0;
    }

    public boolean existeTabela(String tabela) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM formulario_def WHERE tabela = ?", Integer.class, tabela);
        return n != null && n > 0;
    }

    public boolean tabelaFisicaExiste(String tabela) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?",
                Integer.class, tabela);
        return n != null && n > 0;
    }

    public long inserirDefinicao(String slug, String titulo, String descricao, String tabela,
                                 String colunaOrdem, String processador) {
        Long id = jdbc.queryForObject(
                "INSERT INTO formulario_def (slug, titulo, descricao, tabela, coluna_ordem, processador) "
                        + "VALUES (?, ?, ?, ?, ?, ?) RETURNING id",
                Long.class,
                slug, titulo, descricao != null ? descricao : "", tabela, colunaOrdem, processador);
        return id != null ? id : 0L;
    }

    public long inserirGrupo(long formularioId, String titulo, String estilo, int colunas, int ordem) {
        Long id = jdbc.queryForObject(
                "INSERT INTO formulario_grupo (formulario_id, titulo, estilo, colunas, ordem) "
                        + "VALUES (?, ?, ?, ?, ?) RETURNING id",
                Long.class,
                formularioId, titulo, estilo, colunas, ordem);
        return id != null ? id : 0L;
    }

    public void inserirCampo(long grupoId, String nome, String label, String tipo, boolean obrigatorio, int ordem) {
        jdbc.update(
                "INSERT INTO formulario_campo (grupo_id, nome, label, tipo, obrigatorio, ordem) VALUES (?, ?, ?, ?, ?, ?)",
                grupoId, nome, label, tipo, obrigatorio, ordem);
    }

    public void inserirColunaListagem(long formularioId, String nomeColuna, String label, int ordem) {
        jdbc.update(
                "INSERT INTO formulario_listagem (formulario_id, nome_coluna, label, ordem) VALUES (?, ?, ?, ?)",
                formularioId, nomeColuna, label, ordem);
    }

    public void atualizarMetadados(String slug, String titulo, String descricao) {
        jdbc.update("UPDATE formulario_def SET titulo = ?, descricao = ? WHERE slug = ?",
                titulo, descricao != null ? descricao : "", slug);
    }

    public void excluirPorSlug(String slug) {
        jdbc.update("DELETE FROM formulario_def WHERE slug = ?", slug);
    }

    public void suprimirSlug(String slug) {
        if (!tabelaSuprimidoExiste()) {
            return;
        }
        jdbc.update(
                "INSERT INTO formulario_suprimido (slug) VALUES (?) ON CONFLICT (slug) DO NOTHING",
                slug);
    }

    public boolean isSuprimido(String slug) {
        if (!tabelaSuprimidoExiste()) {
            return false;
        }
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM formulario_suprimido WHERE slug = ?", Integer.class, slug);
        return n != null && n > 0;
    }

    public Set<String> slugsSuprimidos() {
        if (!tabelaSuprimidoExiste()) {
            return Set.of();
        }
        return new HashSet<>(jdbc.query(
                "SELECT slug FROM formulario_suprimido",
                (rs, rowNum) -> rs.getString("slug")));
    }

    private boolean tabelaSuprimidoExiste() {
        Boolean existe = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_name = 'formulario_suprimido')",
                Boolean.class);
        return Boolean.TRUE.equals(existe);
    }

    public Optional<Long> idPorSlug(String slug) {
        List<Long> ids = jdbc.query("SELECT id FROM formulario_def WHERE slug = ?",
                (rs, rowNum) -> rs.getLong("id"), slug);
        return ids.isEmpty() ? Optional.empty() : Optional.of(ids.get(0));
    }

    public Optional<Long> grupoPrincipalId(long formularioId) {
        List<Long> ids = jdbc.query(
                "SELECT id FROM formulario_grupo WHERE formulario_id = ? ORDER BY ordem LIMIT 1",
                (rs, rowNum) -> rs.getLong("id"), formularioId);
        return ids.isEmpty() ? Optional.empty() : Optional.of(ids.get(0));
    }

    public boolean campoExiste(long formularioId, String nomeColuna) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM formulario_campo c JOIN formulario_grupo g ON g.id = c.grupo_id WHERE g.formulario_id = ? AND c.nome = ?",
                Integer.class, formularioId, nomeColuna);
        return n != null && n > 0;
    }

    private ModuloConfig montarModulo(Map<String, Object> def) {
        long formularioId = ((Number) def.get("id")).longValue();
        ModuloConfig modulo = new ModuloConfig();
        modulo.setSlug((String) def.get("slug"));
        modulo.setTitulo((String) def.get("titulo"));
        modulo.setDescricao(def.get("descricao") != null ? def.get("descricao").toString() : "");
        modulo.setTabela((String) def.get("tabela"));
        modulo.setColunaOrdem((String) def.get("coluna_ordem"));
        Object proc = def.get("processador");
        if (proc != null && !proc.toString().isBlank()) {
            modulo.setProcessador(proc.toString());
        }

        List<Map<String, Object>> gruposRows = jdbc.queryForList(
                "SELECT * FROM formulario_grupo WHERE formulario_id = ? ORDER BY ordem", formularioId);
        List<GrupoConfig> grupos = new ArrayList<>();
        for (Map<String, Object> gr : gruposRows) {
            long grupoId = ((Number) gr.get("id")).longValue();
            GrupoConfig grupo = new GrupoConfig();
            grupo.setTitulo((String) gr.get("titulo"));
            grupo.setEstilo(gr.get("estilo") != null ? gr.get("estilo").toString() : "primary");
            grupo.setColunas(gr.get("colunas") != null ? ((Number) gr.get("colunas")).intValue() : 2);

            List<Map<String, Object>> camposRows = jdbc.queryForList(
                    "SELECT * FROM formulario_campo WHERE grupo_id = ? ORDER BY ordem", grupoId);
            List<CampoConfig> campos = new ArrayList<>();
            for (Map<String, Object> cr : camposRows) {
                CampoConfig campo = new CampoConfig();
                campo.setNome((String) cr.get("nome"));
                campo.setLabel((String) cr.get("label"));
                campo.setTipo(cr.get("tipo") != null ? cr.get("tipo").toString() : "number");
                campo.setObrigatorio(Boolean.TRUE.equals(cr.get("obrigatorio")));
                campos.add(campo);
            }
            grupo.setCampos(campos);
            grupos.add(grupo);
        }
        modulo.setGrupos(grupos);

        List<Map<String, Object>> listRows = jdbc.queryForList(
                "SELECT * FROM formulario_listagem WHERE formulario_id = ? ORDER BY ordem", formularioId);
        List<ColunaListagem> colunas = new ArrayList<>();
        for (Map<String, Object> lr : listRows) {
            ColunaListagem col = new ColunaListagem();
            col.setNome((String) lr.get("nome_coluna"));
            col.setLabel((String) lr.get("label"));
            colunas.add(col);
        }
        modulo.setColunasListagem(colunas);
        return modulo;
    }

    public List<Map<String, Object>> listarResumo() {
        return jdbc.queryForList(
                "SELECT slug, titulo, COALESCE(descricao, '') AS descricao, tabela, criado_em FROM formulario_def ORDER BY titulo");
    }

    public Map<String, Object> resumoPorSlug(String slug) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM formulario_def WHERE slug = ?", slug);
        return rows.isEmpty() ? new LinkedHashMap<>() : rows.get(0);
    }
}
