package br.mil.sfpc.indicadores.service.calculo;

import java.util.ArrayList;
import java.util.List;

/** Fórmulas padrão do módulo PCE (Controle de PCE para Destruição). */
public final class PceCalculoDefaults {

    public static final String SLUG_PCE = "pce_controle_destruicao";

    private PceCalculoDefaults() {
    }

    public static CalculoConfig configPadrao() {
        CalculoConfig cfg = new CalculoConfig();
        cfg.setSlug(SLUG_PCE);
        cfg.setCalculosAtivos(true);
        cfg.setHerdarSaldoAnterior(true);
        cfg.setRegras(regrasPadrao());
        return cfg;
    }

    public static List<CalculoRegra> regrasPadrao() {
        List<CalculoRegra> regras = new ArrayList<>();
        int ordem = 0;
        regras.add(new CalculoRegra(null, "saldoatualdearmas", "Saldo armas",
                "pcerec_armas + saldoanosanteriorarmas - armasdestruidas - armasdajusticasemordemdedestruicao - armasbrasonadas - armasrestituidas",
                true, ordem++));
        regras.add(new CalculoRegra(null, "saldoatualdemunicoes", "Saldo munições",
                "pcerec_municao + saldoanosanteriormunicao - municaodestruidas",
                true, ordem++));
        regras.add(new CalculoRegra(null, "saldoatualoutrospce", "Saldo outros PCE",
                "pcerec_outrospce + saldoanosanterioroutrospce - outrospcedestruidos",
                true, ordem++));
        regras.add(new CalculoRegra(null, "saldoatualcorregadores", "Saldo carregadores",
                "pcerec_carregadores + saldoanosanteriorcarregadores - carregadoresdestruidos",
                true, ordem));
        return regras;
    }
}
