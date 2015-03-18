
This application can deploy a MongoDB shard cluster in Google Compute Engine using different disk and instance strategies, including micro-sharding. You can run different YCSB load test against your cluster. The application will show the GCE operations and the YCSB test results in a reactive way using WebSockets.

Components
==========

- Typesafe Activator
- Akka
- Spring 4
- Java 8
- Google Java API
- JSch
- Log4j
- YCSB
- Puppet
- JQuery
- Bootstrap 2.3

Installation
============

```
  git clone https://github.com/RicardoLorenzo/MongoDBClusterTool.git
```

Create you Google GCE configurationi in conf/google.conf.

```
  google.clientId="<google client id>"
  google.clientEmail="<google service email>"
  google.clientSecret=<google secretkey>
  application.directory = /Users/ricardolorenzo/Development/MongoDBClusterTool/store
  google.projectId=<google project name>
  google.bucketId=<google cloud storage bucket>
  google.zoneName=us-central1-b
```

You should get this info from your GCE console `https://console.developers.google.com/project/<project>/apiui/credential`.

Finally you can run the application with the following command

```
  cd MongoDBClusterTool
  ./activator run
```

Supported browsers
==================

This application supports modern browsers only.

- Chrome
- Firefox

Known issues
============

The current version creates a MongoDB sharded cluster without a replica set, because is intended to achieve maximum throughput instead of building a reliable data store.


