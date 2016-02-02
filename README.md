# ce-testsuite
Cloud Enablement Testsuite

e.g. to run EAP tests this is an example command

mvn clean install -Pwildfly -Dkubernetes.master=[KUBERNETES_MASTER] -Ddocker.url=[DOCKER_DEAMON_URL] 

Or JDG

mvn clean install -Pjdg -Dkubernetes.master=[KUBERNETES_MASTER]
 
