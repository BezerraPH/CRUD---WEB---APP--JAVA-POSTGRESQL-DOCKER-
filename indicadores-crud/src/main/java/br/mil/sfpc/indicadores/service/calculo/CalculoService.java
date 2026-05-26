package br.mil.sfpc.indicadores.service.calculo;

import br.mil.sfpc.indicadores.config.CampoConfig;
import br.mil.sfpc.indicadores.config.ModuloConfig;
import br.mil.sfpc.indicadores.service.PceProcessador;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Aplica cálculos configuráveis antes de gravar um registro.
 */
@Service
public class CalculoService {

    private final CalculoRepository calculoRepository;
    private final FormulaEvaluator formulaEvaluator;
    private final PceProcessador pceProcessador;

    public CalculoService(CalculoRepository calculoRepository,
                        FormulaEvaluator formulaEvaluator,
                        PceProcessador pceProcessador) {
        this.calculoRepository = calculoRepository;
        this.formulaEvaluator = formulaEvaluator;
        this.pceProcessador = pceProcessador;
    }

    /** Qualquer formulário pode ter fórmulas (+, -, *, /) configuradas pela interface. */
    public boolean suportaCalculos(ModuloConfig modulo) {
        return modulo != null && modulo.getSlug() != null && !modulo.getSlug().isBlank();
    }

    public CalculoConfig obterConfig(ModuloConfig modulo) {
        CalculoConfig padrao = padraoParaModulo(modulo);
        if (!calculoRepository.existeTabelas()) {
            return padrao;
        }
        return calculoRepository.carregarOuPadrao(modulo.getSlug(), padrao);
    }

    public void aplicar(ModuloConfig modulo, Map<String, Object> valores,
                        Optional<Map<String, Object>> ultimo, boolean novo) {
        if (!suportaCalculos(modulo)) {
            return;
        }
        CalculoConfig config = obterConfig(modulo);
        if (!config.isCalculosAtivos()) {
            return;
        }

        if (config.getRegras().isEmpty() && PceProcessador.ID.equals(modulo.getProcessador())) {
            pceProcessador.aplicarAntesDeGravar(valores, ultimo, novo);
            return;
        }

        if (config.isHerdarSaldoAnterior() && novo && ultimo.isPresent()) {
            aplicarHerancaSaldoAnterior(valores, ultimo.get(), modulo.getSlug());
        }

        Set<String> campos = camposNumericosDoModulo(modulo, valores);
        for (CalculoRegra regra : config.getRegras()) {
            if (!regra.ativo()) {
                continue;
            }
            Map<String, BigDecimal> vars = paraBigDecimal(valores, campos);
            BigDecimal resultado = formulaEvaluator.avaliar(regra.expressao(), vars, campos);
            valores.put(regra.campoDestino(), resultado);
        }
    }

    private Set<String> camposNumericosDoModulo(ModuloConfig modulo, Map<String, Object> valores) {
        Set<String> campos = nomesCamposNumericos(valores);
        for (CampoConfig c : modulo.todosCampos()) {
            if ("number".equals(c.getTipo())) {
                campos.add(c.getNome());
            }
        }
        return campos;
    }

    private void aplicarHerancaSaldoAnterior(Map<String, Object> valores, Map<String, Object> ultimo, String slug) {
        if (PceCalculoDefaults.SLUG_PCE.equals(slug)) {
            herdarSeVazio(valores, ultimo, "saldoanosanteriorarmas", "saldoatualdearmas");
            herdarSeVazio(valores, ultimo, "saldoanosanteriormunicao", "saldoatualdemunicoes");
            herdarSeVazio(valores, ultimo, "saldoanosanterioroutrospce", "saldoatualoutrospce");
            herdarSeVazio(valores, ultimo, "saldoanosanteriorcarregadores", "saldoatualcorregadores");
        }
    }

    private void herdarSeVazio(Map<String, Object> valores, Map<String, Object> ultimo,
                               String campoDestino, String campoOrigem) {
        Object atual = valores.get(campoDestino);
        if (atual == null || (atual instanceof String s && s.isBlank())) {
            valores.put(campoDestino, ultimo.get(campoOrigem));
        }
    }

    private CalculoConfig padraoParaModulo(ModuloConfig modulo) {
        if (PceProcessador.ID.equals(modulo.getProcessador())
                || PceCalculoDefaults.SLUG_PCE.equals(modulo.getSlug())) {
            CalculoConfig cfg = PceCalculoDefaults.configPadrao();
            cfg.setSlug(modulo.getSlug());
            return cfg;
        }
        CalculoConfig cfg = new CalculoConfig();
        cfg.setSlug(modulo.getSlug());
        cfg.setCalculosAtivos(false);
        return cfg;
    }

    private Set<String> nomesCamposNumericos(Map<String, Object> valores) {
        return valores.keySet().stream()
                .filter(k -> valores.get(k) instanceof Number || valores.get(k) == null
                        || (valores.get(k) instanceof String s && (s.isBlank() || s.matches("-?\\d+([.,]\\d+)?"))))
                .collect(Collectors.toCollection(HashSet::new));
    }

    private Map<String, BigDecimal> paraBigDecimal(Map<String, Object> valores, Set<String> campos) {
        Map<String, BigDecimal> map = new java.util.LinkedHashMap<>();
        for (String nome : campos) {
            map.put(nome, paraNumero(valores.get(nome)));
        }
        return map;
    }

    private BigDecimal paraNumero(Object valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        if (valor instanceof BigDecimal bd) {
            return bd;
        }
        if (valor instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        String s = valor.toString().trim();
        if (s.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(s.replace(',', '.'));
    }
}
