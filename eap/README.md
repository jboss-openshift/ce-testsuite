# CE-Testsuite - JBoss Enterprise Application Server (EAP)

The EAP testsuite is subdivided in three subprojects:
  - eap64
  - eap7
  - integration

### How to run the tests
The Ce-Testsuite uses the [ce-arq](https://github.com/jboss-openshift/ce-arq) which is a API that allow us to test our application templates in a running OpenshiftV3 instance. To run the tests you need:
  - Maven 3 or higher
  - Java 7 or higher
  - Openshift V3 or higher

###### Required ce-arq parameteres
  - -P**profile_name**
  - -Dkubernetes.master=**address of your running OSE instance (master node)**
  - -Dkubernetes.registry.url=**the registry address running in your ose instance**
  - -Ddocker.url=**Docker url address**

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
-Peap64
```
###### Running all tests
For this example we'll consider the IP address 192.168.1.254 for required parameters, Example:
```sh
$ mvn clean package test -Peap64 -Dkubernetes.master=https://192.168.1.254:8443 -Dkubernetes.registry.url=192.168.1.254:5001 -Ddocker.url=http://192.168.1.254:2375
```
###### Running a specific test and ignoring the cleanup after the tests gets finished
Example:
```sh
$ mvn clean package test -Peap64 -Dkubernetes.master=https://192.168.1.254:8443 -Dkubernetes.registry.url=192.168.1.254:5001 -Ddocker.url=http://192.168.1.254:2375 -Dtest=Eap64BasicTest -Dkubernetes.ignore.cleanup=true
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

The CE-Testsuite is divided by profiles, to enable the **eap64** profile all you need to do is to use the following maven parameter:
```sh
-Peap7
```
###### Running all tests
For this example we'll consider the IP address 192.168.1.254 for required parameters, Example:
```sh
$ mvn clean package test -Peap7-Dkubernetes.master=https://192.168.1.254:8443 -Dkubernetes.registry.url=192.168.1.254:5001 -Ddocker.url=http://192.168.1.254:2375
```
###### Running a specific test and ignoring the cleanup after the tests gets finished
Example:
```sh
$ mvn clean package test -Peap7 -Dkubernetes.master=https://192.168.1.254:8443 -Dkubernetes.registry.url=192.168.1.254:5001 -Ddocker.url=http://192.168.1.254:2375 -Dtest=Eap7BasicTest -Dkubernetes.ignore.cleanup=true
```

___
#### Integration
TODO

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
