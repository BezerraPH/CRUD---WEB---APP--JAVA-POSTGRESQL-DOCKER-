package br.mil.sfpc.indicadores.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.Objects;

/**
 * Garante que as tabelas de metadados do construtor existam (script único versionado).
 */
@Component
@Order(1)
public class DatabaseSchemaRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSchemaRunner.class);

    private final JdbcTemplate jdbc;

    public DatabaseSchemaRunner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (metadadosCompletos()) {
            return;
        }

        log.info("Aplicando script de metadados de formulários (02-formularios-meta.sql)...");
        ClassPathResource script = new ClassPathResource("db/02-formularios-meta.sql");
        try (Connection connection = Objects.requireNonNull(jdbc.getDataSource()).getConnection()) {
            ScriptUtils.executeSqlScript(connection, new EncodedResource(script, StandardCharsets.UTF_8));
        }
        log.info("Metadados de formulários criados com sucesso.");
    }

    private boolean metadadosCompletos() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_name IN "
                        + "('formulario_def','formulario_grupo','formulario_campo','formulario_listagem')",
                Integer.class);
        return count != null && count == 4;
    }
}
