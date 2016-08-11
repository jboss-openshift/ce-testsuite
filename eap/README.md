# CE-Testsuite - JBoss Enterprise Application Server (EAP)

The EAP testsuite is subdivided in three subprojects:
  - eap64
  - eap70
  - integration

### How to run the tests
The Ce-Testsuite uses the [ce-arq](https://github.com/jboss-openshift/ce-arq) which is a API that allow us to test our application templates in a running OpenshiftV3 instance. To run the tests you need:
  - Maven 3 or higher
  - Java 7 or higher
  - Openshift V3 or higher

###### Required ce-arq parameteres
  - -P**profile_name**
    - -Pjdg
  - -Dkubernetes.master=**address of your running OSE instance (master note)**
    - -Dkubernetes.master=https://openshift-master.mydomain.com:8443
  - -Dkubernetes.registry.url=**the registry address running in your ose instance**
    - -Dkubernetes.registry.url=openshift-master.mydomain.com:5001
  - -Ddocker.url=**Docker url address**
    - -Ddocker.url=https://openshift-master.mydomain.com2375
  - -Drouter.hostIP=**The OSE router IP**
    - -Drouter.hostIP=192.168.1.254
      - You can change this parameter name in the pom.xml
      - For EAP 7, this parameter was changed to **router.host**


###### Optional ce-arq parameteres
  - -Dtest=**The test class name, if you want to run only one test, otherwise all tests will be executed**
  - -Dkubernetes.ignore.cleanup=true **(default is false), It will ignore the resources cleanup, so you can take a look in the used pods for troubleshooting**

> **All those are java parameters, so use -D.**
___

#### EAP 6.4 (eap64)
This testsuite will test all JBoss EAP 6.4 S2I application templates which are:
 
  - [eap64-amq-persistent-s2i.json](https://github.com/jboss-openshift/application-templates/blob/master/eap/eap64-amq-persistent-s2i.json)
  - [eap64-amq-s2i.json	CLOUD-517](https://github.com/jboss-openshift/application-templates/blob/master/eap/eap64-amq-s2i.json)
  - [eap64-basic-s2i.json](https://github.com/jboss-openshift/application-templates/blob/master/eap/eap64-basic-s2i.json)
  - [eap64-https-s2i.json](https://github.com/jboss-openshift/application-templates/blob/master/eap/eap64-https-s2i.json)
  - [eap64-mongodb-persistent-s2i.json](https://github.com/jboss-openshift/application-templates/blob/master/eap/eap64-mongodb-persistent-s2i.json)
  - [eap64-mongodb-s2i.json](https://github.com/jboss-openshift/application-templates/blob/master/eap/eap64-mongodb-s2i.json)
  - [eap64-mysql-persistent-s2i.json](https://github.com/jboss-openshift/application-templates/blob/master/eap/eap64-mysql-persistent-s2i.json)
  - [eap64-mysql-s2i.json](https://github.com/jboss-openshift/application-templates/blob/master/eap/eap64-mysql-s2i.json)
  - [eap64-postgresql-persistent-s2i.json](https://github.com/jboss-openshift/application-templates/blob/master/eap/eap64-postgresql-persistent-s2i.json)
  - [eap64-postgresql-s2i.json](https://github.com/jboss-openshift/application-templates/blob/master/eap/eap64-postgresql-s2i.json)
  - [eap64-sso-s2i.json](https://github.com/jboss-openshift/application-templates/blob/master/eap/eap64-sso-s2i.json)

### Quickstarts used:
For the EAP 6.4 tests the following quickistarts are used:
  - [helloworld-mdb](https://github.com/jboss-developer/jboss-eap-quickstarts/tree/6.4.x/helloworld-mdb)
  - [kitchensink](https://github.com/jboss-developer/jboss-eap-quickstarts/tree/6.4.x/kitchensink)
  - [todolist-mongodb](https://github.com/jboss-openshift/openshift-quickstarts/tree/master/todolist/todolist-mongodb)
  - [todolist-jdbc](https://github.com/jboss-openshift/openshift-quickstarts/tree/master/todolist/todolist-jdbc)
  - [keycloak-examples](https://github.com/keycloak/keycloak-examples/tree/0.4-openshift)

The CE-Testsuite is divided by profiles, to enable the **eap64** profile all you need to do is to use the following maven parameter:
```sh
-Peap,eap64
```
###### Running all tests
For this example we'll consider the IP address 192.168.1.254 for required parameters, Example:
```sh
$ mvn clean package test -Peap,eap64 -Dkubernetes.master=https://192.168.1.254:8443 -Dkubernetes.registry.url=192.168.1.254:5001 -Ddocker.url=http://192.168.1.254:2375 -Drouter.hostIP=192.168.1.254
```
###### Running a specific test and ignoring the cleanup after the tests gets finished
Example:
```sh
$ mvn clean package test -Peap,eap64 -Dkubernetes.master=https://192.168.1.254:8443 -Dkubernetes.registry.url=192.168.1.254:5001 -Ddocker.url=http://192.168.1.254:2375 -Drouter.hostIP=192.168.1.254 -Dtest=Eap64BasicTest -Dkubernetes.ignore.cleanup=true
```
___

#### EAP 7 (eap7)
This testsuite will test all JBoss EAP 7 S2I application templates which are:

  - [eap70-basic-s2i.json](https://github.com/jboss-openshift/application-templates/blob/master/eap/eap70-basic-s2i.json)
  - [eap70-https-s2i.json](https://github.com/jboss-openshift/application-templates/blob/master/eap/eap70-https-s2i.json)
  - [eap70-mongodb-persistent-s2i.json](https://github.com/jboss-openshift/application-templates/blob/master/eap/eap70-mongodb-persistent-s2i.json)
  - [eap70-mongodb-s2i.json](https://github.com/jboss-openshift/application-templates/blob/master/eap/eap70-mongodb-s2i.json)
  - [eap70-mysql-persistent-s2i.json](https://github.com/jboss-openshift/application-templates/blob/master/eap/eap70-mysql-persistent-s2i.json)
  - [eap70-mysql-s2i.json](https://github.com/jboss-openshift/application-templates/blob/master/eap/eap70-mysql-s2i.json)
  - [eap70-postgresql-persistent-s2i.json](https://github.com/jboss-openshift/application-templates/blob/master/eap/eap70-postgresql-persistent-s2i.json)
  - [eap70-postgresql-s2i.json](https://github.com/jboss-openshift/application-templates/blob/master/eap/eap70-postgresql-s2i.json)
  
### Quickstarts used:
For the EAP 7 tests the following quickistarts are used:
  - [kitchensink](https://github.com/jboss-developer/jboss-eap-quickstarts/tree/7.0.x-develop/kitchensink)
  - [todolist-mongodb](https://github.com/jboss-openshift/openshift-quickstarts/tree/master/todolist/todolist-mongodb)
  - [todolist-jdbc](https://github.com/jboss-openshift/openshift-quickstarts/tree/master/todolist/todolist-jdbc)

The CE-Testsuite is divided by profiles, to enable the **eap70** profile all you need to do is to use the following maven parameter:
```sh
-Peap,eap70
```
###### Running all tests
For this example we'll consider the IP address 192.168.1.254 for required parameters, Example:
```sh
$ mvn clean package test -Peap,eap70 -Dkubernetes.master=https://192.168.1.254:8443 -Dkubernetes.registry.url=192.168.1.254:5001 -Ddocker.url=http://192.168.1.254:2375 -Drouter.host=192.168.1.254
```
###### Running a specific test and ignoring the cleanup after the tests gets finished
Example:
```sh
$ mvn clean package test -Peap,eap70 -Dkubernetes.master=https://192.168.1.254:8443 -Dkubernetes.registry.url=192.168.1.254:5001 -Ddocker.url=http://192.168.1.254:2375 -Drouter.host=192.168.1.254 -Dtest=Eap70BasicTest -Dkubernetes.ignore.cleanup=true
```

___
#### Integration (EAP6 & EAP7)
EAP integration will run all integration tests from EAP project.
The reason to perform these tests is to make sure all of basic EAP's functionality are properly working in a containerized envorironment.
For this tests we are using EAP 6.4.5 and EAP 7.0.0.GA
To be able to run this tests you may have to download the [source coude](https://access.redhat.com/jbossnetwork/restricted/softwareDetail.html?softwareId=40901&product=appplatform&version=6.4&downloadType=patches) and build the needed dependencies. To build the dependencies please follow the steps below:

**Note**, remember to choose the correct version of EAP 6 or 7.

##### Enable the test-jar in the testsuite sub-projects:
Add the following content on **EAP_SRC/testsuite/integration/pom.xml**
```java
        <plugins>
           <plugin>
               <groupId>org.apache.maven.plugins</groupId>
               <artifactId>maven-jar-plugin</artifactId>
               <version>2.2</version>
               <executions>
                   <execution>
                       <goals>
                           <goal>test-jar</goal>
                       </goals>
                   </execution>
               </executions>
           </plugin>
...
```

##### Build the testsuite sub-project:

Before build and install the needed jars, it is necessary do some modifications in the EAP testsuite:

To make the JBoss CLI tests work, we need to change the jboss-as-testsuite-shared to configure the CLI username and password which will be used to ran the tests. To do that edit the **org.jboss.as.test.integration.management.util.CLITestUtil.java** like below:

###### Create the variables username and password (after line 46):
```java
    private static String username = System.getProperty("jboss.cli.username");
    private static char[] password = System.getProperty("jboss.cli.password").toCharArray();
```

###### Edit the method getCommandContext():
```java
    public static CommandContext getCommandContext() throws CliInitializationException {
        setJBossCliConfig();
        return CommandContextFactory.getInstance().newCommandContext(serverAddr, serverPort, username, password);
    }
```

###### Edit the method getCommandContext(OutputStream out):
```java
    public static CommandContext getCommandContext(OutputStream out) throws CliInitializationException {
        SecurityActions.setSystemProperty(JREADLINE_TERMINAL, JREADLINE_TEST_TERMINAL);
        setJBossCliConfig();
        return CommandContextFactory.getInstance().newCommandContext(serverAddr, serverPort, username, password, null, out);
    }
```

#### Building the testsuite project:
Example:
```sh
cd $EAP_SRC/testsuite && mvn clean install -DskipTests
```
After to execute tue command above, do it:
```sh
cd $EAP_SRC/testsuite/integration/basic && mvn clean install -DskipTests
```

The above command will generate the required jars, which are:

  - $EAP_SRC/testsuite/integration/basic/target/jboss-as-ts-integ-basic-7.5.5.Final-redhat-SNAPSHOT-tests.jar
  - $EAP_SRC/testsuite/integration/smoke/target/jboss-as-ts-integ-smoke-7.5.5.Final-redhat-SNAPSHOT-tests.jar
 
 **Note** for EAP7 the file name will start with **wildfly-**, example: **wildfly-ts-integ-basic-7.0.0.GA-redhat-2-tests.jar**. If you are building EAP7 remember to change the file name in the steps below.

 
### Install the jars manually:

Before installing the jars, is necessary add a user in the **jboss-ejb-client.properties** in the following file: 
**$EAP_SRC/testsuite/integration/smoke/target/jboss-as-ts-integ-basic-7.5.5.Final-redhat-SNAPSHOT-tests.jar** 
and add the following content:

```sh
remote.connection.default.username=guest
remote.connection.default.password=guest
```

For eap 7 so the same as described above in the **wildfly-ts-integ-ws-7.0.0.GA-redhat-2-tests.jar** file.

After change the sources as explained above, build the **jboss-as-testsuite-shared** sources:
```sh
$ cd $EAP_SRC/testsuite/shared
$ mvn clean install -DskipTests
```

And the **model-test** sources (Not needed on EAP6):
Note, on EAP7 this sub-project is located under **jboss-eap-7-core-src**
```sh
$ cd $EAP_SRC/model-test
$ mvn clean install -DskipTests
```

After all steps above install the needed jars:

```sh
mvn install:install-file -Dfile=/sources/jboss-eap-6.4.5-src/testsuite/integration/basic/target/jboss-as-ts-integ-basic-7.5.5.Final-redhat-SNAPSHOT-tests.jar -DgroupId=org.jboss.as -DartifactId=jboss-as-ts-integ-basic -Dversion=7.5.5.Final-redhat-SNAPSHOT -Dpackaging=test-jar
mvn install:install-file -Dfile=/sources/jboss-eap-6.4.5-src/testsuite/integration/smoke/target/jboss-as-ts-integ-smoke-7.5.5.Final-redhat-SNAPSHOT-tests.jar -DgroupId=org.jboss.as -DartifactId=jboss-as-ts-integ-smoke -Dversion=7.5.5.Final-redhat-SNAPSHOT -Dpackaging=test-jar
```

For EAP 7, we also need to install the dependencies below:
```sh
mvn install:install-file -Dfile=/sources/jboss-eap-7.0-src/testsuite/integration/web/target/wildfly-ts-integ-web-7.0.0.GA-redhat-2-tests.jar -DgroupId=org.jboss.eap -DartifactId=wildfly-ts-integ-web -Dversion=7.0.0.GA-redhat-2 -Dpackaging=test-jar
mvn install:install-file -Dfile=/sources/jboss-eap-7.0-src/testsuite/integration/ws/target/wildfly-ts-integ-ws-7.0.0.GA-redhat-2-tests.jar -DgroupId=org.jboss.eap -DartifactId=wildfly-ts-integ-ws -Dversion=7.0.0.GA-redhat-2 -Dpackaging=test-jar
```

At this moment we are ready to start the tests, to start it use the following command:
```sh
cd ce-testsuite/eap/integration/eap6|eap7
mvn clean test -Peap6|eap7 -Dkubernetes.master=https://openshift-master.mydomain.com:8443 -Ddocker.url=http://openshift-docker.mydomain.com:2375
```

If you want to execute a single integration test you need to add the extra test which will prepare the container to run the tests, example:

```sh
cd ce-testsuite/eap/integration/eap6|eap7
mvn clean test -Peap6|eap7 -Dkubernetes.master=https://openshift-master.mydomain.com:8443 -Ddocker.url=http://openshift-docker.mydomain.com:2375 -Dtest=<some test you whish to run>
```

If you are going to use a newer EAP version, remember to change parent pom.xml according EAP version that you are using:
```java
<version.eap>7.5.5.Final-redhat-SNAPSHOT</version.eap>
<version.eap7>7.0.0.GA-redhat-2</version.eap7>
```
Also remember to change the EAP related stuff versions like CLI.

___
 
#### How this test works:
Each application template may use a different quickstart. Basically the tests will do basic operations depending of the what quickstart are being used:
  - todolist: This application only have a home page with a little form with the **TODO's** summary and description. The tests against this application consists in add a todo list, and, for persistent databases, make sure that added items remains there after restarting/redeploying the pods (Database and Application)
  - kitchensink: for more information please see this [link](https://github.com/jboss-developer/jboss-eap-quickstarts/blob/7.0.x-develop/kitchensink/README.md)
  - SSO: for more information please see this [link](https://github.com/jboss-openshift/ce-testsuite/tree/master/sso)


#### What this tests cover?
This test covers all basic operations to make sure the docker image generated by the used application templates will work as expected, it will do basic operations against the deployed quickstart application, which is:
  - Todo list (Non persistent)
    - Add items in the todo list then verifies if the added items were successfully added.
  - Todo list (Persistent)
    - Add items in the todo list, restart do pods (scale up and scale down) and then check if the added itens remains there as expected.
  - kitchensink: add a new contact and verifies if it was successfully added.
  
All tests above are executed using HTTP and HTTPS protocols.


#### Found an issue?
Please, feel free to report the issue that you found [here](https://github.com/jboss-openshift/ce-testsuite/issues/new).

__For feedbacks please send us an email (cloud-enablement-feedback@redhat.com) and let us know what you are thinking.__ 