# CE-Testsuite - A-MQ

This testsuite will test all A-MQ S2I application templates which are:
 
  - [amq62-basic.json](https://github.com/jboss-openshift/application-templates/blob/master/amq/amq62-basic.json)
  - [amq62-persistent-ssl.json](https://github.com/jboss-openshift/application-templates/blob/master/amq/amq62-persistent-ssl.json)
  - [amq62-persistent.json](https://github.com/jboss-openshift/application-templates/blob/master/amq/amq62-persistent.json)
  - [amq62-ssl.json](https://github.com/jboss-openshift/application-templates/blob/master/amq/amq62-ssl.json)
  
In addition, three other tests will show some of the features that can be used with A-MQ:

  - A-MQ Clustering with Mesh
  - External Non-OpenShift clients accessing A-MQ
  - Source-to-Image to change default configuration

The A-MQ application templates does not use quickstart applications, it only register Queues and Topics. This tests register the following:
  - **Queues**:
    - QUEUES.FOO
    - QUEUES.BAR
  - **Topics**:
    - TOPICS.FOO
    - TOPICS.BAR

#### How this test works:
The queues and topics are created at the moment the tests starts, example:
```java
        @TemplateParameter(name = "MQ_QUEUES", value = "QUEUES.FOO,QUEUES.BAR"),
        @TemplateParameter(name = "MQ_TOPICS", value = "TOPICS.FOO,TOPICS.BAR"),
```
The tests basically consists in send e consume messages using the following protocols:
  - [Openwire](http://activemq.apache.org/openwire.html)
  - [AMQP - Advanced Message Queuing Protocol](https://www.amqp.org/)
  - [Mqtt](http://mqtt.org/)
  - [Stomp](https://stomp.github.io/)

### How to run the tests
The Ce-Testsuite uses the [ce-arq](https://github.com/jboss-openshift/ce-arq) which is a API that allow us to test our application templates in a running OpenshiftV3 instance. To run the tests you need:
  - Maven 3 or higher
  - Java 7 or higher
  - Openshift V3 or higher
 
The CE-Testsuite is divided by profiles, to enable the **amq** profile all you need to do is to use the following maven parameter:
```sh
-Pamq
```
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

###### Running all tests
For this example we'll consider the IP address 192.168.1.254 for required parameters, Example:
```sh
$ mvn clean package test -Pamq -Dkubernetes.master=https://192.168.1.254:8443 -Dkubernetes.registry.url=192.168.1.254:5001 -Ddocker.url=http://192.168.1.254:2375
```
###### Running a specific test and ignoring the cleanup after the tests gets finished
Example:
```sh
$ mvn clean package test -Pamq -Dkubernetes.master=https://192.168.1.254:8443 -Dkubernetes.registry.url=192.168.1.254:5001 -Ddocker.url=http://192.168.1.254:2375 -Dtest=testOpenWireConnection -Dkubernetes.ignore.cleanup=true
```

#### What this tests cover?
The A-MQ tests will produces and consume messages using the Openwire, mqtt, amqp and stomp protocols.

#### Found an issue?
Please, feel free to report the issue that you found [here](https://github.com/jboss-openshift/ce-testsuite/issues/new).

__For feedbacks please send us an email (cloud-enablement-feedback@redhat.com) and let us know what you are thinking.__ 
