package br.mil.sfpc.indicadores.service;

import br.mil.sfpc.indicadores.config.ModuloConfig;
import br.mil.sfpc.indicadores.config.ModuloProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Catálogo unificado de formulários/módulos: mescla definições do YAML ({@code modulos.yml})
 * com formulários criados pela interface (metadados em {@code formulario_def}).
 */
@Service
@Order(2)
public class ModuloRegistry implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ModuloRegistry.class);

    private final ModuloProperties yamlProperties;
    private final FormularioMetaRepository metaRepository;

    private volatile List<ModuloConfig> modulos = List.of();

    public ModuloRegistry(ModuloProperties yamlProperties, FormularioMetaRepository metaRepository) {
        this.yamlProperties = yamlProperties;
        this.metaRepository = metaRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        reload();
    }

    public synchronized void reload() {
        Map<String, ModuloConfig> porSlug = new LinkedHashMap<>();
        Set<String> suprimidos = Set.of();
        try {
            suprimidos = metaRepository.slugsSuprimidos();
        } catch (Exception e) {
            log.debug("Tabela formulario_suprimido indisponível: {}", e.getMessage());
        }
        for (ModuloConfig m : yamlProperties.getModulos()) {
            if (!suprimidos.contains(m.getSlug())) {
                porSlug.putIfAbsent(m.getSlug(), m);
            }
        }
        try {
            for (ModuloConfig m : metaRepository.carregarTodos()) {
                if (!suprimidos.contains(m.getSlug())) {
                    porSlug.put(m.getSlug(), m);
                }
            }
        } catch (Exception e) {
            log.warn("Formulários dinâmicos indisponíveis (metadados): {}", e.getMessage());
        }
        modulos = List.copyOf(porSlug.values());
    }

    public List<ModuloConfig> getModulos() {
        return modulos;
    }

    public Optional<ModuloConfig> findBySlug(String slug) {
        return modulos.stream().filter(m -> slug.equals(m.getSlug())).findFirst();
    }

    public boolean isFormularioDinamico(String slug) {
        return metaRepository.carregarPorSlug(slug).isPresent();
    }

    public List<ModuloConfig> modulosYaml() {
        return yamlProperties.getModulos();
    }

    public List<Map<String, Object>> formulariosDinamicosResumo() {
        try {
            return metaRepository.listarResumo();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
