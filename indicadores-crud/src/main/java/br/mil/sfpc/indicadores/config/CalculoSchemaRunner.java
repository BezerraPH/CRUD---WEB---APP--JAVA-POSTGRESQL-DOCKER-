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

/** Cria tabelas de configuração de cálculos automáticos, se necessário. */
@Component
@Order(2)
public class CalculoSchemaRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CalculoSchemaRunner.class);

    private final JdbcTemplate jdbc;

    public CalculoSchemaRunner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Boolean existe = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_name = 'formulario_calculo_config')",
                Boolean.class);
        if (Boolean.TRUE.equals(existe)) {
            return;
        }
        log.info("Aplicando script de cálculos (03-calculos.sql)...");
        ClassPathResource script = new ClassPathResource("db/03-calculos.sql");
        try (Connection connection = Objects.requireNonNull(jdbc.getDataSource()).getConnection()) {
            ScriptUtils.executeSqlScript(connection, new EncodedResource(script, StandardCharsets.UTF_8));
        }
        log.info("Tabelas de cálculos criadas.");
    }
}
