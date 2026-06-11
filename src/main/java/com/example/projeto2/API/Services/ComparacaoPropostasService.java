package com.example.projeto2.API.Services;

import com.example.projeto2.API.Services.geracao.dto.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.example.projeto2.API.Services.geracao.HorarioFormatters.formatarDuracao;
import static com.example.projeto2.API.Services.geracao.HorarioFormatters.formatarDiferencaDuracao;

/**
 * Compara duas propostas de horário já resolvidas, produzindo as diferenças por
 * colaborador e a contagem de turnos que mudam de mão. Extraído da
 * {@link GeracaoHorariosService} (que mistura geração, permissões e análise) para
 * isolar a responsabilidade de <em>análise comparativa</em>.
 *
 * <p>Não toca em repositórios nem em permissões — recebe os {@code PropostaResultado}
 * já carregados pela fachada de serviço.
 */
@Component
public class ComparacaoPropostasService {

    /**
     * Constrói a comparação entre duas propostas do mesmo período.
     *
     * @param base           proposta de referência
     * @param comparada      proposta a comparar
     * @param rotuloBase     rótulo curto da base (para o resumo textual)
     * @param rotuloComparada rótulo curto da comparada
     */
    public ComparacaoPropostas comparar(
            PropostaResultado base,
            PropostaResultado comparada,
            String rotuloBase,
            String rotuloComparada) {

        Map<Integer, ResumoColaborador> basePorColaborador =
                indexarResumoPorColaborador(base.resumoColaboradores());
        Map<Integer, ResumoColaborador> comparadaPorColaborador =
                indexarResumoPorColaborador(comparada.resumoColaboradores());

        Set<Integer> idsColaboradores = new LinkedHashSet<>();
        idsColaboradores.addAll(basePorColaborador.keySet());
        idsColaboradores.addAll(comparadaPorColaborador.keySet());

        List<DiferencaColaborador> diferencas = idsColaboradores.stream()
                .map(idColaborador -> construirDiferencaColaborador(
                        basePorColaborador.get(idColaborador),
                        comparadaPorColaborador.get(idColaborador)))
                .sorted(Comparator.comparing(
                        DiferencaColaborador::colaborador, String.CASE_INSENSITIVE_ORDER))
                .toList();

        Map<String, String> atribuicoesBase = indexarAtribuicoesPorSlot(base.linhas());
        Map<String, String> atribuicoesComparada = indexarAtribuicoesPorSlot(comparada.linhas());
        Set<String> slots = new LinkedHashSet<>();
        slots.addAll(atribuicoesBase.keySet());
        slots.addAll(atribuicoesComparada.keySet());

        int turnosDiferentes = 0;
        Set<LocalDate> diasAfetados = new LinkedHashSet<>();
        for (String slot : slots) {
            if (!Objects.equals(atribuicoesBase.get(slot), atribuicoesComparada.get(slot))) {
                turnosDiferentes++;
                extrairDataDoSlot(slot).ifPresent(diasAfetados::add);
            }
        }

        String resumo = "Comparacao entre " + rotuloBase + " e " + rotuloComparada + ": "
                + turnosDiferentes + " turnos mudam de colaborador em " + diasAfetados.size()
                + " dias. Pontuacao IO " + base.metricas().pontuacao()
                + " vs " + comparada.metricas().pontuacao() + " (menor e melhor).";

        return new ComparacaoPropostas(
                base.idProposta(), comparada.idProposta(),
                rotuloBase, rotuloComparada, resumo,
                turnosDiferentes, diasAfetados.size(), diferencas);
    }

    private Map<Integer, ResumoColaborador> indexarResumoPorColaborador(
            List<ResumoColaborador> resumos) {
        Map<Integer, ResumoColaborador> porColaborador = new LinkedHashMap<>();
        for (ResumoColaborador resumo : resumos) {
            if (resumo.idColaborador() != null) {
                porColaborador.put(resumo.idColaborador(), resumo);
            }
        }
        return porColaborador;
    }

    private DiferencaColaborador construirDiferencaColaborador(
            ResumoColaborador base,
            ResumoColaborador comparada) {
        ResumoColaborador referencia = base != null ? base : comparada;
        long minutosBase = base != null ? base.minutos() : 0;
        long minutosComparada = comparada != null ? comparada.minutos() : 0;
        int turnosBase = base != null ? base.turnos() : 0;
        int turnosComparada = comparada != null ? comparada.turnos() : 0;

        return new DiferencaColaborador(
                referencia.idColaborador(),
                referencia.nome(),
                referencia.cargo(),
                turnosBase,
                formatarDuracao(minutosBase),
                turnosComparada,
                formatarDuracao(minutosComparada),
                turnosComparada - turnosBase,
                formatarDiferencaDuracao(minutosComparada - minutosBase));
    }

    private Map<String, String> indexarAtribuicoesPorSlot(List<HorarioLinha> linhas) {
        Map<String, String> atribuicoes = new LinkedHashMap<>();
        for (HorarioLinha linha : linhas) {
            if (linha.data() == null) {
                continue;
            }
            String chave = linha.data() + "|" + linha.turno() + "|" + linha.periodo();
            atribuicoes.put(chave, linha.colaborador());
        }
        return atribuicoes;
    }

    private Optional<LocalDate> extrairDataDoSlot(String slot) {
        if (slot == null || slot.isBlank()) {
            return Optional.empty();
        }
        int separador = slot.indexOf('|');
        String data = separador >= 0 ? slot.substring(0, separador) : slot;
        try {
            return Optional.of(LocalDate.parse(data));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
