package org.jboss.arquillian.ce.common;

import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class FailListener extends RunListener {
    public void testFailure(Failure failure) throws Exception {
        if (Boolean.getBoolean("AbortOnFirstFailure")) {
            System.err.println("CE - Aborting on first failure. REASON: " + failure);
            System.exit(-1);
        }
    }
}
