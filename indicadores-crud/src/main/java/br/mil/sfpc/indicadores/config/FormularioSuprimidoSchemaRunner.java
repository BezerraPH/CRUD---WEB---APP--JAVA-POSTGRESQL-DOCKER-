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

@Component
@Order(3)
public class FormularioSuprimidoSchemaRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FormularioSuprimidoSchemaRunner.class);

    private final JdbcTemplate jdbc;

    public FormularioSuprimidoSchemaRunner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Boolean existe = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_name = 'formulario_suprimido')",
                Boolean.class);
        if (Boolean.TRUE.equals(existe)) {
            return;
        }
        log.info("Aplicando script formulario_suprimido (04-formulario-suprimido.sql)...");
        ClassPathResource script = new ClassPathResource("db/04-formulario-suprimido.sql");
        try (Connection connection = Objects.requireNonNull(jdbc.getDataSource()).getConnection()) {
            ScriptUtils.executeSqlScript(connection, new EncodedResource(script, StandardCharsets.UTF_8));
        }
    }
}
