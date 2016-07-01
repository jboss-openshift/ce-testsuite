This is a spark testing module.

You can run it like so:

```
IP="10.1.236.15"
mvn clean package test -Pjdg \
-Dkubernetes.master=https://$IP:8443 \
-Dkubernetes.registry.url=$IP:5001 \
-Ddocker.url=http://$IP:2375 \
-Drouter.hostIP=$IP -Dtest=SparkTest \
-Dkubernetes.ignore.cleanup=true \
-Dsurefire.useFile=false -DtrimStackTrace=false`
```

i.e. on a VM

```
wget ftp://mirror.reverse.net/pub/apache/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.tar.gz
tar -xvf apache-maven-3.3.9-bin.tar.gz
alias mvn=/opt/jay/apache-maven-3.3.9/bin/mvn \
mvn clean package test -Pjdg -Dkubernetes.master=https://172.17.0.8:8443 \
-Dkubernetes.registry.url=https://172.17.0.8:5001 \
-Ddocker.url=https://172.17.0.8:2375 -Drouter.hostIP=172.17.0.8 \
-Dtest=SparkTest -Dkubernetes.ignore.cleanup=true \
-Dsurefire.useFile=false -DtrimStackTrace=false
```

This of course assumes you have an openshift cluster running at the above (10.1....) address, and that you have run `oc login` so that you can submit openshift pods.

This tests starts a spark cluster using the supported xpaas templates and then launches a container which is running in the same namespace.

That container can then uses DNS to connect to spark services, the web ui, launch jobs, and so on.