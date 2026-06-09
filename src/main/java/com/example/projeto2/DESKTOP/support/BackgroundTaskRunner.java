package com.example.projeto2.DESKTOP.support;

import javafx.concurrent.Task;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Pequeno utilitário para correr trabalho em background a partir de um controller JavaFX.
 * Garante que só há uma operação em curso de cada vez, e dispara callbacks na thread FX
 * para refrescar o estado da UI quando termina.
 *
 * <p>Uso típico:
 * <pre>{@code
 *   BackgroundTaskRunner runner = new BackgroundTaskRunner(
 *           this::mostrarInformacao,
 *           this::atualizarEstadoInterativo,
 *           "geracao-horarios-worker");
 *
 *   runner.executar(
 *           "A carregar planeamento...",
 *           () -> servico.obterPlaneamento(...),
 *           planeamento -> preencherUI(planeamento),
 *           erro -> mostrarErro(erro.getMessage())
 *   );
 * }</pre>
 */
public final class BackgroundTaskRunner {

    private final Consumer<String> notificadorProgresso;
    private final Runnable onEstadoMudou;
    private final String nomeThread;
    private Task<?> emCurso;

    public BackgroundTaskRunner(Consumer<String> notificadorProgresso,
                                Runnable onEstadoMudou,
                                String nomeThread) {
        this.notificadorProgresso = notificadorProgresso;
        this.onEstadoMudou = onEstadoMudou;
        this.nomeThread = nomeThread;
    }

    public boolean isRunning() {
        return emCurso != null && emCurso.isRunning();
    }

    /**
     * Tenta executar {@code acao} em background. Se já houver uma operação em curso, mostra
     * uma mensagem ao utilizador e devolve sem fazer nada.
     *
     * <p>Os callbacks {@code onSucesso}/{@code onFalha} correm na thread JavaFX.
     */
    public <T> void executar(String mensagemProgresso,
                             Callable<T> acao,
                             Consumer<T> onSucesso,
                             Consumer<Throwable> onFalha) {
        if (isRunning()) {
            notificadorProgresso.accept(
                    "Existe uma operação em curso. Aguarda pela conclusão antes de iniciar outra.");
            return;
        }

        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return acao.call();
            }
        };

        emCurso = task;
        onEstadoMudou.run();
        notificadorProgresso.accept(mensagemProgresso);

        task.setOnSucceeded(e -> {
            emCurso = null;
            onEstadoMudou.run();
            onSucesso.accept(task.getValue());
        });
        task.setOnFailed(e -> {
            emCurso = null;
            onEstadoMudou.run();
            Throwable erro = task.getException() != null
                    ? task.getException()
                    : new IllegalStateException("Ocorreu um erro inesperado durante a operação.");
            onFalha.accept(erro);
        });
        task.setOnCancelled(e -> {
            emCurso = null;
            onEstadoMudou.run();
            notificadorProgresso.accept("A operação foi cancelada.");
        });

        Thread thread = new Thread(task, nomeThread);
        thread.setDaemon(true);
        thread.start();
    }
}
