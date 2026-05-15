package sa.edu.kau.fcit.cpit252.project;

import javafx.application.Application;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BootstrapSmokeTest {

    @Test
    void appCanBeConstructedWithoutLaunchingJavaFx() {
        assertInstanceOf(Application.class, new App());
    }

    @Test
    void mainWrapperCanBeConstructedWithoutLaunchingJavaFx() {
        assertNotNull(new Main());
    }
}
