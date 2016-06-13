This is a spark testing module.

You can run it like so:

`mvn clean package test -Pspark -Dkubernetes.master=https://ce-os-rhel-master.usersys.redhat.com:8443 -Dkubernetes.registry.url=https://ce-os-rhel-master.usersys.redhat.com:5001 -Ddocker.url=https://ce-os-rhel-master.usersys.redhat.com:2375 -Dtest=SparkTest -Dkubernetes.ignore.cleanup=true -Dsurefire.useFile=false -DtrimStackTrace=false  -Dkubernetes.namespace=my-ns-for-testing`

This of course assumes you have an openshift cluster running at the above (10.1....) address, and that you have run `oc login` so that you can submit openshift pods.

That container can then uses DNS to connect to spark services, the web ui, launch jobs, and so on.
