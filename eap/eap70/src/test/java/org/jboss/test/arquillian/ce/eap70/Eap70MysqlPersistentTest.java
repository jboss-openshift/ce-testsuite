package org.jboss.test.arquillian.ce.eap70;

import org.jboss.arquillian.ce.api.OpenShiftResource;
import org.jboss.arquillian.ce.api.Template;
import org.jboss.arquillian.ce.api.TemplateParameter;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.test.arquillian.ce.eap.common.EapPersistentTestBase;
import org.junit.runner.RunWith;

/**
 * @author Marko Luksa
 */
@RunWith(Arquillian.class)
@Template(url = "https://raw.githubusercontent.com/${template.repository:jboss-openshift}/application-templates/${template.branch:master}/eap/eap70-mysql-persistent-s2i.json", parameters = {
        @TemplateParameter(name = "HTTPS_NAME", value = "jboss"),
        @TemplateParameter(name = "HTTPS_PASSWORD", value = "mykeystorepass")})
@OpenShiftResource("https://raw.githubusercontent.com/${template.repository:jboss-openshift}/application-templates/${template.branch:master}/secrets/eap7-app-secret.json")
public class Eap70MysqlPersistentTest extends EapPersistentTestBase {

    @Override
    protected String[] getRCNames() {
        return new String[] {"eap-app-mysql", "eap-app"};
    }
}
