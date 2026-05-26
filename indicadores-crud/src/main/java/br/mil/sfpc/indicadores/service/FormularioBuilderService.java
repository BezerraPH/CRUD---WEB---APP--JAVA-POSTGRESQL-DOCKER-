package br.mil.sfpc.indicadores.service;

import br.mil.sfpc.indicadores.config.ModuloConfig;
import br.mil.sfpc.indicadores.config.ModuloProperties;
import br.mil.sfpc.indicadores.util.IdentificadorUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Cria e altera formulários dinâmicos: gera tabela física no PostgreSQL,
 * registra metadados (campos, listagem) e recarrega o {@link ModuloRegistry}.
 */
@Service
public class FormularioBuilderService {

    private final JdbcTemplate jdbc;
    private final FormularioMetaRepository metaRepository;
    private final ModuloProperties yamlProperties;
    private final ModuloRegistry moduloRegistry;
    private final FormularioYamlSyncService yamlSync;

    public FormularioBuilderService(JdbcTemplate jdbc,
                                    FormularioMetaRepository metaRepository,
                                    ModuloProperties yamlProperties,
                                    ModuloRegistry moduloRegistry,
                                    FormularioYamlSyncService yamlSync) {
        this.jdbc = jdbc;
        this.metaRepository = metaRepository;
        this.yamlProperties = yamlProperties;
        this.moduloRegistry = moduloRegistry;
        this.yamlSync = yamlSync;
    }

    @Transactional
    public String criarFormulario(String titulo, String descricao, String slugInformado, String tabelaInformada,
                                  String colunaOrdem, List<CampoDefinicao> campos) {
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("Informe o título do formulário.");
        }
        if (campos == null || campos.isEmpty()) {
            throw new IllegalArgumentException("Adicione pelo menos um campo.");
        }

        String slug = slugInformado != null && !slugInformado.isBlank()
                ? slugInformado.trim().toLowerCase()
                : IdentificadorUtil.slugify(titulo);
        IdentificadorUtil.validarIdentificador(slug, "Identificador (slug)");

        String tabela = tabelaInformada != null && !tabelaInformada.isBlank()
                ? tabelaInformada.trim().toLowerCase()
                : slug;
        IdentificadorUtil.validarIdentificador(tabela, "Nome da tabela");

        if (metaRepository.existeSlug(slug) || yamlProperties.findBySlug(slug).isPresent()) {
            throw new IllegalArgumentException("Já existe um formulário com o identificador: " + slug);
        }
        if (metaRepository.existeTabela(tabela) || metaRepository.tabelaFisicaExiste(tabela)) {
            throw new IllegalArgumentException("A tabela já existe: " + tabela);
        }

        List<String> nomesFinais = new ArrayList<>();
        List<CampoDefinicao> camposNorm = new ArrayList<>();
        Set<String> nomesUsados = new HashSet<>();
        boolean temData = false;

        for (int i = 0; i < campos.size(); i++) {
            CampoDefinicao c = campos.get(i);
            String nome = resolverNomeColuna(c, i, nomesUsados);
            if ("date".equals(c.tipo())) {
                temData = true;
            }
            nomesFinais.add(nome);
            camposNorm.add(new CampoDefinicao(c.label(), c.tipo(), c.obrigatorio(), nome));
        }
        if (!temData) {
            throw new IllegalArgumentException("Inclua pelo menos um campo do tipo Data (para ordenação e Grafana).");
        }

        String colOrdem = colunaOrdem != null && !colunaOrdem.isBlank()
                ? colunaOrdem.trim().toLowerCase()
                : primeiraColunaData(camposNorm, nomesFinais);
        if (!nomesFinais.contains(colOrdem)) {
            throw new IllegalArgumentException("Coluna de ordenação inválida: " + colOrdem);
        }
        IdentificadorUtil.validarIdentificador(colOrdem, "Coluna de ordenação");

        criarTabelaFisica(tabela, camposNorm);
        jdbc.execute("CREATE INDEX IF NOT EXISTS idx_" + tabela + "_ordem ON " + tabela + " (" + colOrdem + " DESC, id DESC)");

        long formId = metaRepository.inserirDefinicao(slug, titulo, descricao, tabela, colOrdem, null);
        long grupoId = metaRepository.inserirGrupo(formId, "Dados", "primary", 1, 0);

        int ordem = 0;
        for (CampoDefinicao c : camposNorm) {
            metaRepository.inserirCampo(grupoId, c.nome(), c.label(), c.tipo(), c.obrigatorio(), ordem++);
        }

