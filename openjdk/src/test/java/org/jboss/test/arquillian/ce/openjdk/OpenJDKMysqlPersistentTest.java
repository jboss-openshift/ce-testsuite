package org.jboss.test.arquillian.ce.openjdk;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.ce.api.OpenShiftResource;
import org.jboss.arquillian.ce.api.OpenShiftResources;
import org.jboss.arquillian.ce.api.Template;
import org.jboss.arquillian.ce.api.TemplateParameter;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@Template(url = "file://${user.dir}/src/test/resources/openjdk18-mysql-persistent-s2i.json")
@OpenShiftResources({
    @OpenShiftResource("https://raw.githubusercontent.com/${template.repository:jboss-openshift}/application-templates/${template.branch:master}/secrets/openjdk-app-secret.json")
})

public class OpenJDKMysqlPersistentTest extends OpenJDKDatabaseTest {

    public OpenJDKMysqlPersistentTest() {
        super(true, "mysql");
    }
}
