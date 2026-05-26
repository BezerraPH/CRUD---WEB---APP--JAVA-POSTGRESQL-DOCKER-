package br.mil.sfpc.indicadores.web;

/**
 * Erro de regra de negócio com destino de redirect explícito para a interface.
 */
public class NegocioException extends RuntimeException {

    private final String redirectPath;

    public NegocioException(String message, String redirectPath) {
        super(message);
        this.redirectPath = redirectPath != null && !redirectPath.isBlank() ? redirectPath : "/formularios";
    }

    public String getRedirectPath() {
        return redirectPath;
    }
}
