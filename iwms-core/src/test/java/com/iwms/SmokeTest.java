package com.iwms;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SmokeTest {

    @Test
    @DisplayName("Verify testing framework and Java runtime environment")
    void testRuntimeEnvironment() {
        String javaVersion = System.getProperty("java.version");
        assertTrue(javaVersion != null && !javaVersion.isBlank(), "Java version must be detected");
    }
}
