package br.mil.sfpc.indicadores.web;

import br.mil.sfpc.indicadores.config.ModuloConfig;
import br.mil.sfpc.indicadores.service.ModuloRegistry;
import br.mil.sfpc.indicadores.service.ModuloCrudService;
import br.mil.sfpc.indicadores.service.ModuloRepository;
import br.mil.sfpc.indicadores.service.calculo.CalculoRegra;
import br.mil.sfpc.indicadores.service.calculo.CalculoService;
import br.mil.sfpc.indicadores.service.PceProcessador;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Controla a tela inicial e o preenchimento de dados (CRUD) de cada módulo/formulário.
 */
@Controller
public class ModuloController {

    private static final Logger log = LoggerFactory.getLogger(ModuloController.class);

    private final ModuloRegistry modulos;
    private final ModuloRepository repository;
    private final ModuloCrudService crudService;
    private final CalculoService calculoService;

    public ModuloController(ModuloRegistry modulos, ModuloRepository repository,
                            ModuloCrudService crudService, CalculoService calculoService) {
        this.modulos = modulos;
        this.repository = repository;
        this.crudService = crudService;
        this.calculoService = calculoService;
    }

    @GetMapping({"/", "/inicio"})
    public String inicio(Model model) {
        model.addAttribute("pageTitle", "Início");
        model.addAttribute("paginaAtiva", "inicio");
        model.addAttribute("showNavFormularios", false);
        return "painel";
    }

    @GetMapping("/m/{slug}")
    public String listar(@PathVariable String slug, Model model) {
        ModuloConfig modulo = requireModulo(slug);
        model.addAttribute("pageTitle", modulo.getTitulo());
        model.addAttribute("paginaAtiva", "lancamento");
        model.addAttribute("modulo", modulo);
        model.addAttribute("registros", repository.listar(modulo));
        model.addAttribute("editReg", null);
        popularCalculosNoModel(modulo, model);
        if (PceProcessador.ID.equals(modulo.getProcessador())) {
            repository.ultimoRegistro(modulo).ifPresent(u -> model.addAttribute("ultimoRegistro", u));
        }
        return "modulo-crud";
    }

    @GetMapping("/m/{slug}/editar/{id}")
    public String editar(@PathVariable String slug, @PathVariable long id, Model model) {
        ModuloConfig modulo = requireModulo(slug);
        Map<String, Object> editReg = repository.buscarPorId(modulo, id)
                .orElseThrow(() -> new NegocioException("Registro não encontrado.", "/m/" + slug));
        model.addAttribute("pageTitle", modulo.getTitulo());
        model.addAttribute("paginaAtiva", "lancamento");
        model.addAttribute("modulo", modulo);
        model.addAttribute("registros", repository.listar(modulo));
        model.addAttribute("editReg", editReg);
        popularCalculosNoModel(modulo, model);
        if (PceProcessador.ID.equals(modulo.getProcessador())) {
            repository.ultimoRegistro(modulo).ifPresent(u -> model.addAttribute("ultimoRegistro", u));
        }
        return "modulo-crud";
    }

    @PostMapping("/m/{slug}/salvar")
    public String salvar(@PathVariable String slug,
                         @RequestParam Map<String, String> form,
                         RedirectAttributes redirect) {
        ModuloConfig modulo = requireModulo(slug);
        try {
            Long idInformado = parseId(form.get("id"));
            Long idGravar = null;
            if (idInformado != null) {
                if (repository.buscarPorId(modulo, idInformado).isEmpty()) {
                    throw new NegocioException("Registro não encontrado para edição.", "/m/" + slug);
                }
                idGravar = idInformado;
            }
            crudService.salvar(modulo, idGravar, form);
            redirect.addFlashAttribute("sucesso", "Registro gravado com sucesso.");
        } catch (NegocioException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao gravar módulo {}", slug, e);
            redirect.addFlashAttribute("erro", "Não foi possível gravar. Verifique os campos e tente novamente.");
        }
        return "redirect:/m/" + slug;
    }

    @PostMapping({"/m/{slug}/excluir/{id}", "/m/{slug}/deletar/{id}"})
    public String excluir(@PathVariable String slug, @PathVariable long id, RedirectAttributes redirect) {
        ModuloConfig modulo = requireModulo(slug);
        try {
            if (repository.buscarPorId(modulo, id).isEmpty()) {
                throw new NegocioException("Registro não encontrado.", "/m/" + slug);
            }
            repository.excluir(modulo, id);
            redirect.addFlashAttribute("sucesso", "Registro excluído.");
        } catch (NegocioException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao excluir registro {} do módulo {}", id, slug, e);
            redirect.addFlashAttribute("erro", "Não foi possível excluir o registro.");
        }
        return "redirect:/m/" + slug;
    }

    /** Compatibilidade: links GET antigos não excluem mais (evita exclusão acidental). */
    @GetMapping({"/m/{slug}/excluir/{id}", "/m/{slug}/deletar/{id}"})
    public String excluirGetDesativado(@PathVariable String slug, RedirectAttributes redirect) {
        redirect.addFlashAttribute("erro", "Confirme a exclusão pelo botão na tabela (ação segura).");
        return "redirect:/m/" + slug;
    }

    private void popularCalculosNoModel(ModuloConfig modulo, Model model) {
        var cfg = calculoService.obterConfig(modulo);
        model.addAttribute("suportaCalculos", true);
        model.addAttribute("calculosAtivos", cfg.isCalculosAtivos());
        Set<String> calculados = new LinkedHashSet<>();
        if (cfg.isCalculosAtivos()) {
            cfg.getRegras().stream()
                    .filter(CalculoRegra::ativo)
                    .forEach(r -> calculados.add(r.campoDestino()));
        }
        model.addAttribute("camposCalculados", calculados);
    }

    private ModuloConfig requireModulo(String slug) {
        return modulos.findBySlug(slug)
                .orElseThrow(() -> new NegocioException("Formulário não encontrado: " + slug, "/formularios"));
    }

    private Long parseId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Identificador do registro inválido.");
        }
    }
}
