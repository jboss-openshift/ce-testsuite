package org.jboss.test.arquillian.ce.eap64;

import org.arquillian.cube.openshift.api.OpenShiftResource;
import org.arquillian.cube.openshift.api.Template;
import org.arquillian.cube.openshift.api.TemplateParameter;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.test.arquillian.ce.eap.common.EapPersistentTestBase;
import org.junit.runner.RunWith;

/**
 * @author Jonh Wendell
 */

@RunWith(Arquillian.class)
@Template(url = "https://raw.githubusercontent.com/${template.repository:jboss-openshift}/application-templates/${template.branch:master}/eap/eap64-mysql-persistent-s2i.json", parameters = {
        @TemplateParameter(name = "HTTPS_NAME", value = "jboss"),
        @TemplateParameter(name = "HTTPS_PASSWORD", value = "mykeystorepass") })
@OpenShiftResource("https://raw.githubusercontent.com/${template.repository:jboss-openshift}/application-templates/${template.branch:master}/secrets/eap-app-secret.json")
public class Eap64MysqlPersistentTest extends EapPersistentTestBase {

    @Override
    protected String[] getRCNames() {
        return new String[] {"eap-app-mysql", "eap-app"};
    }
}
