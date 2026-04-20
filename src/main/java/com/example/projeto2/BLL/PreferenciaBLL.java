package com.example.projeto2.BLL;

import com.example.projeto2.Modules.Preferencia;
import com.example.projeto2.Modules.Utilizador;
import com.example.projeto2.Repositories.PreferenciaRepository;
import com.example.projeto2.Repositories.UtilizadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
public class PreferenciaBLL {

    private static final Set<String> TIPOS_VALIDOS = Set.of("folgas", "ferias", "colegas", "turnos");
    private static final Set<String> ESTADOS_VALIDOS = Set.of("pendente", "aprovado", "rejeitado");

    private final PreferenciaRepository preferenciaRepository;
    private final UtilizadorRepository utilizadorRepository;

    public PreferenciaBLL(PreferenciaRepository preferenciaRepository, UtilizadorRepository utilizadorRepository) {
        this.preferenciaRepository = preferenciaRepository;
        this.utilizadorRepository = utilizadorRepository;
    }

    @Transactional(readOnly = true)
    public List<Preferencia> listarPreferenciasPorUtilizador(Integer idUtilizador) {
        validarUtilizador(idUtilizador);
        return preferenciaRepository.findByIdUtilizadorIdOrderByDataInicioAscIdDesc(idUtilizador);
    }

    @Transactional(readOnly = true)
    public Preferencia obterPreferenciaDoUtilizador(Integer idUtilizador, Integer idPreferencia) {
        validarUtilizador(idUtilizador);

        if (idPreferencia == null) {
            throw new IllegalArgumentException("A preferencia selecionada e obrigatoria.");
        }

        return preferenciaRepository.findByIdAndIdUtilizadorId(idPreferencia, idUtilizador)
                .orElseThrow(() -> new IllegalArgumentException("Nao foi possivel encontrar a preferencia selecionada."));
    }

    @Transactional
    public Preferencia guardarPreferencia(Integer idUtilizador, Preferencia preferenciaRecebida) {
        Utilizador utilizador = obterUtilizador(idUtilizador);

        if (preferenciaRecebida == null) {
            throw new IllegalArgumentException("A preferencia e obrigatoria.");
        }

        Preferencia preferenciaPersistida;
        if (preferenciaRecebida.getId() != null) {
            preferenciaPersistida = preferenciaRepository.findByIdAndIdUtilizadorId(preferenciaRecebida.getId(), idUtilizador)
                    .orElseThrow(() -> new IllegalArgumentException("Nao foi possivel atualizar a preferencia selecionada."));
        } else {
            preferenciaPersistida = new Preferencia();
            preferenciaPersistida.setIdUtilizador(utilizador);
        }

        String tipoNormalizado = normalizarTipo(preferenciaRecebida.getTipo());
        String descricaoNormalizada = normalizarDescricao(preferenciaRecebida.getDescricao());
        Integer prioridadeNormalizada = normalizarPrioridade(preferenciaRecebida.getPrioridade());
        LocalDate dataInicio = preferenciaRecebida.getDataInicio();
        LocalDate dataFim = preferenciaRecebida.getDataFim();

        validarPeriodo(tipoNormalizado, dataInicio, dataFim);

        if (preferenciaRepository.existsPreferenciaDuplicada(
                idUtilizador,
                tipoNormalizado,
                descricaoNormalizada,
                prioridadeNormalizada,
                dataInicio,
                dataFim,
                preferenciaPersistida.getId()
        )) {
            throw new IllegalArgumentException("Ja tens uma preferencia igual registada.");
        }

        preferenciaPersistida.setIdUtilizador(utilizador);
        preferenciaPersistida.setTipo(tipoNormalizado);
        preferenciaPersistida.setDataInicio(dataInicio);
        preferenciaPersistida.setDataFim(dataFim);
        preferenciaPersistida.setPrioridade(prioridadeNormalizada);
        preferenciaPersistida.setDescricao(descricaoNormalizada);
        preferenciaPersistida.setEstado("pendente");

        return preferenciaRepository.save(preferenciaPersistida);
    }

    @Transactional
    public void removerPreferencia(Integer idUtilizador, Integer idPreferencia) {
        Preferencia preferencia = obterPreferenciaDoUtilizador(idUtilizador, idPreferencia);
        preferenciaRepository.delete(preferencia);
    }

    @Transactional(readOnly = true)
    public boolean estadoValido(String estado) {
        if (estado == null || estado.isBlank()) {
            return false;
        }
        return ESTADOS_VALIDOS.contains(estado.trim().toLowerCase());
    }

    private void validarUtilizador(Integer idUtilizador) {
        if (idUtilizador == null) {
            throw new IllegalArgumentException("Nao foi possivel identificar o utilizador autenticado.");
        }
    }

    private Utilizador obterUtilizador(Integer idUtilizador) {
        validarUtilizador(idUtilizador);

        return utilizadorRepository.findById(idUtilizador)
                .orElseThrow(() -> new IllegalArgumentException("Nao foi possivel encontrar o utilizador autenticado."));
    }

    private String normalizarTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            throw new IllegalArgumentException("Seleciona um tipo de preferencia.");
        }

        String tipoNormalizado = tipo.trim().toLowerCase();
        if (!TIPOS_VALIDOS.contains(tipoNormalizado)) {
            throw new IllegalArgumentException("O tipo de preferencia selecionado e invalido.");
        }

        return tipoNormalizado;
    }

    private String normalizarDescricao(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("A descricao da preferencia e obrigatoria.");
        }

        String descricaoNormalizada = descricao.trim();
        if (descricaoNormalizada.length() < 5) {
            throw new IllegalArgumentException("A descricao deve ter pelo menos 5 caracteres.");
        }

        if (descricaoNormalizada.length() > 1000) {
            throw new IllegalArgumentException("A descricao nao pode ter mais de 1000 caracteres.");
        }

        return descricaoNormalizada;
    }

    private Integer normalizarPrioridade(Integer prioridade) {
        if (prioridade == null) {
            throw new IllegalArgumentException("A prioridade e obrigatoria.");
        }

        if (prioridade < 1 || prioridade > 5) {
            throw new IllegalArgumentException("A prioridade deve estar entre 1 e 5.");
        }

        return prioridade;
    }

    private void validarPeriodo(String tipo, LocalDate dataInicio, LocalDate dataFim) {
        if (dataFim != null && dataInicio == null) {
            throw new IllegalArgumentException("Seleciona a data inicial antes da data final.");
        }

        if (dataInicio != null && dataFim != null && dataFim.isBefore(dataInicio)) {
            throw new IllegalArgumentException("A data final nao pode ser anterior a data inicial.");
        }

        if (("folgas".equals(tipo) || "ferias".equals(tipo)) && dataInicio == null) {
            throw new IllegalArgumentException("As preferencias de folgas e ferias precisam de pelo menos uma data inicial.");
        }
    }
}
