package com.example.projeto2;

import com.example.projeto2.API.Services.GeracaoHorariosService;
import com.example.projeto2.API.Services.GestaoLojaService;
import com.example.projeto2.API.Services.HorarioService;
import com.example.projeto2.API.Services.SessaoService;
import com.example.projeto2.API.Services.SnapshotOperacionalLojaService;
import com.example.projeto2.DESKTOP.DashboardController;
import com.example.projeto2.DESKTOP.HomeController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = Projeto2Application.class)
@ActiveProfiles("test")
@Transactional
@Rollback
class DashboardHomeIntegrationTest extends FluxosCriticosTestSupport {

    private static volatile boolean javafxInicializado;

    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private SessaoService sessaoBLL;

    @MockitoBean
    private GestaoLojaService gestaoLojaBLL;

    @MockitoBean
    private GeracaoHorariosService geracaoHorariosBLL;

    @MockitoBean
    private HorarioService horarioBLL;

    @MockitoBean
    private SnapshotOperacionalLojaService snapshotOperacionalLojaBLL;

    @BeforeEach
    void prepararSessaoMock() {
        doNothing().when(sessaoBLL).iniciarSessao(any());
        doNothing().when(sessaoBLL).terminarSessaoManual();
        doNothing().when(sessaoBLL).expirarSessao();
        doNothing().when(sessaoBLL).registarAtividade();
        when(sessaoBLL.temSessaoAtiva()).thenReturn(false);
        when(sessaoBLL.obterTempoMaximoInatividade()).thenReturn(java.time.Duration.ofMinutes(15));

        when(gestaoLojaBLL.utilizadorPodeGerirLoja(anyInt())).thenReturn(true);
        when(geracaoHorariosBLL.utilizadorPodeValidarHorarios(anyInt())).thenReturn(true);

        when(horarioBLL.listarHorarioPublicadoDoUtilizador(anyInt(), any(), any())).thenReturn(List.of());
        when(horarioBLL.listarEquipaDeHoje(anyInt())).thenReturn(List.of());
        when(horarioBLL.listarColaboradoresAtivosDaLojaDoUtilizador(anyInt())).thenReturn(List.of());
        when(horarioBLL.listarHorarioPublicadoDaLojaDoUtilizador(anyInt(), any(), any(), any())).thenReturn(List.of());

        when(snapshotOperacionalLojaBLL.carregarSnapshot(anyInt(), any(), any())).thenReturn(
                new SnapshotOperacionalLojaService.SnapshotOperacionalLoja(
                        new SnapshotOperacionalLojaService.ContextoLoja(1, "Loja Teste", "Viana do Castelo", "Gerente"),
                        new SnapshotOperacionalLojaService.IntervaloOperacional(java.time.LocalDate.now(), java.time.LocalDate.now(), true),
                        new SnapshotOperacionalLojaService.ResumoOperacional(0, 0, 0, 0, 0, 0, 0),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()
                )
        );
    }

    @BeforeAll
    static void inicializarJavaFx() throws Exception {
        if (javafxInicializado) {
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        FutureTask<Void> startup = new FutureTask<>(() -> {
            Platform.startup(() -> {
                Platform.setImplicitExit(false);
                javafxInicializado = true;
                latch.countDown();
            });
            return null;
        });
        Thread startupThread = new Thread(startup, "javafx-startup-test");
        startupThread.setDaemon(true);
        startupThread.start();
        startup.get(15, TimeUnit.SECONDS);
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @AfterAll
    static void terminarJavaFx() throws Exception {
        if (!javafxInicializado) {
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            Platform.setImplicitExit(true);
            Platform.exit();
            latch.countDown();
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        javafxInicializado = false;
    }

    @Test
    void dashboardHomeCarregaSemFalhasParaPerfilDeGestao() throws Exception {
        GeracaoFixture fixture = criarContextoGeracao("dashboard-home");
        AtomicReference<Throwable> erro = new AtomicReference<>();
        AtomicReference<HomeController> controllerRef = new AtomicReference<>();
        AtomicReference<Parent> rootRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/projeto2/dashboard/home-view.fxml"));
                loader.setControllerFactory(applicationContext::getBean);
                Parent root = loader.load();
                HomeController controller = loader.getController();
                controller.setUtilizadorLogado(fixture.lojaFixture().gerente());
                rootRef.set(root);
                controllerRef.set(controller);
            } catch (Throwable throwable) {
                erro.set(throwable);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(45, TimeUnit.SECONDS));
        assertNull(erro.get(), () -> erro.get() == null ? "" : erro.get().toString());
        assertNotNull(rootRef.get());
        assertNotNull(controllerRef.get());
    }

    @Test
    void geracaoHorariosViewCarregaSemFalhas() throws Exception {
        // Protege a FXML do assistente de Geração de Horários: garante que todos os
        // fx:id e onAction continuam ligados ao controller (a FXML não é coberta por
        // mais nenhum teste, por isso um binding partido passaria despercebido).
        AtomicReference<Throwable> erro = new AtomicReference<>();
        AtomicReference<Parent> rootRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/projeto2/dashboard/geracao-horarios-view.fxml"));
                loader.setControllerFactory(applicationContext::getBean);
                Parent root = loader.load();
                rootRef.set(root);
            } catch (Throwable throwable) {
                erro.set(throwable);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(45, TimeUnit.SECONDS));
        assertNull(erro.get(), () -> erro.get() == null ? "" : erro.get().toString());
        assertNotNull(rootRef.get());
    }

    @Test
    void dashboardInicialNaoFicaComCentroVazioDepoisDoLogin() throws Exception {
        GeracaoFixture fixture = criarContextoGeracao("dashboard-login");
        AtomicReference<Throwable> erro = new AtomicReference<>();
        AtomicReference<BorderPane> rootRef = new AtomicReference<>();
        AtomicReference<DashboardController> controllerRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/projeto2/dashboard/dashboard-view.fxml"));
                loader.setControllerFactory(applicationContext::getBean);
                BorderPane root = loader.load();
                DashboardController controller = loader.getController();
                controller.setUtilizadorLogado(fixture.lojaFixture().gerente());
                rootRef.set(root);
                controllerRef.set(controller);
            } catch (Throwable throwable) {
                erro.set(throwable);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(45, TimeUnit.SECONDS));
        assertNull(erro.get(), () -> erro.get() == null ? "" : erro.get().toString());
        assertNotNull(rootRef.get());
        assertNotNull(controllerRef.get());
        assertNotNull(rootRef.get().getCenter());
    }
}
