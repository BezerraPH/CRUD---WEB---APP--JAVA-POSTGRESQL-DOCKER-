package br.mil.sfpc.indicadores.web;

import br.mil.sfpc.indicadores.config.ModuloConfig;
import br.mil.sfpc.indicadores.service.ModuloRegistry;
import br.mil.sfpc.indicadores.service.calculo.CalculoConfig;
import br.mil.sfpc.indicadores.service.calculo.CalculoRepository;
import br.mil.sfpc.indicadores.service.calculo.CalculoRegra;
import br.mil.sfpc.indicadores.service.calculo.CalculoService;
import br.mil.sfpc.indicadores.service.calculo.FormulaEvaluator;
import br.mil.sfpc.indicadores.service.calculo.PceCalculoDefaults;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tela para editar fórmulas de cálculo automático (ex.: saldos PCE).
 */
@Controller
@RequestMapping("/formularios/calculos")
public class CalculoAdminController {

    private final ModuloRegistry moduloRegistry;
    private final CalculoService calculoService;
    private final CalculoRepository calculoRepository;
    private final FormulaEvaluator formulaEvaluator;

    public CalculoAdminController(ModuloRegistry moduloRegistry,
                                  CalculoService calculoService,
                                  CalculoRepository calculoRepository,
                                  FormulaEvaluator formulaEvaluator) {
        this.moduloRegistry = moduloRegistry;
        this.calculoService = calculoService;
        this.calculoRepository = calculoRepository;
        this.formulaEvaluator = formulaEvaluator;
    }

    @GetMapping("/{slug}")
    public String editar(@PathVariable String slug, Model model) {
        ModuloConfig modulo = requireModulo(slug);
        CalculoConfig config = calculoService.obterConfig(modulo);
        model.addAttribute("pageTitle", "Cálculos — " + modulo.getTitulo());
        model.addAttribute("paginaAtiva", "formularios");
        model.addAttribute("modulo", modulo);
        model.addAttribute("config", config);
        model.addAttribute("camposDisponiveis", modulo.todosCampos().stream()
                .filter(c -> "number".equals(c.getTipo()))
                .toList());
        model.addAttribute("temPadraoPce", PceCalculoDefaults.SLUG_PCE.equals(modulo.getSlug())
                || "pce".equals(modulo.getProcessador()));
        return "formularios/calculos";
    }

    @PostMapping("/{slug}/salvar")
    public String salvar(@PathVariable String slug,
                         @RequestParam(name = "calculosAtivos", required = false) String calculosAtivos,
                         @RequestParam(name = "herdarSaldoAnterior", required = false) String herdarSaldoAnterior,
                         @RequestParam(required = false) List<String> campoDestino,
                         @RequestParam(required = false) List<String> label,
                         @RequestParam(required = false) List<String> expressao,
                         @RequestParam(required = false) List<String> ativo,
                         RedirectAttributes redirect) {
        ModuloConfig modulo = requireModulo(slug);
        try {
            CalculoConfig config = new CalculoConfig();
            config.setSlug(slug);
            config.setCalculosAtivos("true".equalsIgnoreCase(calculosAtivos));
            config.setHerdarSaldoAnterior("true".equalsIgnoreCase(herdarSaldoAnterior));
            config.setRegras(montarRegras(campoDestino, label, expressao, ativo, modulo));
            validarRegras(config, modulo);
            calculoRepository.salvar(config);
            redirect.addFlashAttribute("sucesso", "Fórmulas salvas com sucesso.");
        } catch (Exception e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/formularios/calculos/" + slug;
    }

    @PostMapping("/{slug}/restaurar-padrao")
    public String restaurarPadrao(@PathVariable String slug, RedirectAttributes redirect) {
        ModuloConfig modulo = requireModulo(slug);
        if (PceCalculoDefaults.SLUG_PCE.equals(modulo.getSlug())
                || "pce".equals(modulo.getProcessador())) {
            CalculoConfig padrao = PceCalculoDefaults.configPadrao();
            padrao.setSlug(modulo.getSlug());
            calculoRepository.salvar(padrao);
            redirect.addFlashAttribute("sucesso", "Fórmulas padrão do PCE restauradas.");
        } else {
            redirect.addFlashAttribute("erro", "Não há fórmulas padrão para este formulário.");
        }
        return "redirect:/formularios/calculos/" + slug;
    }

    private ModuloConfig requireModulo(String slug) {
        return moduloRegistry.findBySlug(slug)
                .orElseThrow(() -> new NegocioException("Formulário não encontrado.", "/formularios"));
    }

    private List<CalculoRegra> montarRegras(List<String> campos, List<String> labels,
                                            List<String> expressoes, List<String> ativos,
                                            ModuloConfig modulo) {
        if (campos == null || campos.isEmpty()) {
            return List.of();
        }
        Set<String> ativoSet = ativos != null ? new HashSet<>(ativos) : Set.of();
        List<CalculoRegra> regras = new ArrayList<>();
        for (int i = 0; i < campos.size(); i++) {
            String dest = campos.get(i);
            if (dest == null || dest.isBlank()) {
                continue;
            }
            String expr = expressoes != null && i < expressoes.size() ? expressoes.get(i) : "";
            String lbl = labels != null && i < labels.size() ? labels.get(i) : dest;
            boolean ativo = ativoSet.contains(String.valueOf(i));
            regras.add(new CalculoRegra(null, dest.trim(), lbl.trim(), expr.trim(), ativo, i));
        }
        return regras;
    }

    private void validarRegras(CalculoConfig config, ModuloConfig modulo) {
        Set<String> campos = new HashSet<>();
        modulo.todosCampos().stream()
                .filter(c -> "number".equals(c.getTipo()))
                .forEach(c -> campos.add(c.getNome()));
        Map<String, BigDecimal> zeros = new LinkedHashMap<>();
        campos.forEach(c -> zeros.put(c, BigDecimal.ZERO));
        for (CalculoRegra regra : config.getRegras()) {
            if (!regra.ativo() || regra.expressao().isBlank()) {
                continue;
            }
            formulaEvaluator.avaliar(regra.expressao(), zeros, campos);
        }
    }
}
