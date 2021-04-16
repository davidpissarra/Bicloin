# A59-Bicloin

Distributed Systems 2020-2021, 2nd semester project


## Authors

**Group A59**

92502 [Jorge Godinho](jorge.godinho@tecnico.ulisboa.pt)

92448 [David Pissarra](david.pissarra@tecnico.ulisboa.pt)

92426 [António Branco](pedro.paredes.branco@tecnico.ulisboa.pt)

## Getting Started

The overall system is composed of multiple modules.

See the project statement for a full description of the domain and the system.

### Prerequisites

Java Developer Kit 11 is required running on Linux, Windows or Mac.
Maven 3 is also required.

To confirm that you have them installed, open a terminal and type:

```
javac -version

mvn -version
```

### Installing

To compile and install all modules:

```
mvn clean install -DskipTests
```

The integration tests are skipped because they require theservers to be running.


## Built With

* [Maven](https://maven.apache.org/) - Build Tool and Dependency Management
* [gRPC](https://grpc.io/) - RPC framework


## Versioning

We use [SemVer](http://semver.org/) for versioning. 
