package org.jboss.as.ce.testsuite;

import java.util.logging.Logger;

import org.arquillian.cube.openshift.impl.client.OpenShiftClient;
import org.jboss.arquillian.ce.adapter.OpenShiftAdapter;
import org.jboss.arquillian.ce.cube.CECubeConfiguration;
import org.jboss.arquillian.core.api.annotation.Observes;

public class PrepareProject {
    private static final Logger log = Logger.getLogger(PrepareProject.class.getName());

    private static final String DEFAULT_ROLE_REF = "view";

    public void prepareProject(@Observes(precedence = 10) OpenShiftAdapter adapter, CECubeConfiguration configuration, OpenShiftClient client) throws Exception {
        String namespace = client.getClientExt().getNamespace();
        final String DEFAULT_USER_NAME = "system:serviceaccount:" + namespace + ":default";
        log.info("Adding role " + DEFAULT_ROLE_REF + " to " + DEFAULT_USER_NAME);
        adapter.addRoleBinding("rolebinding", DEFAULT_ROLE_REF, DEFAULT_USER_NAME);
    }
}
