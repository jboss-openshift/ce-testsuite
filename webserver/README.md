# CE-Testsuite - Web Servers
There are two tomcat (JWS3) versions that will be tested by this testsuite:
  - tomcat 7
  - tomcat 8

This testsuite will test all WebServer S2I application templates which are:
 
  - [jws30-tomcat7-basic-s2i.json](https://github.com/jboss-openshift/application-templates/blob/master/webserver/jws30-tomcat7-basic-s2i.json)
  - [jws30-tomcat7-https-s2i.json](https://github.com/jboss-openshift/application-templates/blob/master/webserver/jws30-tomcat7-https-s2i.json)
  - [jws30-tomcat7-mongodb-persistent-s2i.json](https://github.com/jboss-openshift/application-templates/blob/master/webserver/jws30-tomcat7-mongodb-persistent-s2i.json)
  - [jws30-tomcat7-mongodb-s2i.json](https://github.com/jboss-openshift/application-templates/blob/master/webserver/jws30-tomcat7-mongodb-s2i.json)
  - [jws30-tomcat7-mysql-persistent-s2i.json](https://github.com/jboss-openshift/application-templates/blob/master/webserver/jws30-tomcat7-mysql-persistent-s2i.json)
  - [jws30-tomcat7-mysql-s2i.json](https://github.com/jboss-openshift/application-templates/blob/master/webserver/jws30-tomcat7-mysql-s2i.json)
  - [jws30-tomcat7-postgresql-persistent-s2i.json days ago](https://github.com/jboss-openshift/application-templates/blob/master/webserver/jws30-tomcat7-postgresql-persistent-s2i.json)
  - [jws30-tomcat7-postgresql-s2i.json](https://github.com/jboss-openshift/application-templates/blob/master/webserver/jws30-tomcat7-postgresql-s2i.json)
  - [jws30-tomcat8-basic-s2i.json](https://github.com/jboss-openshift/application-templates/blob/master/webserver/jws30-tomcat8-basic-s2i.json)
  - [jws30-tomcat8-https-s2i.json](https://github.com/jboss-openshift/application-templates/blob/master/webserver/jws30-tomcat8-https-s2i.json)
  - [jws30-tomcat8-mongodb-persistent-s2i.json](https://github.com/jboss-openshift/application-templates/blob/master/webserver/jws30-tomcat8-mongodb-persistent-s2i.json)
  - [jws30-tomcat8-mongodb-s2i.json](https://github.com/jboss-openshift/application-templates/blob/master/webserver/jws30-tomcat8-mongodb-s2i.json)
  - [jws30-tomcat8-mysql-persistent-s2i.json](https://github.com/jboss-openshift/application-templates/blob/master/webserver/jws30-tomcat8-mysql-persistent-s2i.json)
  - [jws30-tomcat8-mysql-s2i.json](https://github.com/jboss-openshift/application-templates/blob/master/webserver/jws30-tomcat8-mysql-s2i.json)
  - [jws30-tomcat8-postgresql-persistent-s2i.json](https://github.com/jboss-openshift/application-templates/blob/master/webserver/jws30-tomcat8-postgresql-persistent-s2i.json)
  - [jws30-tomcat8-postgresql-s2i.json](https://github.com/jboss-openshift/application-templates/blob/master/webserver/jws30-tomcat8-postgresql-s2i.json)

For all tests, on this case we are using:
  - For templates which don't use database:
    - [Todo list jdbc](https://github.com/jboss-openshift/openshift-quickstarts/tree/master/todolist/todolist-jdbc)
    - [Todo list MongoDB](https://github.com/jboss-openshift/openshift-quickstarts/tree/master/todolist/todolist-mongodb)
  - And for those templates that don't use database:
    - [Websocket chat](https://github.com/jboss-openshift/openshift-quickstarts/tree/master/tomcat-websocket-chat)

#### How the Quickstarts applications works:
  - Todo list:
    - This application only have a home page with a little form with the **TODO's** summary and description. The tests against this application consists in add a todo list, and, for persistent databases, make sure that added items remains there after restarting/redeploying the pods (Database and Application). [Source code](https://github.com/jboss-openshift/ce-testsuite/blob/master/webserver/src/test/java/org/jboss/test/arquillian/ce/webserver/WebserverTestBase.java#L158)

  - Web Socket:
    - This application is a websocket chat application which, every message sent is broadcasted for all connected clients. To test it there is 2 clients, a sender and a recevier, the message sent by sender should be successfully received by the receiver. [Source code](https://github.com/jboss-openshift/ce-testsuite/blob/master/webserver/src/test/java/org/jboss/test/arquillian/ce/webserver/WebserverTestBase.java#L91)

### How to run the tests
The Ce-Testsuite uses the [ce-arq](https://github.com/jboss-openshift/ce-arq) which is a API that allow us to test our application templates in a running OpenshiftV3 instance. To run the tests you need:
  - Maven 3 or higher
  - Java 7 or higher
  - Openshift V3 or higher
 
The CE-Testsuite is divided by profiles, to enable the WebServer profile all you need to do is to use the following maven parameter:
```sh
-Pwebserver
```
###### Required ce-arq parameteres
  - -P**profile_name**
  - -Dkubernetes.master=**address of your running OSE instance (master node)**
  - -Dkubernetes.registry.url=**the registry address running in your ose instance**
  - -Ddocker.url=**Docker url address**
  - -Drouter.hostIP=**The OSE router IP**

###### Optional ce-arq parameteres
  - -Dtest=**The test class name, if you want to run only one test, otherwise all tests will be executed**
  - -Dkubernetes.ignore.cleanup=true **(default is false), It will ignore the resources cleanup, so you can take a look in the used pods for troubleshooting**

> **All those are java parameters, so use -D.**
___

###### Running all tests
For this example we'll consider the IP address 192.168.1.254 for required parameters, Example:
```sh
$ mvn clean install -Pwebserver -Dkubernetes.master=https://192.168.1.254:8443 -Dkubernetes.registry.url=192.168.1.254:5001 -Ddocker.url=http://192.168.1.254:2375 -Drouter.hostIP=192.168.1.254
```
###### Running a specific test and ignoring the cleanup after the tests gets finished
Example:
```sh
$ mvn clean install -Pwebserver -Dkubernetes.master=https://192.168.1.254:8443 -Dkubernetes.registry.url=192.168.1.254:5001 -Ddocker.url=http://192.168.1.254:2375 -Drouter.hostIP=192.168.1.254 -Dtest=WebServerTomcat7BasicTest -Dkubernetes.ignore.cleanup=true
```

#### What this tests cover?
This test covers all basic operations to make sure the docker image generated by the used application templates will work as expected, it will do basic operations against the deployed quickstart application, which is:
  - Todo list (Non persistent)
    - Add items in the todo list then verifies if the added items were successfully added.
  - Todo list (Persistent)
    - Add items in the todo list, restart do pods (scale up and scale down) and then check if the added itens remains there as expected.
  - Websocket chat
    - Just send a message and check if it was received by the client.

All tests above are executed using *HTTP, HTTPS, WS* and *WSS* protocols.


#### Found an issue?
Please, feel free to report the issue that you found [here](https://github.com/jboss-openshift/ce-testsuite/issues/new).

__For feedbacks please send us an email (cloud-enablement-feedback@redhat.com) and let us know what you are thinking.__ 
