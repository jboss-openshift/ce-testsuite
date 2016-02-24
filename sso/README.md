#Arquillian Tests for Red Hat Single-Sign On (SSO)
This project contains OpenShift v3 tests for the Red Hat SSO/Keycloak image
and the SSO/Keycloak capabilities of the EAP image

##Requirements
The following are required to run the tests:
 * ImageStreams currently pull the SSO and SSO-enabled EAP images from docker-registry.usersys.redhat.com so this registry must be enabled for the Docker service
 * The system property "openshift.domain" is configured in pom.xml (default ".openshift"). This must be DNS resolvable
