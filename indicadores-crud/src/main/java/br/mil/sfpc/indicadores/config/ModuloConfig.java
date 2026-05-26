package br.mil.sfpc.indicadores.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Definição de um formulário/módulo: slug, tabela PostgreSQL, grupos de campos e colunas da listagem.
 */
public class ModuloConfig {

    private String slug;
    private String titulo;
    private String descricao;
    private String tabela;
    private String colunaOrdem;
    private String processador;
    private List<GrupoConfig> grupos = new ArrayList<>();
    private List<ColunaListagem> colunasListagem = new ArrayList<>();

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getTabela() {
        return tabela;
    }

    public void setTabela(String tabela) {
        this.tabela = tabela;
    }

    public String getColunaOrdem() {
        return colunaOrdem;
    }

    public void setColunaOrdem(String colunaOrdem) {
        this.colunaOrdem = colunaOrdem;
    }

    public String getProcessador() {
        return processador;
    }

    public void setProcessador(String processador) {
        this.processador = processador;
    }

    public List<GrupoConfig> getGrupos() {
        return grupos;
    }

    public void setGrupos(List<GrupoConfig> grupos) {
        this.grupos = grupos != null ? grupos : new ArrayList<>();
    }

    public List<ColunaListagem> getColunasListagem() {
        return colunasListagem;
    }

    public void setColunasListagem(List<ColunaListagem> colunasListagem) {
        this.colunasListagem = colunasListagem != null ? colunasListagem : new ArrayList<>();
    }

    public List<CampoConfig> todosCampos() {
        List<CampoConfig> campos = new ArrayList<>();
        for (GrupoConfig grupo : grupos) {
            campos.addAll(grupo.getCampos());
        }
        return campos;
    }
}
