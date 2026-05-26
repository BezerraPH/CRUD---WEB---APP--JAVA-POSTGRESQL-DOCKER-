package br.mil.sfpc.indicadores.service;

import br.mil.sfpc.indicadores.config.CampoConfig;
import br.mil.sfpc.indicadores.config.ColunaListagem;
import br.mil.sfpc.indicadores.config.GrupoConfig;
import br.mil.sfpc.indicadores.config.ModuloConfig;
import br.mil.sfpc.indicadores.config.ModuloProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Copia metadados de formulários definidos em {@code modulos.yml} para o banco,
 * permitindo editar/excluir pela interface sem alterar o arquivo YAML.
 */
@Service
public class FormularioYamlSyncService {

    private final ModuloProperties yamlProperties;
    private final FormularioMetaRepository metaRepository;

    public FormularioYamlSyncService(ModuloProperties yamlProperties, FormularioMetaRepository metaRepository) {
        this.yamlProperties = yamlProperties;
        this.metaRepository = metaRepository;
    }

    /** Garante que o slug exista em {@code formulario_def} (importa do YAML se necessário). */
    @Transactional
    public void garantirMetadadosNoBanco(String slug) {
        if (metaRepository.existeSlug(slug) || metaRepository.isSuprimido(slug)) {
            return;
        }
        yamlProperties.findBySlug(slug).ifPresent(this::importarMetadados);
    }

    @Transactional
    public void sincronizarTodosDoYaml() {
        for (ModuloConfig modulo : yamlProperties.getModulos()) {
            if (!metaRepository.existeSlug(modulo.getSlug()) && !metaRepository.isSuprimido(modulo.getSlug())) {
                importarMetadados(modulo);
            }
        }
    }

    private void importarMetadados(ModuloConfig m) {
        if (m.getGrupos() == null || m.getGrupos().isEmpty()) {
            return;
        }
        String colOrdem = m.getColunaOrdem() != null ? m.getColunaOrdem() : "id";
        long formId = metaRepository.inserirDefinicao(
                m.getSlug(),
                m.getTitulo(),
                m.getDescricao(),
                m.getTabela(),
                colOrdem,
                m.getProcessador());

        int ordemGrupo = 0;
        for (GrupoConfig grupo : m.getGrupos()) {
            long grupoId = metaRepository.inserirGrupo(
                    formId,
                    grupo.getTitulo() != null ? grupo.getTitulo() : "Dados",
                    grupo.getEstilo() != null ? grupo.getEstilo() : "primary",
                    grupo.getColunas() > 0 ? grupo.getColunas() : 1,
                    ordemGrupo++);
            int ordemCampo = 0;
            if (grupo.getCampos() != null) {
                for (CampoConfig campo : grupo.getCampos()) {
                    metaRepository.inserirCampo(
                            grupoId,
                            campo.getNome(),
                            campo.getLabel(),
                            campo.getTipo() != null ? campo.getTipo() : "number",
                            campo.isObrigatorio(),
                            ordemCampo++);
                }
            }
        }

        int ordemListagem = 0;
        if (m.getColunasListagem() != null && !m.getColunasListagem().isEmpty()) {
            for (ColunaListagem col : m.getColunasListagem()) {
                metaRepository.inserirColunaListagem(formId, col.getNome(), col.getLabel(), ordemListagem++);
            }
        } else {
            int max = 0;
            for (CampoConfig c : m.todosCampos()) {
                if (max >= 6) {
                    break;
                }
                metaRepository.inserirColunaListagem(formId, c.getNome(), c.getLabel(), ordemListagem++);
                max++;
            }
        }
    }
}
