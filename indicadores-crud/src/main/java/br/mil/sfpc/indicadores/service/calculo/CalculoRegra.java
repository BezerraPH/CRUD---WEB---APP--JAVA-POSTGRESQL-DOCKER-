package br.mil.sfpc.indicadores.service.calculo;

/**
 * Uma regra que define o valor de um campo calculado a partir de uma expressão.
 */
public record CalculoRegra(
        Long id,
        String campoDestino,
        String label,
        String expressao,
        boolean ativo,
        int ordem
) {
}
