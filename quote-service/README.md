# quote-service

#### Native images with Spring Native and GraalVM
One of the reasons why Java applications became widely popular was the common platform (the Java Runtime Environment, or JRE), allowing developers to write them
once, run them everywhere, no matter the operating system. That comes from the way applications are compiled. Rather than compiling the application code
directly into machine code (the code understood by operating systems), the Java compiler produces bytecode that a dedicated component (the Java Virtual Machine,
or JVM) runs. During execution, the JRE interprets the bytecode into machine code dynamically, allowing the same application executable to run on any machine
and OS where a JVM is available. This is called a just-in-time (JIT) compilation.

Applications running on the JVM are subject to startup and footprint costs. The startup phase used to be quite long for traditional applications, for which it
could even take several minutes. Standard cloud native applications have a much faster startup phase: a few seconds rather than a few minutes. This is good
enough for most scenarios, but it can become a serious issue for serverless workloads that are required to start almost instantaneously.

GraalVM is a newer distribution from Oracle based on OpenJDK and is designed to accelerate the execution of applications written in Java and other JVM
languages (www.graalvm.org).

By replacing a standard OpenJDK distribution with GraalVM as the runtime environment for your Java applications, you can increase their performance and
efficiency, thanks to a new optimized technology for performing JIT compilation (the GraalVM compiler).

GraalVM offers two primary operational modes. The JVM Runtime mode lets you run your Java applications like any other OpenJDK distribution while improving
performance and efficiency thanks to the GraalVM compiler. What makes GraalVM so innovative and popular in the serverless context is the Native Image mode.
Rather than compiling your Java code into bytecode and relying on a JVM to interpret it and convert it to machine code, GraalVM offers a new technology (the
Native Image builder) that compiles Java applications directly into machine code, obtaining a native executable or native image that contains the whole machine
code necessary for its execution.

Java applications compiled as native images have faster startup times, optimized memory consumption, and instant peak performance compared to the JVM options.
GraalVM builds them by changing the way applications are compiled. Instead of a JIT-compiler optimizing and producing machine code at runtime, the Native Image
mode is based on Ahead-Of-Time (AOT) compilation.

When using native images, much of the work that used to be performed at runtime by the JVM is now done at build time. As a result, building an application into
a native executable takes longer and requires more computational resources than the JVM option.

#### Introducing GraalVM support for Spring Boot with Spring Native
Spring Native is a new project introduced to support compiling Spring Boot applications with GraalVM. The main goal of Spring Native is to make it possible to
compile any Spring application into a native executable using GraalVM without any code changes. To achieve that goal, the project provides an AOT infrastructure
(invoked from a dedicated Gradle/Maven plugin) that contributes all the required configurations for GraalVM to AOT-compile Spring classes.

We can run the quote-service as standard Spring Boot application.

```shell
$ ./gradlew test
$ ./gradlew bootRun
```

Verify the application is running correctly by calling endpoints:

```shell
$ http :9101/quotes
$ http :9101/quotes/random
$ http :9101/quotes/random/FANTASY
```
