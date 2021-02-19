````
  ____  ____ _____   ____                 _                                  _   
 / ___||  _ \_   _| |  _ \  _____   _____| | ___  _ __  _ __ ___   ___ _ __ | |_ 
 \___ \| |_) || |   | | | |/ _ \ \ / / _ \ |/ _ \| '_ \| '_ ` _ \ / _ \ '_ \| __|
  ___) |  __/ | |   | |_| |  __/\ V /  __/ | (_) | |_) | | | | | |  __/ | | | |_ 
 |____/|_|    |_|   |____/ \___| \_/ \___|_|\___/| .__/|_| |_| |_|\___|_| |_|\__|
                                                 |_|                                           
 testing ------------------------------------------------------------------------
````

[![build_status](https://travis-ci.com/spt-development/spt-development-test.svg?branch=main)](https://travis-ci.com/spt-development/spt-development-test)

This project provides re-usable components and helper classes to be used for integration and unit tests to perform the
following:

* HTTP requests (with optional authentication).
* Receive messages from JMS queues.
* Verify that log messages have been logged with Logback.
* GreenMail JUnit extension.

Usage
=====

See Javadoc.

Building locally
================

To build the project, run the following maven command:

    $ mvn clean install

Release
=======

To build a release and upload to Maven Central run the following maven command:

    $ export GPG_TTY=$(tty) # Required on Mac OS X
    $ mvn deploy -DskipTests -Prelease

NOTE. This is currently a manual step as not currently integrated into the build.