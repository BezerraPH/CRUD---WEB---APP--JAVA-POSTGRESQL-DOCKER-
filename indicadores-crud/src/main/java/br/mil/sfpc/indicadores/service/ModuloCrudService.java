package br.mil.sfpc.indicadores.service;

import br.mil.sfpc.indicadores.config.CampoConfig;
import br.mil.sfpc.indicadores.config.ModuloConfig;
import br.mil.sfpc.indicadores.service.calculo.CalculoService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import br.mil.sfpc.indicadores.service.calculo.CalculoConfig;
import br.mil.sfpc.indicadores.service.calculo.CalculoRegra;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Regras de negócio do CRUD: validação, conversão e persistência.
 */
@Service
public class ModuloCrudService {

    private final ModuloRepository repository;
    private final CalculoService calculoService;

    public ModuloCrudService(ModuloRepository repository, CalculoService calculoService) {
        this.repository = repository;
        this.calculoService = calculoService;
    }

    public Map<String, Object> montarValoresFormulario(ModuloConfig modulo, Map<String, String> form) {
        Set<String> calculados = camposCalculadosAtivos(modulo);
        Map<String, Object> valores = new LinkedHashMap<>();
        for (CampoConfig campo : modulo.todosCampos()) {
            if ("id".equals(campo.getNome())) {
                continue;
            }
            if (campo.isOcultoFormulario() || calculados.contains(campo.getNome())) {
                continue;
            }
            String raw = form.get(campo.getNome());
            valores.put(campo.getNome(), converter(campo, raw));
        }
        return valores;
    }

    public void salvar(ModuloConfig modulo, Long id, Map<String, String> form) {
        validarCampos(modulo, form);
        boolean novo = id == null;
        Map<String, Object> valores = montarValoresFormulario(modulo, form);

        Optional<Map<String, Object>> ultimo = novo ? repository.ultimoRegistro(modulo) : Optional.empty();
        calculoService.aplicar(modulo, valores, ultimo, novo);

        if (novo) {
            repository.inserir(modulo, valores);
        } else {
            repository.atualizar(modulo, id, valores);
        }
    }

    private Set<String> camposCalculadosAtivos(ModuloConfig modulo) {
        CalculoConfig cfg = calculoService.obterConfig(modulo);
        if (!cfg.isCalculosAtivos()) {
            return Set.of();
        }
        Set<String> destinos = new HashSet<>();
        for (CalculoRegra regra : cfg.getRegras()) {
            if (regra.ativo()) {
                destinos.add(regra.campoDestino());
            }
        }
        return destinos;
    }

    private void validarCampos(ModuloConfig modulo, Map<String, String> form) {
        Set<String> calculados = camposCalculadosAtivos(modulo);
        for (CampoConfig campo : modulo.todosCampos()) {
            if ("id".equals(campo.getNome()) || campo.isOcultoFormulario() || calculados.contains(campo.getNome())) {
                continue;
            }
            if (!campo.isObrigatorio()) {
                continue;
            }
            String raw = form.get(campo.getNome());
            if (raw == null || raw.isBlank()) {
                throw new IllegalArgumentException("Preencha o campo obrigatório: " + campo.getLabel());
            }
        }
    }

    private Object converter(CampoConfig campo, String raw) {
        if (raw == null || raw.isBlank()) {
            if ("number".equals(campo.getTipo())) {
                return BigDecimal.ZERO;
            }
            if ("date".equals(campo.getTipo())) {
                return null;
            }
            if (campo.getValorPadrao() != null) {
                return campo.getValorPadrao();
            }
            return "";
        }
        try {
            return switch (campo.getTipo()) {
                case "number" -> new BigDecimal(raw.trim().replace(',', '.'));
                case "date" -> Date.valueOf(LocalDate.parse(raw.trim()));
                default -> raw.trim();
            };
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor numérico inválido em \"" + campo.getLabel() + "\".");
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data inválida em \"" + campo.getLabel() + "\". Use o formato AAAA-MM-DD.");
        }
    }
}
