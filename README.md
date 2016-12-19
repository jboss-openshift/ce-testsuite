# Cloud Enablement Testsuite - ce-testsuite

#### Requirements
 - Java 7 or higher.
 - Maven 3 or higher.
 
 
#### Before starting
As a first step, make sure you have the ce-arq dependency available in your local maven repository. If you don't have it yet, just clone the project and install it:

```sh
$ git clone https://github.com/jboss-openshift/ce-arq.git
$ cd ce-arq
$ mvn clean install
```

The above commands will generate the required ce-arq dependency and install it on your local maven repository.

The ce-arq project, in turn, depends on arquillian-cube project. At the moment, we are using the SNAPHOST version, which means you will have to build it locally in order to satisfy this dependency.
To build it, please follow the steps below:

```sh
git clone https://github.com/arquillian/arquillian-cube.git
cd arquillian-cube
mvn clean install -DskipTests
```

PS: The step above might not be necessary in the future, since we will use released versions once all the changes we need are made available in Arquillian Cube.


#### Useful System Properties configurations

Sometimes, one might want to run more than one test at the same time but you probably will get an error saying the **Address already in use** that means there is another test in progress or some port 
used is being used by another process. To solve this issue you can easily use the following [System Property](https://docs.oracle.com/javase/tutorial/essential/environment/sysprop.html)
 to configure the Arquillian Cube to use a different bind address to start the port forwarder server, in this way you can start multiple tests by setting the following property:
- -Darq.extension.openshift.portForwardBindAddress=\<LOCAL_IP_ADDRESS\>

*Note that the default bind address is **localhost**.*

# Persistent Volumes and Persistent Volume Claims

Some tests make use of the Persistent Volumes feature of Kubernetes/OpenShift. It is out of the tests' scope, however, to setup such PV's. This is left to the cluster administrator as a requirement for running the whole suite of tests.

Those tests just use the PersistentVolumeClaims to claim for some volumes. [Check the docs](https://docs.openshift.org/latest/dev_guide/persistent_volumes.html) for more information on how to configure Persistent Volumes.

You can, if you prefer, disable such tests temporarily, by [excluding them within pom.xml](http://maven.apache.org/surefire/maven-surefire-plugin/examples/inclusion-exclusion.html). Those tests have the word `Persistent` into their names, like, for instance: `Eap70MysqlPersistentTest.java`.

For more details about each component of this testsuite, please verify their docs:

 - [AMQ](https://github.com/jboss-openshift/ce-testsuite/blob/master/amq/README.md)
 - [EAP and EAP Integration Tests](https://github.com/jboss-openshift/ce-testsuite/blob/master/eap/README.md)
 - [JDG](https://github.com/jboss-openshift/ce-testsuite/blob/master/jdg/jdg65/README.md)
 - KIE SERVER:
    - [6.2](https://github.com/jboss-openshift/ce-testsuite/blob/master/kieserver/62/README.md)
    - [6.3](https://github.com/jboss-openshift/ce-testsuite/blob/master/kieserver/63/README.md)
 - [SPARK](https://github.com/jboss-openshift/ce-testsuite/blob/master/spark/README.md)
 - [SSO](https://github.com/jboss-openshift/ce-testsuite/blob/master/sso/README.md)
 - [WEBSERVER](https://github.com/jboss-openshift/ce-testsuite/blob/master/webserver/README.md)

#### Found an issue?
Feel free to report it [here](https://github.com/jboss-openshift/ce-testsuite/issues/new).

__For any feedback, please send us an email (cloud-enablement-feedback@redhat.com) and let us know about your thoughts.__
