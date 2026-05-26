package br.mil.sfpc.indicadores.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Mantém compatibilidade com URLs antigas ({@code /admin/...}, {@code /painel}).
 */
@Controller
public class RedirectController {

    @GetMapping("/admin/formularios")
    public String adminLista() {
        return "redirect:/formularios";
    }

    @GetMapping("/admin/formularios/novo")
    public String adminNovo() {
        return "redirect:/formularios/novo";
    }

    @GetMapping("/admin/formularios/{slug}/editar")
    public String adminEditar(@PathVariable String slug) {
        return "redirect:/formularios/" + slug + "/editar";
    }

    @GetMapping("/painel")
    public String painelAntigo() {
        return "redirect:/";
    }
}
