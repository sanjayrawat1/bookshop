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

#### Compiling Spring Boot applications as native images
There are two ways to compile your Spring Boot applications into native executables. The first option uses GraalVM explicitly and produces an OS-specific
executable that runs directly on a machine. The second option relies on Cloud Native Buildpacks to containerize the native executable and run it on a container
runtime like Docker.

The first option requires the GraalVM runtime to be available on your machine. You can install it directly from the website (www.graalvm.org) or use a tool like
sdkman. I'll be using the latest GraalVM 22.1 distribution available at the time of writing, based on OpenJDK 19.

At the end of the installation procedure, sdkman will ask whether you want to make that distribution the default one. I recommend you say no, since we're going
to be explicit whenever we need to use GraalVM instead of the standard OpenJDK.

```shell
$ sdk install java 22.3.1.r19-grl
```

Navigate to Quote Service project (quote-service), configure the shell to use GraalVM, and install the native-image GraalVM component as follows:

```shell
# Configures the current shell to use the specified Java runtime
$ sdk use java 22.3.1.r19-grl
# Uses the gu utility provided by GraalVM to install the native-image component
$ gu install native-image
```

When you initialized the Quote Service project, the GraalVM Gradle/Maven official plugin was included automatically. That's the one providing the functionality
to compile applications using the GraalVM Native Image mode.

**NOTE** - The following Gradle tasks require that GraalVM is the current Java runtime. When using sdkman, you can do that by running sdk use java 22.2.r17-grl
in the Terminal window where you want to use GraalVM.

Take into account that the compilation step for GraalVM apps is more prolonged, taking several minutes depending on the computational resources available on
your machine. That is one of the drawbacks of working with native images.

From the same Terminal window where you switched to GraalVM as the current Java runtime, run the following command to compile the application to a native image:

```shell
$ ./gradlew nativeCompile
```

A standalone binary is the result of the command. Since it's a native executable, it will be different on macOS, Linux, and Windows. You can run it on your
machine natively, without the need for a JVM. In the case of Gradle, the native executable is generated in the build/native/nativeCompile folder. Go ahead and
run it.

```shell
$ build/native/nativeCompile/quote-service
```

The first thing to notice is the startup time, usually less than 100 ms with Spring Native. It’s an impressive improvement compared to the JVM option, which
takes a few seconds. The best part of this is that we didn't have to write any code to make that happen! Let's send a request to ensure that the application is
running correctly:

```shell
$ http :9101/quotes/random
```

You can also run the autotests as native executables to make them even more reliable, since they will use the actual runtime environment used in production.
However, the compilation step still takes longer than when running on the JVM:

```shell
$ ./gradlew nativeTest
```

Finally, you can run a Spring Boot application as a native image directly from Gradle/Maven:

```shell
$ ./gradlew nativeRun
```

##### CONTAINERIZING NATIVE IMAGES WITH BUILDPACKS
The second option for compiling Spring Boot applications to native executables relies on Cloud Native Buildpacks. We can use Buildpacks to build a container
image from the application native executable compiled by GraalVM. This approach benefits from not requiring GraalVM to be installed on your machine.

The produce a containerized native image, run the following command:

```shell
$ ./gradlew bootBuildImage
```

When it's done, try running the resulting container image:

```shell
$ docker run --rm -p 9101:9101 quote-service
```

The startup time should again be less than 100 ms. Go ahead and send a few requests to test whether the application is working correctly:

```shell
$ http :9101/quotes/random
```
