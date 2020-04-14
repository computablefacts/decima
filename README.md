# Decima

![Maven Central](https://img.shields.io/maven-central/v/com.computablefacts/decima)
[![Build Status](https://travis-ci.com/computablefacts/decima.svg?branch=master)](https://travis-ci.com/computablefacts/decima)
[![codecov](https://codecov.io/gh/computablefacts/decima/branch/master/graph/badge.svg)](https://codecov.io/gh/computablefacts/decima)

Decima is a proof-of-concept Java implementation of the probabilistic logic programming language [ProbLog](https://dtai.cs.kuleuven.be/problog).

This library embeds a Java port of the C# library [BDDSharp](https://github.com/ancailliau/BDDSharp) (under [MIT licence](https://opensource.org/licenses/mit-license.php)). 
BDDSharp is a library for manipulating [ROBDDs](https://en.wikipedia.org/wiki/Binary_decision_diagram) (Reduced Ordered 
Binary Decision Diagrams). A good overview of Binary Decision Diagrams can be found in [Lecture Notes on Binary Decision Diagrams](https://www.cs.cmu.edu/~fp/courses/15122-f10/lectures/19-bdds.pdf)
by Frank Pfenning.

## Usage

ProbLog is a redesign and new implementation of Prolog in which facts and rules can be annotated with probabilities 
(ProbLog makes the assumption that all probabilistic facts are mutually independent) by adding a floating-point number 
in front of the fact/rule followed by double-colons (from [ProbLog's site](https://dtai.cs.kuleuven.be/problog/tutorial/basic/05_smokers.html)) :

```
0.3::stress(X) :- person(X).
0.2::influences(X, Y) :- person(X), person(Y).

smokes(X) :- stress(X).
smokes(X) :- friend(X, Y), influences(Y, X), smokes(Y).

0.4::asthma(X) :- smokes(X).

person(éléana).
person(jean).
person(pierre).
person(alexis).

friend(jean, pierre).
friend(jean, éléana).
friend(jean, alexis).
friend(éléana, pierre).
```

The program above encodes a variant of the "Friends & Smokers" problem. The first two rules state that there are two 
possible causes for a person X to smoke, namely X having stress, and X having a friend Y who smokes himself and 
influences X. Furthermore, the program encodes that if X smokes, X has asthma with probability 0.4.

It is then possible to calculates the probability of the various people smoking and having asthma :

```
smokes(éléana)?
0.342

smokes(jean)?
0.42556811
```

## Adding Decima to your build

Decima's Maven group ID is `com.computablefacts` and its artifact ID is `decima`.

To add a dependency on Decima using Maven, use the following:

```xml
<dependency>
  <groupId>com.computablefacts</groupId>
  <artifactId>decima</artifactId>
  <version>1.x</version>
</dependency>
```

## Snapshots 

Snapshots of Decima built from the `master` branch are available through Sonatype 
using the following dependency:

```xml
<dependency>
  <groupId>com.computablefacts</groupId>
  <artifactId>decima</artifactId>
  <version>1.x-SNAPSHOT</version>
</dependency>
```

In order to be able to download snapshots from Sonatype add the following profile 
to your project `pom.xml`:

```xml
 <profiles>
    <profile>
        <id>allow-snapshots</id>
        <activation><activeByDefault>true</activeByDefault></activation>
        <repositories>
            <repository>
                <id>snapshots-repo</id>
                <url>https://oss.sonatype.org/content/repositories/snapshots</url>
                <releases><enabled>false</enabled></releases>
                <snapshots><enabled>true</enabled></snapshots>
            </repository>
        </repositories>
    </profile>
</profiles>
```

## Publishing a new version

Deploy a release to Maven Central with these commands:

```bash
$ git tag <version_number>
$ git push origin <version_number>
```

To update and publish the next SNAPSHOT version, just change and push the version:

```bash
$ mvn versions:set -DnewVersion=<version_number>-SNAPSHOT
$ git commit -am "Update to version <version_number>-SNAPSHOT"
$ git push origin master
```