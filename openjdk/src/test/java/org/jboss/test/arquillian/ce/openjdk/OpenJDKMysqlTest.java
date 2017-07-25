package org.jboss.test.arquillian.ce.openjdk;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.ce.api.OpenShiftResource;
import org.jboss.arquillian.ce.api.OpenShiftResources;
import org.jboss.arquillian.ce.api.Template;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@Template(url = "file://${user.dir}/src/test/resources/openjdk18-mysql-s2i.json")
@OpenShiftResources({
    @OpenShiftResource("classpath:openjdk-app-secret.json")
})

public class OpenJDKMysqlTest extends OpenJDKDatabaseTest {

    public OpenJDKMysqlTest() {
        super(false, "mysql");
    }
}
