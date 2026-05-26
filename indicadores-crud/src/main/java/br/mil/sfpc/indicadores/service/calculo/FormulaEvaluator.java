package br.mil.sfpc.indicadores.service.calculo;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Avalia expressões numéricas simples: campos, + - * / e parênteses.
 * Ex.: {@code pcerec_armas + saldoanosanteriorarmas - armasdestruidas}
 */
@Component
public class FormulaEvaluator {

    private static final Pattern CHARS_PERMITIDOS = Pattern.compile("^[a-zA-Z0-9_+\\-*/().\\s]+$");
    private static final MathContext MC = new MathContext(16, RoundingMode.HALF_UP);

    public BigDecimal avaliar(String expressao, Map<String, BigDecimal> variaveis, Set<String> camposPermitidos) {
        if (expressao == null || expressao.isBlank()) {
            throw new IllegalArgumentException("Expressão vazia.");
        }
        String expr = expressao.trim();
        if (!CHARS_PERMITIDOS.matcher(expr).matches()) {
            throw new IllegalArgumentException("Expressão contém caracteres não permitidos.");
        }
        List<Token> tokens = tokenizar(expr, variaveis, camposPermitidos);
        List<Token> rpn = paraRpn(tokens);
        return avaliarRpn(rpn);
    }

    private List<Token> tokenizar(String expr, Map<String, BigDecimal> variaveis, Set<String> camposPermitidos) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        while (i < expr.length()) {
            char c = expr.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            if (Character.isLetter(c) || c == '_') {
                int start = i;
                while (i < expr.length() && (Character.isLetterOrDigit(expr.charAt(i)) || expr.charAt(i) == '_')) {
                    i++;
                }
                String nome = expr.substring(start, i);
                if (!camposPermitidos.contains(nome)) {
                    throw new IllegalArgumentException("Campo desconhecido na fórmula: " + nome);
                }
                BigDecimal val = variaveis.getOrDefault(nome, BigDecimal.ZERO);
                tokens.add(Token.numero(val));
                continue;
            }
            if (Character.isDigit(c) || c == '.') {
                int start = i;
                while (i < expr.length() && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) {
                    i++;
                }
                tokens.add(Token.numero(new BigDecimal(expr.substring(start, i))));
                continue;
            }
            switch (c) {
                case '+' -> tokens.add(Token.op('+', 1));
                case '-' -> tokens.add(Token.op('-', 1));
                case '*' -> tokens.add(Token.op('*', 2));
                case '/' -> tokens.add(Token.op('/', 2));
                case '(' -> tokens.add(Token.paren('('));
                case ')' -> tokens.add(Token.paren(')'));
                default -> throw new IllegalArgumentException("Caractere inválido: " + c);
            }
            i++;
        }
        return tokens;
    }

    private List<Token> paraRpn(List<Token> tokens) {
        List<Token> saida = new ArrayList<>();
        List<Token> ops = new ArrayList<>();
        for (Token t : tokens) {
            if (t.tipo == Tipo.NUMERO) {
                saida.add(t);
            } else if (t.tipo == Tipo.PAREN && t.texto.equals("(")) {
                ops.add(t);
            } else if (t.tipo == Tipo.PAREN && t.texto.equals(")")) {
                while (!ops.isEmpty() && !ops.get(ops.size() - 1).texto.equals("(")) {
                    saida.add(ops.remove(ops.size() - 1));
                }
                if (ops.isEmpty()) {
                    throw new IllegalArgumentException("Parênteses desbalanceados.");
                }
                ops.remove(ops.size() - 1);
            } else if (t.tipo == Tipo.OP) {
                while (!ops.isEmpty() && ops.get(ops.size() - 1).tipo == Tipo.OP
                        && ops.get(ops.size() - 1).precedencia >= t.precedencia) {
                    saida.add(ops.remove(ops.size() - 1));
                }
                ops.add(t);
            }
        }
        while (!ops.isEmpty()) {
            Token op = ops.remove(ops.size() - 1);
            if (op.tipo == Tipo.PAREN) {
                throw new IllegalArgumentException("Parênteses desbalanceados.");
            }
            saida.add(op);
        }
        return saida;
    }

    private BigDecimal avaliarRpn(List<Token> rpn) {
        List<BigDecimal> pilha = new ArrayList<>();
        for (Token t : rpn) {
            if (t.tipo == Tipo.NUMERO) {
                pilha.add(t.valor);
            } else {
                if (pilha.size() < 2) {
                    throw new IllegalArgumentException("Expressão inválida.");
                }
                BigDecimal b = pilha.remove(pilha.size() - 1);
                BigDecimal a = pilha.remove(pilha.size() - 1);
                pilha.add(operar(a, b, t.texto.charAt(0)));
            }
        }
        if (pilha.size() != 1) {
            throw new IllegalArgumentException("Expressão inválida.");
        }
        return pilha.get(0);
    }

    private BigDecimal operar(BigDecimal a, BigDecimal b, char op) {
        return switch (op) {
            case '+' -> a.add(b, MC);
            case '-' -> a.subtract(b, MC);
            case '*' -> a.multiply(b, MC);
            case '/' -> b.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : a.divide(b, MC);
            default -> throw new IllegalArgumentException("Operador inválido.");
        };
    }

    private enum Tipo { NUMERO, OP, PAREN }

    private static final class Token {
        final Tipo tipo;
        final BigDecimal valor;
        final String texto;
        final int precedencia;

        private Token(Tipo tipo, BigDecimal valor, String texto, int precedencia) {
            this.tipo = tipo;
            this.valor = valor;
            this.texto = texto;
            this.precedencia = precedencia;
        }

        static Token numero(BigDecimal v) {
            return new Token(Tipo.NUMERO, v, null, 0);
        }

        static Token op(char c, int p) {
            return new Token(Tipo.OP, null, String.valueOf(c), p);
        }

        static Token paren(char c) {
            return new Token(Tipo.PAREN, null, String.valueOf(c), 0);
        }
    }
}
