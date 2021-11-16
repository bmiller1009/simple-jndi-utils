[![Build Status](https://travis-ci.org/bmiller1009/simple-jndi-utils.svg?branch=master)](https://travis-ci.org/bmiller1009/simple-jndi-utils)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.bradfordmiller/simplejndiutils/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.bradfordmiller/simplejndiutils)
[![github: bmiller1009/simple-jndi-utils](https://img.shields.io/badge/github%3A-issues-blue.svg?style=flat-square)](https://github.com/bmiller1009/simple-jndi-utils/issues)

# simple-jndi-utils
Helper functions for the simple-jndi API

API docs were generated using javadoc and can be found [here](https://bmiller1009.github.io/simple-jndi-utils/).

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

What things you need to install the software and how to install them

* Gradle 5.4.1 or greater if you want to build from source
* JVM 8+

### Installing

#### Running with Maven

If you're using [Maven](maven.apache.org) simply specify the GAV coordinate below and Maven will do the rest

```xml
<dependency>
  <groupId>org.bradfordmiller</groupId>
  <artifactId>simplejndiutils</artifactId>
  <version>0.0.14</version>
</dependency>
```

#### Running with SBT

Add this GAV coordinate to your SBT dependency list

```sbt
libraryDependencies += "org.bradfordmiller" %% "simplejndiutils" % "0.0.14"
```

#### Running with Gradle

Add this GAV coordinate to your Gradle dependencies section

```gradle
dependencies {
    ...
    ...
    implementation 'org.bradfordmiller:simplejndiutils:0.0.14'
}
```
