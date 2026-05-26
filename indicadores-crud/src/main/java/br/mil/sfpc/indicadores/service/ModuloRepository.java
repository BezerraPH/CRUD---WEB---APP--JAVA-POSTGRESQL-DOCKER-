package br.mil.sfpc.indicadores.service;

import br.mil.sfpc.indicadores.config.ModuloConfig;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Operações SQL genéricas (INSERT/UPDATE/DELETE/SELECT) na tabela física de cada módulo.
 */
@Repository
public class ModuloRepository {

    private final JdbcTemplate jdbc;

    public ModuloRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> listar(ModuloConfig modulo) {
        String sql = "SELECT * FROM " + modulo.getTabela()
                + " ORDER BY " + modulo.getColunaOrdem() + " DESC, id DESC";
        return jdbc.queryForList(sql);
    }

    public Optional<Map<String, Object>> buscarPorId(ModuloConfig modulo, long id) {
        String sql = "SELECT * FROM " + modulo.getTabela() + " WHERE id = ?";
        List<Map<String, Object>> rows = jdbc.queryForList(sql, id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public Optional<Map<String, Object>> ultimoRegistro(ModuloConfig modulo) {
        String sql = "SELECT * FROM " + modulo.getTabela()
                + " ORDER BY " + modulo.getColunaOrdem() + " DESC, id DESC LIMIT 1";
        List<Map<String, Object>> rows = jdbc.queryForList(sql);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public long inserir(ModuloConfig modulo, Map<String, Object> valores) {
        List<String> colunas = valores.keySet().stream().sorted().toList();
        String cols = String.join(", ", colunas);
        String placeholders = String.join(", ", colunas.stream().map(c -> "?").toList());
        Object[] params = colunas.stream().map(valores::get).toArray();
        String sql = "INSERT INTO " + modulo.getTabela() + " (" + cols + ") VALUES (" + placeholders + ") RETURNING id";
        Long id = jdbc.queryForObject(sql, Long.class, params);
        return id != null ? id : 0L;
    }

    public void atualizar(ModuloConfig modulo, long id, Map<String, Object> valores) {
        List<String> colunas = valores.keySet().stream().sorted().toList();
        String sets = String.join(", ", colunas.stream().map(c -> c + " = ?").toList());
        Object[] params = new Object[colunas.size() + 1];
        for (int i = 0; i < colunas.size(); i++) {
            params[i] = valores.get(colunas.get(i));
        }
        params[colunas.size()] = id;
        String sql = "UPDATE " + modulo.getTabela() + " SET " + sets + " WHERE id = ?";
        jdbc.update(sql, params);
    }

    public void excluir(ModuloConfig modulo, long id) {
        jdbc.update("DELETE FROM " + modulo.getTabela() + " WHERE id = ?", id);
    }
}
