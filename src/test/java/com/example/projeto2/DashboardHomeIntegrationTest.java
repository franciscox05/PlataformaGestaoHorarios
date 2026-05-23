package com.example.projeto2;

import com.example.projeto2.Controller.DashboardController;
import com.example.projeto2.Controller.HomeController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = Projeto2Application.class)
@ActiveProfiles("test")
@Transactional
@Rollback
class DashboardHomeIntegrationTest extends FluxosCriticosTestSupport {

    private static volatile boolean javafxInicializado;

    @Autowired
    private ApplicationContext applicationContext;

    @BeforeAll
    static void inicializarJavaFx() throws Exception {
        if (javafxInicializado) {
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        Platform.startup(() -> {
            javafxInicializado = true;
            latch.countDown();
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS));
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

        assertTrue(latch.await(15, TimeUnit.SECONDS));
        assertNull(erro.get(), () -> erro.get() == null ? "" : erro.get().toString());
        assertNotNull(rootRef.get());
        assertNotNull(controllerRef.get());
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

        assertTrue(latch.await(15, TimeUnit.SECONDS));
        assertNull(erro.get(), () -> erro.get() == null ? "" : erro.get().toString());
        assertNotNull(rootRef.get());
        assertNotNull(controllerRef.get());
        assertNotNull(rootRef.get().getCenter());
    }
}
