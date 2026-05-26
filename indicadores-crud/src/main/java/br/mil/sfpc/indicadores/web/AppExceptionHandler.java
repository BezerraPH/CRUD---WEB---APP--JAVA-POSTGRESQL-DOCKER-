package br.mil.sfpc.indicadores.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Converte erros de validação de negócio em mensagem flash exibida na interface.
 */
@ControllerAdvice
public class AppExceptionHandler {

    @ExceptionHandler(NegocioException.class)
    public String tratarNegocio(NegocioException ex, RedirectAttributes redirect) {
        redirect.addFlashAttribute("erro", ex.getMessage());
        return "redirect:" + ex.getRedirectPath();
    }

    /** Fallback: tenta voltar à página de origem ou à lista de formulários. */
    @ExceptionHandler(IllegalArgumentException.class)
    public String tratarErroNegocio(IllegalArgumentException ex, RedirectAttributes redirect,
                                    HttpServletRequest request) {
        redirect.addFlashAttribute("erro", ex.getMessage());
        String destino = inferirRedirect(request);
        return "redirect:" + destino;
    }

    private String inferirRedirect(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null) {
            try {
                var uri = java.net.URI.create(referer);
                String path = uri.getPath();
                if (path != null && (path.startsWith("/m/") || path.contains("/m/"))) {
                    int idx = path.indexOf("/m/");
                    String rest = path.substring(idx + 3);
                    int slash = rest.indexOf('/');
                    String slug = slash > 0 ? rest.substring(0, slash) : rest;
                    if (!slug.isBlank()) {
                        return "/m/" + slug;
                    }
                }
                if (path != null && path.startsWith("/formularios")) {
                    return path;
                }
            } catch (Exception ignored) {
                // referer inválido
            }
        }
        return "/formularios";
    }
}
