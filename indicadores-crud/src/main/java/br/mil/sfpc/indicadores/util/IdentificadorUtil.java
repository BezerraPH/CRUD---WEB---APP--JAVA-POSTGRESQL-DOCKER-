package br.mil.sfpc.indicadores.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

/**
 * Normaliza títulos em slugs/nomes de coluna SQL seguros (a-z, 0-9, underscore)
 * e bloqueia identificadores reservados do sistema.
 */
public final class IdentificadorUtil {

    private static final Set<String> RESERVADOS = Set.of(
            "id", "postgres", "public", "admin", "painel", "formulario_def",
            "formulario_grupo", "formulario_campo", "formulario_listagem",
            "dados_indicadores", "pce_controle_destruicao"
    );

    private IdentificadorUtil() {
    }

    public static String slugify(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        String n = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String s = n.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (s.length() > 60) {
            s = s.substring(0, 60).replaceAll("_+$", "");
        }
        return s;
    }

    public static String nomeColuna(String label, String sufixo) {
        String base = slugify(label);
        if (base.isEmpty()) {
            base = "campo";
        }
        if (sufixo != null && !sufixo.isBlank()) {
            base = base + "_" + sufixo;
        }
        if (base.length() > 63) {
            base = base.substring(0, 63).replaceAll("_+$", "");
        }
        return base;
    }

    public static boolean isIdentificadorValido(String valor) {
        return valor != null && valor.matches("^[a-z][a-z0-9_]{0,62}$");
    }

    public static void validarIdentificador(String valor, String rotulo) {
        if (!isIdentificadorValido(valor)) {
            throw new IllegalArgumentException(rotulo + " inválido: use apenas letras minúsculas, números e _ (começando com letra).");
        }
        if (RESERVADOS.contains(valor)) {
            throw new IllegalArgumentException(rotulo + " reservado: " + valor);
        }
    }
}
