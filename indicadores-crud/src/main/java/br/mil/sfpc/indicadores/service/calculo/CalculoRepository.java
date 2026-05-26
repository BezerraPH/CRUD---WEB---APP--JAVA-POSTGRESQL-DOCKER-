package br.mil.sfpc.indicadores.service.calculo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class CalculoRepository {

    private final JdbcTemplate jdbc;

    public CalculoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean existeTabelas() {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_name = 'formulario_calculo_config'",
                Integer.class);
        return n != null && n > 0;
    }

    public Optional<CalculoConfig> carregar(String slug) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM formulario_calculo_config WHERE slug = ?", slug);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        CalculoConfig cfg = mapConfig(rows.get(0));
        cfg.setRegras(carregarRegras(slug));
        return Optional.of(cfg);
    }

    public CalculoConfig carregarOuPadrao(String slug, CalculoConfig padrao) {
        return carregar(slug).orElse(padrao);
    }

    @Transactional
    public void salvar(CalculoConfig config) {
        jdbc.update(
                "INSERT INTO formulario_calculo_config (slug, calculos_ativos, herdar_saldo_anterior, atualizado_em) "
                        + "VALUES (?, ?, ?, CURRENT_TIMESTAMP) "
                        + "ON CONFLICT (slug) DO UPDATE SET calculos_ativos = EXCLUDED.calculos_ativos, "
                        + "herdar_saldo_anterior = EXCLUDED.herdar_saldo_anterior, atualizado_em = CURRENT_TIMESTAMP",
                config.getSlug(), config.isCalculosAtivos(), config.isHerdarSaldoAnterior());

        jdbc.update("DELETE FROM formulario_calculo_regra WHERE slug = ?", config.getSlug());
        int ordem = 0;
        for (CalculoRegra regra : config.getRegras()) {
            jdbc.update(
                    "INSERT INTO formulario_calculo_regra (slug, campo_destino, label, expressao, ativo, ordem) "
                            + "VALUES (?, ?, ?, ?, ?, ?)",
                    config.getSlug(),
                    regra.campoDestino(),
                    regra.label() != null ? regra.label() : "",
                    regra.expressao(),
                    regra.ativo(),
                    ordem++);
        }
    }

    private List<CalculoRegra> carregarRegras(String slug) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM formulario_calculo_regra WHERE slug = ? ORDER BY ordem, id", slug);
        List<CalculoRegra> regras = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            regras.add(new CalculoRegra(
                    ((Number) row.get("id")).longValue(),
                    (String) row.get("campo_destino"),
                    (String) row.get("label"),
                    (String) row.get("expressao"),
                    Boolean.TRUE.equals(row.get("ativo")),
                    row.get("ordem") != null ? ((Number) row.get("ordem")).intValue() : 0
            ));
        }
        return regras;
    }

    private CalculoConfig mapConfig(Map<String, Object> row) {
        CalculoConfig cfg = new CalculoConfig();
        cfg.setSlug((String) row.get("slug"));
        cfg.setCalculosAtivos(Boolean.TRUE.equals(row.get("calculos_ativos")));
        cfg.setHerdarSaldoAnterior(Boolean.TRUE.equals(row.get("herdar_saldo_anterior")));
        return cfg;
    }
}
