package br.mil.sfpc.indicadores.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Carrega a lista {@code app.modulos} do arquivo {@code modulos.yml}. */
@ConfigurationProperties(prefix = "app")
public class ModuloProperties {

    private List<ModuloConfig> modulos = new ArrayList<>();

    public List<ModuloConfig> getModulos() {
        return modulos;
    }

    public void setModulos(List<ModuloConfig> modulos) {
        this.modulos = modulos != null ? modulos : new ArrayList<>();
    }

    public Optional<ModuloConfig> findBySlug(String slug) {
        return modulos.stream().filter(m -> slug.equals(m.getSlug())).findFirst();
    }
}
