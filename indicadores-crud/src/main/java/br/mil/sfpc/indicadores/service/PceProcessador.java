package br.mil.sfpc.indicadores.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * Processador do módulo PCE (controle de destruição): calcula saldos atuais
 * a partir dos saldos anteriores, recebimentos e quantidades destruídas.
 */
@Component
public class PceProcessador {

    /** Identificador usado em {@code modulos.yml} e {@code formulario_def.processador}. */
    public static final String ID = "pce";

    public void aplicarAntesDeGravar(Map<String, Object> valores, Optional<Map<String, Object>> ultimo, boolean novo) {
        if (novo && ultimo.isPresent()) {
            preencherSaldoAnteriorSeVazio(valores, ultimo.get(), "saldoanosanteriorarmas", "saldoatualdearmas");
            preencherSaldoAnteriorSeVazio(valores, ultimo.get(), "saldoanosanteriormunicao", "saldoatualdemunicoes");
            preencherSaldoAnteriorSeVazio(valores, ultimo.get(), "saldoanosanterioroutrospce", "saldoatualoutrospce");
            preencherSaldoAnteriorSeVazio(valores, ultimo.get(), "saldoanosanteriorcarregadores", "saldoatualcorregadores");
        }

        BigDecimal pcerecArmas = numero(valores.get("pcerec_armas"));
        BigDecimal pcerecMunicao = numero(valores.get("pcerec_municao"));
        BigDecimal pcerecOutros = numero(valores.get("pcerec_outrospce"));
        BigDecimal pcerecCarreg = numero(valores.get("pcerec_carregadores"));

        BigDecimal saldoAntArmas = numero(valores.get("saldoanosanteriorarmas"));
        BigDecimal saldoAntMunicao = numero(valores.get("saldoanosanteriormunicao"));
        BigDecimal saldoAntOutros = numero(valores.get("saldoanosanterioroutrospce"));
        BigDecimal saldoAntCarreg = numero(valores.get("saldoanosanteriorcarregadores"));

        BigDecimal armasDestruidas = numero(valores.get("armasdestruidas"));
        BigDecimal municaoDestruidas = numero(valores.get("municaodestruidas"));
        BigDecimal outrosDestruidos = numero(valores.get("outrospcedestruidos"));
        BigDecimal carregDestruidos = numero(valores.get("carregadoresdestruidos"));
        BigDecimal armasJustica = numero(valores.get("armasdajusticasemordemdedestruicao"));
        BigDecimal armasBrasonadas = numero(valores.get("armasbrasonadas"));
        BigDecimal armasRestituidas = numero(valores.get("armasrestituidas"));

        valores.put("saldoatualdearmas",
                pcerecArmas.add(saldoAntArmas).subtract(armasDestruidas.add(armasJustica).add(armasBrasonadas).add(armasRestituidas)));
        valores.put("saldoatualdemunicoes", pcerecMunicao.add(saldoAntMunicao).subtract(municaoDestruidas));
        valores.put("saldoatualoutrospce", pcerecOutros.add(saldoAntOutros).subtract(outrosDestruidos));
        valores.put("saldoatualcorregadores", pcerecCarreg.add(saldoAntCarreg).subtract(carregDestruidos));
    }

    private void preencherSaldoAnteriorSeVazio(Map<String, Object> valores, Map<String, Object> ultimo,
                                               String campoDestino, String campoOrigem) {
        Object atual = valores.get(campoDestino);
        if (atual == null || (atual instanceof String s && s.isBlank())) {
            valores.put(campoDestino, ultimo.get(campoOrigem));
        }
    }

    private BigDecimal numero(Object valor) {
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