        int listOrdem = 0;
        int maxListagem = Math.min(6, camposNorm.size());
        for (int i = 0; i < maxListagem; i++) {
            CampoDefinicao c = camposNorm.get(i);
            metaRepository.inserirColunaListagem(formId, c.nome(), c.label(), listOrdem++);
        }

        moduloRegistry.reload();
        return slug;
    }

    @Transactional
    public void adicionarCampo(String slug, String label, String tipo, boolean obrigatorio, String nomeInformado) {
        var moduloOpt = metaRepository.carregarPorSlug(slug);
        if (moduloOpt.isEmpty()) {
            throw new IllegalArgumentException("Formulário não encontrado.");
        }
        var modulo = moduloOpt.get();
        long formId = metaRepository.idPorSlug(slug).orElseThrow();
        long grupoId = metaRepository.grupoPrincipalId(formId).orElseThrow();

        String nome = nomeInformado != null && !nomeInformado.isBlank()
                ? nomeInformado.trim().toLowerCase()
                : IdentificadorUtil.nomeColuna(label, null);
        IdentificadorUtil.validarIdentificador(nome, "Nome da coluna");
        if (metaRepository.campoExiste(formId, nome)) {
            throw new IllegalArgumentException("Campo já existe: " + nome);
        }
        validarTipoCampo(tipo);

        String sqlType = tipoSql(tipo, obrigatorio);
        jdbc.execute("ALTER TABLE " + modulo.getTabela() + " ADD COLUMN " + nome + " " + sqlType);

        int ordem = modulo.todosCampos().size();
        metaRepository.inserirCampo(grupoId, nome, label, tipo, obrigatorio, ordem);
        metaRepository.inserirColunaListagem(formId, nome, label, ordem);

        moduloRegistry.reload();
    }

    @Transactional
    public void excluirFormulario(String slug) {
        yamlSync.garantirMetadadosNoBanco(slug);
        var moduloOpt = metaRepository.carregarPorSlug(slug);
        var yamlOpt = yamlProperties.findBySlug(slug);
        if (moduloOpt.isEmpty() && yamlOpt.isEmpty()) {
            throw new IllegalArgumentException("Formulário não encontrado.");
        }
        String tabela = moduloOpt.map(ModuloConfig::getTabela)
                .orElseGet(() -> yamlOpt.map(ModuloConfig::getTabela).orElse(null));
        if (tabela != null && !tabela.isBlank()) {
            jdbc.execute("DROP TABLE IF EXISTS " + tabela + " CASCADE");
        }
        metaRepository.excluirPorSlug(slug);
        if (yamlOpt.isPresent()) {
            metaRepository.suprimirSlug(slug);
        }
        moduloRegistry.reload();
    }

    private void criarTabelaFisica(String tabela, List<CampoDefinicao> campos) {
        StringBuilder ddl = new StringBuilder("CREATE TABLE ");
        ddl.append(tabela).append(" (id SERIAL PRIMARY KEY");
        for (CampoDefinicao c : campos) {
            ddl.append(", ").append(c.nome()).append(" ").append(tipoSql(c.tipo(), c.obrigatorio()));
        }
        ddl.append(")");
        jdbc.execute(ddl.toString());
    }

    private String resolverNomeColuna(CampoDefinicao c, int indice, Set<String> nomesUsados) {
        String nome = c.nome() != null && !c.nome().isBlank()
                ? c.nome().trim().toLowerCase()
                : IdentificadorUtil.nomeColuna(c.label(), String.valueOf(indice + 1));
        IdentificadorUtil.validarIdentificador(nome, "Nome da coluna");
        if (!nomesUsados.add(nome)) {
            throw new IllegalArgumentException("Campo duplicado: " + nome);
        }
        return nome;
    }

    private String primeiraColunaData(List<CampoDefinicao> campos, List<String> nomes) {
        for (int i = 0; i < campos.size(); i++) {
            if ("date".equals(campos.get(i).tipo())) {
                return nomes.get(i);
            }
        }
        throw new IllegalArgumentException("Nenhuma coluna de data encontrada.");
    }

    private void validarTipoCampo(String tipo) {
        if (tipo == null || (!tipo.equals("date") && !tipo.equals("number") && !tipo.equals("text"))) {
            throw new IllegalArgumentException("Tipo de campo inválido. Use: Data, Número ou Texto.");
        }
    }

    private String tipoSql(String tipo, boolean obrigatorio) {
        return switch (tipo != null ? tipo : "number") {
            case "date" -> obrigatorio ? "DATE NOT NULL" : "DATE";
            case "text" -> "VARCHAR(500) DEFAULT ''";
            default -> "NUMERIC DEFAULT 0";
        };
    }

    public record CampoDefinicao(String label, String tipo, boolean obrigatorio, String nome) {
    }
}
