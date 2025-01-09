# Updating calfuzzer for modern Java

## GitHub Repository

The GitHub repository containing the updated version of calfuzzer can be found at:
https://github.com/Pr3roxDLC/calfuzzer

## Using the deadlock-fuzzer
The calfuzzer project requires `ant` and a Java 1.8 JRE to run. After cloning the repository, the deadlock-fuzzer can be executed on a preexisting set of benchmarks by executing the following command from within the root directory of the project: `ant -f run.xml deadlockfuzzer`. To use the fuzzer on a different programm a new `ant target` has to be added to the `run.xml`. 
Set up the `ant target` like this: 
```
<target name="new_test">
        <property name="javato.work.dir" value="${benchdir}"/>
        <property name="javato.app.main.class" value="fully.qualified.class.ToTest"/>
        <antcall target="deadlock-analysis"/>
</target>
```
To then use the fuzzer on your newly added target, execute the command: `ant -f run.xml new_test` from the root directory of the project.

## Our goal
The initial goal of this project was to migrate the [calfuzzer project](https://people.eecs.berkeley.edu/~ksen/papers/deadlock.pdf) to a modern version of Java in order to be able to further build upon the existing code and to be able to use its deadlock-fuzzing features on any given modern Java program. The migration to modern Java however posed a major challange, such as [Jigsaw Project](https://openjdk.org/projects/jigsaw/) (an attempt by the JCP to furhter limit the ability for developers to use internal features of the JVM) which was introduced with Java 1.9 or the major design changes done to the Soot library when compared to more recent versions. Because of this the decission was made to only migrate the project to Java 1.8 and to include the work done by [Hünkar Can Tunç et al.](https://zenodo.org/records/7809600) for their 2023 research paper "Sound Dynamic Deadlock Prediction in Linear Time".

## Migrating the program
Migration of the calfuzzer program was done in several steps:
- Merging the changes done to the source code by [Hünkar Can Tunç et al.](https://zenodo.org/records/7809600) for their project of extending calfuzzer. The changes done by the team were extracted out of the dockerfile included in their artifact.
- Unlike previously assumed, the extracted version was not already ported to a newer version of Java and was still using Java 1.6. The decision was made to start by migrating the program to Java 1.8 to not have to deal with the module encapsulation introduced with [Jigsaw](https://openjdk.org/projects/jigsaw/) in Java 9 as the introduction of Jigsaw made several of the JVM Internal features used by calfuzzer difficult to use.
- The migration to Java 1.8 introduced several changes to the Java Standard Library and its APIs that required changes to be made to calfuzzer and the included benchmarks.
- For the program to be able to work under Java 1.8, the [Soot](https://github.com/soot-oss/soot) Library, used for runtime bytecode instrumentation, had to be updated to a newer and Java 1.8 compatible version. Changes to the implementation of Soot between the versions required some changes to the bytecode instrumentation, notably to [`InstrumentorForActiveTesting.java`](https://github.com/Pr3roxDLC/calfuzzer/blob/master/src/javato/activetesting/instrumentor/InstrumentorForActiveTesting.java), where Soot required two system properties to be set, which are no longer automatically supported by the JVM. These System properties represent paths to jars that have to be included in the classpath at runtime, they are currently hardcoded and have to be changed in order for the programm to work.
- After the migration of the codebase to Java 1.8, the deadlock fuzzer could be compiled and run for the first time. The fuzzer was able to find results on several of the test cases shown in [`run.xml`](https://github.com/Pr3roxDLC/calfuzzer/blob/master/run.xml). However, the fuzzer seemed to randomly run into an endless loop in some of the tests from time to time. Through debugging and dumping the JVM the endlessly running tests were attributed to the JVM running out of memory. This was fixed by increasing the amount of memory the JVM is allowed to use in the [`run.xml`](https://github.com/Pr3roxDLC/calfuzzer/blob/c90f2c70eb87780fb7ffc40bd70d3871915303c7/run.xml#L152).
- Two unit tests remained to be fixed. One was fixed by upgrading the [Junit](https://junit.org/junit4/) version from `4.7` to `4.12` and including the [Hamcrest](https://hamcrest.org/) library. The other test required the recalculation of a JVM implementation specific float constant in [`ApproxDeterministicSchedulerTest.java`](https://github.com/Pr3roxDLC/calfuzzer/blame/master/test/javato/activetesting/deterministicscheduler/ApproxDeterministicSchedulerTest.java). With the two changes to the source code, all unit-tests are now able to be successfully executed.

## Issues with migrating to Java 1.9 and above
The introduction of the Jigsaw Project in Java 1.9 and above was part of a move away from JVM internal features and towards a more standardized Java environment. The two most notable issues for the calfuzzer project with these changes are:
1. The deprecation, removal and encapsulation of the `sun.misc.Unsafe` class. This class was used by developers to directly interact with the JVMs memory. In the deadlockfuzzer specifically `Unsafe` was used to detect locks/monitors. This specific feature has been marked as deprecated since Java 1.8 and was removed for Java 1.9. This could most likely be replaced with the use of a less low-level lock implementation from the Java standard library such as a `ReentrantLock`.
2. The removal of some of the reflections features. These changes however can mostly be bypassed by passing along the `--illegal-access=permit` paramter as a JVM argument. Aswell as opening the internal modules to the default unnamed module when starting the programm.
  
## Open topics 
- Migrate the deadlock-fuzzer to a more recent Java version, like Java 17 or Java 21
- Reverse-engineering and including the python based test-result-compilation infrastructure that is included in the dockerfile, to be able to get more results that the console provides
- Replace hardcoded classpath System properties with a relative path to the location
- Including own benchmarks and running the deadlock fuzzer on a separate program
