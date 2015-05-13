# Commons eID Project


## Introduction

This project contains the source code tree of Commons eID.

The source code is hosted at: https://github.com/e-Contract/commons-eid

The Maven project site is hosted at e-contract.be: https://www.e-contract.be/sites/commons-eid/

Issues can be reported via github: https://github.com/e-Contract/commons-eid/issues

Also check out the eID Applet mailing list for announcements: https://groups.google.com/forum/#!forum/eid-applet


## Getting Started

A good entry point for using the Commons eID project is the Maven project site.

https://www.e-contract.be/sites/commons-eid/


## Requirements

The following is required for compiling the Commons eID software:

* Oracle Java 1.7.0_80 or 1.8.0_45
* Apache Maven 3.3.3
* Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy

Commons eID runs on Java 6+.


## Build

The project can be build via:

```shell
mvn clean install
```


## License

The source code of the Commons eID Project is licensed under GNU LGPL v3.0.
All source code files remain under control of the GNU LGPL v3.0 license 
unless otherwise decided in the future by _ALL_ Commons eID Project 
copyright holders.
The license conditions can be found in the file: LICENSE.txt


## Style Guide

Summary: "Eclipse Default Java Formatting"

Rationale:

Different code formatting wreaks havoc on diffs after commit:
Everything looks changed all the time.
I've added the java-formatter plugin to the main pom for this reason.

http://maven-java-formatter-plugin.googlecode.com/svn/site/0.3.1/plugin-info.html

Usage:

```shell
mvn -e com.googlecode.maven-java-formatter-plugin:maven-java-formatter-plugin:format
```

Using this leaves everyone free to have their own favorite formatting
in e.g. IDE's while still having a common formatting in the repository.
This is a great relief to someone like me to whom code formatted using
the default Eclipse/Sun styles looks like an RFC with modem noise.

If required, we could include a custom config XML at some point..

When using the Eclipse code formatter on default settings, according to
the docs, you should be OK without this plugin, as well. In practice
I've noticed there are small differences, mostly in empty line handling.
