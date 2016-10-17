# Cloud Enablement Testsuite - ce-testsuite

#### Before starting
Make sure you have the ce-arq dependencies available in your local maven repository. If you don't have it yet just clone the project and install it:

```sh
$ git clone https://github.com/jboss-openshift/ce-arq.git

$ cd ce-arq

$ mvn clean install
```


For more details about each test, please verify its README.md file.

# Setting up your machine to build and run tests.

First, make sure you have an openshift instance running that you can log into, these

tests execute against an externally running openshift cluster.

Next, build the prerequisite projects:

```
git clone https://github.com/arquillian/arquillian-cube.git
cd arquillian-cube
mvn clean install -DskipTests=true
cd ../
git clone https://github.com/jboss-openshift/ce-arq.git
cd ce-arq
mvn clean install
cd ../
```

Then, download an openshift client (https://access.redhat.com/documentation/en/openshift-enterprise/3.2/cli-reference/chapter-2-get-started-with-the-cli).

And run `oc login $yourhost`, if the login succeeds, then you're ready to run the suite of arquillian tests.

# Running the tests

Then, you can follow individual instructions in different test directories.

In general, each test consists of a mvn command, where the unit tests execute locally and use local openshift login which was executed in the above section.

#### Found an issue?
Please, feel free to report the issue that you found [here](https://github.com/jboss-openshift/ce-testsuite/issues/new).

__For feedbacks please send us an email (cloud-enablement-feedback@redhat.com) and let us know what you are thinking.__
