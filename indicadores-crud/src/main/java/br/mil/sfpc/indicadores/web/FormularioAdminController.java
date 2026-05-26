package br.mil.sfpc.indicadores.web;

import br.mil.sfpc.indicadores.config.ModuloConfig;
import br.mil.sfpc.indicadores.service.FormularioBuilderService;
import br.mil.sfpc.indicadores.service.FormularioBuilderService.CampoDefinicao;
import br.mil.sfpc.indicadores.service.FormularioMetaRepository;
import br.mil.sfpc.indicadores.service.FormularioYamlSyncService;
import br.mil.sfpc.indicadores.service.ModuloRegistry;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Construtor de formulários: listar, criar, editar metadados, adicionar campos e excluir módulos dinâmicos.
 */
@Controller
@RequestMapping("/formularios")
public class FormularioAdminController {

    private final ModuloRegistry moduloRegistry;
    private final FormularioBuilderService builderService;
    private final FormularioMetaRepository metaRepository;
    private final FormularioYamlSyncService yamlSync;

    public FormularioAdminController(ModuloRegistry moduloRegistry,
                                     FormularioBuilderService builderService,
                                     FormularioMetaRepository metaRepository,
                                     FormularioYamlSyncService yamlSync) {
        this.moduloRegistry = moduloRegistry;
        this.builderService = builderService;
        this.metaRepository = metaRepository;
        this.yamlSync = yamlSync;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("pageTitle", "Formulários");
        model.addAttribute("paginaAtiva", "formularios");
        model.addAttribute("formularios", moduloRegistry.getModulos());
        return "formularios/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("pageTitle", "Novo formulário");
        model.addAttribute("paginaAtiva", "novo");
        return "formularios/novo";
    }

    @PostMapping("/criar")
    public String criar(@RequestParam String titulo,
                        @RequestParam(required = false) String descricao,
                        @RequestParam(required = false) String slug,
                        @RequestParam(required = false) String tabela,
                        @RequestParam(required = false) String colunaOrdem,
                        @RequestParam List<String> campoLabel,
                        @RequestParam List<String> campoTipo,
                        @RequestParam(required = false) List<String> campoNome,
                        @RequestParam(required = false) List<String> campoObrigatorio,
                        RedirectAttributes redirect) {
        try {
            List<CampoDefinicao> campos = montarCampos(campoLabel, campoTipo, campoNome, campoObrigatorio);
            String slugCriado = builderService.criarFormulario(titulo, descricao, slug, tabela, colunaOrdem, campos);
            redirect.addFlashAttribute("sucesso", "Formulário criado com sucesso! Pode começar a preencher os dados.");
            return "redirect:/m/" + slugCriado;
        } catch (Exception e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/formularios/novo";
        }
    }

    @GetMapping("/{slug}/editar")
    public String editar(@PathVariable String slug, Model model) {
        yamlSync.garantirMetadadosNoBanco(slug);
        moduloRegistry.reload();
        ModuloConfig modulo = metaRepository.carregarPorSlug(slug)
                .orElseThrow(() -> new NegocioException("Formulário não encontrado.", "/formularios"));
        model.addAttribute("pageTitle", "Editar formulário");
        model.addAttribute("paginaAtiva", "formularios");
        model.addAttribute("modulo", modulo);
        return "formularios/editar";
    }

    @PostMapping("/{slug}/atualizar")
    public String atualizar(@PathVariable String slug,
                            @RequestParam String titulo,
                            @RequestParam(required = false) String descricao,
                            RedirectAttributes redirect) {
        try {
            metaRepository.atualizarMetadados(slug, titulo, descricao);
            moduloRegistry.reload();
            redirect.addFlashAttribute("sucesso", "Alterações salvas com sucesso.");
        } catch (Exception e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/formularios/" + slug + "/editar";
    }

    @PostMapping("/{slug}/adicionar-campo")
    public String adicionarCampo(@PathVariable String slug,
                                 @RequestParam String label,
                                 @RequestParam String tipo,
                                 @RequestParam(required = false) String nome,
                                 @RequestParam(defaultValue = "false") boolean obrigatorio,
                                 RedirectAttributes redirect) {
        try {
            builderService.adicionarCampo(slug, label, tipo, obrigatorio, nome);
            redirect.addFlashAttribute("sucesso", "Campo adicionado ao formulário.");
        } catch (Exception e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/formularios/" + slug + "/editar";
    }

    @PostMapping("/{slug}/excluir")
    public String excluir(@PathVariable String slug, RedirectAttributes redirect) {
        try {
            builderService.excluirFormulario(slug);
            redirect.addFlashAttribute("sucesso", "Formulário excluído.");
            return "redirect:/formularios";
        } catch (Exception e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/formularios/" + slug + "/editar";
        }
    }

    private List<CampoDefinicao> montarCampos(List<String> labels, List<String> tipos,
                                              List<String> nomes, List<String> obrigatorios) {
        Set<Integer> obrIndices = new HashSet<>();
        if (obrigatorios != null) {
            for (String o : obrigatorios) {
                try {
                    obrIndices.add(Integer.parseInt(o));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        List<CampoDefinicao> campos = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++) {
            String label = labels.get(i);
            if (label == null || label.isBlank()) {
                continue;
            }
            String tipo = i < tipos.size() ? tipos.get(i) : "number";
            String nome = nomes != null && i < nomes.size() ? nomes.get(i) : "";
            boolean obr = obrIndices.contains(i);
            campos.add(new CampoDefinicao(label.trim(), tipo, obr, nome != null ? nome.trim() : ""));
        }
        return campos;
    }
}
