package br.mil.sfpc.indicadores.config;

/** Um campo do formulário (nome da coluna, tipo, rótulo, obrigatoriedade). */
public class CampoConfig {

    private String nome;
    private String label;
    private String tipo = "text";
    private boolean obrigatorio;
    private boolean somenteLeitura;
    private boolean ocultoFormulario;
    private Object valorPadrao;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public boolean isObrigatorio() {
        return obrigatorio;
    }

    public void setObrigatorio(boolean obrigatorio) {
        this.obrigatorio = obrigatorio;
    }

    public boolean isSomenteLeitura() {
        return somenteLeitura;
    }

    public void setSomenteLeitura(boolean somenteLeitura) {
        this.somenteLeitura = somenteLeitura;
    }

    public boolean isOcultoFormulario() {
        return ocultoFormulario;
    }

    public void setOcultoFormulario(boolean ocultoFormulario) {
        this.ocultoFormulario = ocultoFormulario;
    }

    public Object getValorPadrao() {
        return valorPadrao;
    }

    public void setValorPadrao(Object valorPadrao) {
        this.valorPadrao = valorPadrao;
    }
}
