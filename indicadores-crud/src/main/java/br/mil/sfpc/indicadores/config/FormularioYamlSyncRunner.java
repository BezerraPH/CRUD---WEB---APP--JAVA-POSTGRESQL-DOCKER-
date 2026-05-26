package br.mil.sfpc.indicadores.config;

import br.mil.sfpc.indicadores.service.FormularioYamlSyncService;
import br.mil.sfpc.indicadores.service.ModuloRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Importa metadados do YAML para o banco na subida da aplicação. */
@Component
@Order(4)
public class FormularioYamlSyncRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FormularioYamlSyncRunner.class);

    private final FormularioYamlSyncService yamlSync;
    private final ModuloRegistry moduloRegistry;

    public FormularioYamlSyncRunner(FormularioYamlSyncService yamlSync, ModuloRegistry moduloRegistry) {
        this.yamlSync = yamlSync;
        this.moduloRegistry = moduloRegistry;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            yamlSync.sincronizarTodosDoYaml();
            moduloRegistry.reload();
            log.info("Metadados dos formulários YAML sincronizados com o banco.");
        } catch (Exception e) {
            log.warn("Sincronização YAML→banco indisponível: {}", e.getMessage());
        }
    }
}
