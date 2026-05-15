package sa.edu.kau.fcit.cpit252.project.api;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EnvConfigTest {

    @Test
    void getReturnsDefaultValueWhenKeyIsMissing() {
        String missingKey = "STOX_TEST_MISSING_KEY_" + System.nanoTime();

        assertEquals("fallback", EnvConfig.get(missingKey, "fallback"));
    }

    @Test
    void getReturnsValueFromLoadedPropertiesBeforeDefault() throws Exception {
        Properties properties = getProperties();
        String key = "STOX_TEST_PROPERTY_KEY";
        properties.setProperty(key, "configured-value");

        try {
            assertEquals("configured-value", EnvConfig.get(key, "fallback"));
        } finally {
            properties.remove(key);
        }
    }

    @Test
    void getTreatsBlankPropertyValueAsMissing() throws Exception {
        Properties properties = getProperties();
        String key = "STOX_TEST_BLANK_PROPERTY_KEY_" + System.nanoTime();
        properties.setProperty(key, "   ");

        try {
            assertEquals("fallback", EnvConfig.get(key, "fallback"));
        } finally {
            properties.remove(key);
        }
    }

    @Test
    void envConfigCanBeConstructed() {
        assertNotNull(new EnvConfig());
    }

    private Properties getProperties() throws Exception {
        Field field = EnvConfig.class.getDeclaredField("properties");
        field.setAccessible(true);
        return (Properties) field.get(null);
    }
}
