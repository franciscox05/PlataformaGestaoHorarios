package com.example.projeto2.API.Services.geracao.diagnostico;

import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Services.HorarioValidatorService;
import com.example.projeto2.API.Services.geracao.EstadoColaborador;
import com.example.projeto2.API.Services.geracao.MotivoFalhaGeracao;
import com.example.projeto2.API.Services.geracao.PedidoGeracao;
import com.example.projeto2.API.Services.geracao.SugestaoFalhaGeracao;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Analisa <em>porquê</em> um turno crítico ficou sem colaboradores disponíveis,
 * agregando os motivos de exclusão por código e produzindo sugestões acionáveis
 * para o gestor. Função pura — não altera estado, apenas inspeciona.
 *
 * <p>Antes, a exceção de falha era lançada com listas de motivos/sugestões vazias,
 * pelo que o painel de diagnóstico aparecia sem conteúdo. Esta classe preenche-o.
 */
public final class DiagnosticoCobertura {

    private final String motivoPrincipal;
    private final List<MotivoFalhaGeracao> motivos;
    private final List<SugestaoFalhaGeracao> sugestoes;

    private DiagnosticoCobertura(String motivoPrincipal,
                                 List<MotivoFalhaGeracao> motivos,
                                 List<SugestaoFalhaGeracao> sugestoes) {
        this.motivoPrincipal = motivoPrincipal;
        this.motivos = motivos;
        this.sugestoes = sugestoes;
    }

    public String motivoPrincipal() { return motivoPrincipal; }

    public List<MotivoFalhaGeracao> motivos() { return motivos; }

    public List<SugestaoFalhaGeracao> sugestoes() { return sugestoes; }

    /**
     * Constrói o diagnóstico para o slot crítico num dia: para cada colaborador
     * considerado, determina o motivo (representativo) pelo qual ficou de fora.
     */
    public static DiagnosticoCobertura analisar(LocalDate data,
                                                List<Turno> turnosCriticos,
                                                Collection<EstadoColaborador> estados,
                                                PedidoGeracao pedido,
                                                HorarioValidatorService validator) {
        Map<String, List<String>> nomesPorCodigo = new LinkedHashMap<>();

        for (EstadoColaborador estado : estados) {
            String codigo = motivoRepresentativo(estado, data, turnosCriticos, pedido, validator);
            if (codigo == null) {
                continue; // afinal era elegível (cenário improvável na falha) — ignora
            }
            nomesPorCodigo.computeIfAbsent(codigo, k -> new ArrayList<>()).add(estado.nome());
        }

        if (nomesPorCodigo.isEmpty()) {
            return new DiagnosticoCobertura(
                    "Sem candidatos suficientes para o slot mais crítico.", List.of(), List.of());
        }

        List<MotivoFalhaGeracao> motivos = nomesPorCodigo.entrySet().stream()
                .map(e -> new MotivoFalhaGeracao(e.getKey(), e.getValue().size(),
                        descricao(e.getKey()), List.copyOf(e.getValue())))
                .sorted(Comparator.comparingInt(MotivoFalhaGeracao::total).reversed())
                .toList();

        MotivoFalhaGeracao dominante = motivos.getFirst();
        String motivoPrincipal = descricao(dominante.codigo())
                + " (" + dominante.total() + " colaborador" + (dominante.total() == 1 ? "" : "es") + ").";

        List<SugestaoFalhaGeracao> sugestoes = motivos.stream()
                .map(m -> sugestao(m.codigo()))
                .filter(s -> s != null)
                .toList();

        return new DiagnosticoCobertura(motivoPrincipal, motivos, sugestoes);
    }

    /** Motivo do turno representativo (mais longo); fallback para o primeiro turno. */
    private static String motivoRepresentativo(EstadoColaborador estado,
                                               LocalDate data,
                                               List<Turno> turnosCriticos,
                                               PedidoGeracao pedido,
                                               HorarioValidatorService validator) {
        String motivo = null;
        long maiorDuracao = -1;
        for (Turno turno : turnosCriticos) {
            long minutos = validator.calcularDuracaoEmMinutos(turno);
            String codigo = estado.diagnosticarExclusao(data, turno, minutos, pedido);
            if (codigo == null) {
                return null; // elegível para algum turno do slot
            }
            if (minutos > maiorDuracao) {
                maiorDuracao = minutos;
                motivo = codigo;
            }
        }
        return motivo;
    }

    private static String descricao(String codigo) {
        return switch (codigo) {
            case "parttime_fim_semana" -> "Reforço de fim de semana — indisponível em dias úteis";
            case "turno_curto"         -> "Perfil de tempo inteiro exige turno de pelo menos 8 horas";
            case "bloqueado"           -> "Folga ou ausência aprovada nesta data";
            case "ja_escalado"         -> "Já tem turno atribuído neste dia";
            case "carga_esgotada"      -> "Carga contratual mensal esgotada";
            case "descanso_semanal"    -> "Atingiu o máximo de dias de trabalho na semana";
            case "rotacao_fim_semana"  -> "Rotação de fins de semana — já trabalhou um fim de semana recente";
            case "dias_consecutivos"   -> "Atingiu o máximo de dias consecutivos de trabalho";
            case "descanso_minimo"     -> "Descanso mínimo entre turnos não cumprido";
            default                     -> "Indisponível por restrição operacional";
        };
    }

    private static SugestaoFalhaGeracao sugestao(String codigo) {
        return switch (codigo) {
            case "carga_esgotada" -> new SugestaoFalhaGeracao(codigo,
                    "A carga contratual da equipa esgotou-se antes do fim do mês. Abre 'O que é considerado?' "
                            + "no passo 1 para comparar a capacidade da equipa com a necessidade mínima; "
                            + "depois reduz os mínimos por turno ou reforça a equipa.",
                    "full-time");
            case "turno_curto" -> new SugestaoFalhaGeracao(codigo,
                    "Disponibiliza um turno mais curto para este dia ou reforça com part-time.",
                    "part-time");
            case "parttime_fim_semana" -> new SugestaoFalhaGeracao(codigo,
                    "Este dia útil precisa de colaboradores de tempo inteiro ou part-time, não de reforço de fim de semana.",
                    "full-time");
            case "bloqueado" -> new SugestaoFalhaGeracao(codigo,
                    "Há folgas/ausências aprovadas a concentrar-se nesta data; revê os pedidos aprovados.",
                    null);
            case "descanso_semanal", "dias_consecutivos", "descanso_minimo" -> new SugestaoFalhaGeracao(codigo,
                    "Os limites de descanso impedem mais atribuições; reduz o mínimo por turno ou reforça a equipa.",
                    "part-time");
            case "rotacao_fim_semana" -> new SugestaoFalhaGeracao(codigo,
                    "A rotação de fins de semana esgotou a equipa disponível; considera reforço de fim de semana.",
                    "reforco de fim de semana");
            default -> null;
        };
    }
}
