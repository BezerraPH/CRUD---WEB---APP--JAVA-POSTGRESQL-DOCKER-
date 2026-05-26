package br.mil.sfpc.indicadores.config;

import java.util.ArrayList;
import java.util.List;

/** Agrupamento visual de campos na tela de lançamento. */
public class GrupoConfig {

    private String titulo;
    private String estilo = "secondary";
    private int colunas = 1;
    private List<CampoConfig> campos = new ArrayList<>();

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getEstilo() {
        return estilo;
    }

    public void setEstilo(String estilo) {
        this.estilo = estilo;
    }

    public int getColunas() {
        return colunas;
    }

    public void setColunas(int colunas) {
        this.colunas = colunas;
    }

    public List<CampoConfig> getCampos() {
        return campos;
    }

    public void setCampos(List<CampoConfig> campos) {
        this.campos = campos != null ? campos : new ArrayList<>();
    }
}
