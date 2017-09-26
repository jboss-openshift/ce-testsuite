package org.jboss.test.arquillian.ce.jdg.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class JdgUtils {
    public static void configureTrustStore() throws IOException {
        InputStream stream = null;

        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            stream = cl.getResourceAsStream("keystore.jks");
            Files.copy(stream, Paths.get("/tmp/keystore.jks"), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            stream.close();
        }
    }
}
