package br.mil.sfpc.indicadores.service.calculo;

import java.util.ArrayList;
import java.util.List;

/** Configuração de cálculos automáticos de um módulo/formulário. */
public class CalculoConfig {

    private String slug;
    private boolean calculosAtivos = true;
    private boolean herdarSaldoAnterior = true;
    private List<CalculoRegra> regras = new ArrayList<>();

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public boolean isCalculosAtivos() {
        return calculosAtivos;
    }

    public void setCalculosAtivos(boolean calculosAtivos) {
        this.calculosAtivos = calculosAtivos;
    }

    public boolean isHerdarSaldoAnterior() {
        return herdarSaldoAnterior;
    }

    public void setHerdarSaldoAnterior(boolean herdarSaldoAnterior) {
        this.herdarSaldoAnterior = herdarSaldoAnterior;
    }

    public List<CalculoRegra> getRegras() {
        return regras;
    }

    public void setRegras(List<CalculoRegra> regras) {
        this.regras = regras != null ? regras : new ArrayList<>();
    }
}
