package br.mil.sfpc.indicadores.web;

import br.mil.sfpc.indicadores.config.ModuloConfig;
import br.mil.sfpc.indicadores.service.ModuloRegistry;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/**
 * Atributos globais enviados a todas as páginas Thymeleaf (menu lateral, flags de layout).
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private final ModuloRegistry moduloRegistry;

    public GlobalModelAdvice(ModuloRegistry moduloRegistry) {
        this.moduloRegistry = moduloRegistry;
    }

    /** Exibe link "Formulários" no cabeçalho (desligado na tela inicial). */
    @ModelAttribute("showNavFormularios")
    public boolean showNavFormularios() {
        return true;
    }

    /** Lista de módulos para atalhos no menu lateral. */
    @ModelAttribute("menuModulos")
    public List<ModuloConfig> menuModulos() {
        return moduloRegistry.getModulos();
    }
}
