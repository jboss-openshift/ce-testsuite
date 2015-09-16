# ce-testsuite
Cloud Enablement Testsuite

e.g. to run EAP tests this is an example command

mvn clean install -Pwildfly 

-Dkubernetes.master=[KUBERNETES_MASTER] 

-Ddocker.url=[DOCKER_DEAMON_URL] 

-Dkubernetes.certs.client.file=[CLIENT_FILE] 

-Dkubernetes.certs.client.key.file=[CLIENT_KEY_FILE] 

-Dkubernetes.certs.ca.file=[CA_FILE] 

-Dfrom.parent=ce-registry.usersys.redhat.com/jboss-eap-6/eap-openshift:6.4-289 

-Ddocker.test.namespace=[DOCKER_NAMESPACE] 

-Dkubernetes.namespace=[OSE_NAMESPACE] 

-Dkubernetes.container.pre-stop-ignore=true 
