# Updateting calfuzzer for modern Java

## Github Repository

The github repository containing the updated version of calfuzzer can be found at:
https://github.com/Pr3roxDLC/calfuzzer

## Migrating the programm
Migration of the calfuzzer Programm was done in several steps:
- Merging the changes done to the sourcecode by [Hünkar Can Tunç et al.](https://zenodo.org/records/7809600) for their project of extending calfuzzer. The changes done by the team were extracted out of the dockerfile included in their artifact.
- Unlike previously assumed, the extracted version was not already ported to a newer version of Java and was still using Java 1.6. The decission was made to start by migrating the programm to Java 1.8 to not have to deal with the module encapsulation introduced with [Jigsaw](https://openjdk.org/projects/jigsaw/) in Java 9 as the introduction of Jigsaw made several of the JVM Internal features used by calfuzzer difficult to use.
- The migration to Java 1.8 introduced several changes to the Java Standard Library and its APIs that required changes to be made to calfuzzer and the included benchmarks.
- For the programm to be able to work under Java 1.8, the [Soot](https://github.com/soot-oss/soot) Library, used for runtime bytecode instrumentation, had to be updated to a newer and Java 1.8 compatible version. Changes to the implementation of Soot between the versions required some changes to the bytecode instrumentation, notably to [`InstrumentorForActiveTesting.java`](https://github.com/Pr3roxDLC/calfuzzer/blob/master/src/javato/activetesting/instrumentor/InstrumentorForActiveTesting.java), where Soot required two system properties to be set, which are no longer automatically supported by the JVM.
- After the migration of the codebase to Java 1.8, the deadlock fuzzer was able to be compiled and run for the firs time. The fuzzer was able to find results on several of the test cases shown in [`run.xml`](https://github.com/Pr3roxDLC/calfuzzer/blob/master/run.xml). However the fuzzer seemed to randomly run into an endless loop in some of the tests from time to time. Through debugging and dumping the JVM the endlessly running tests were attributed to the JVM running out of memory. This was fixed by increasing the ammount of memory the JVM is allowed to use in the [`run.xml`](https://github.com/Pr3roxDLC/calfuzzer/blob/c90f2c70eb87780fb7ffc40bd70d3871915303c7/run.xml#L152).
- Two unit tests remained to be fixed. One was able to be fixed by upgrading the [`junit`](https://junit.org/junit4/) version from `4.7` to `4.12` and including the ['hamcrest'](https://hamcrest.org/) library. The other test required the recalculation of a JVM implementation specific float constant in ['ApproxDeterministicSchedulerTest.java'](https://github.com/Pr3roxDLC/calfuzzer/blame/master/test/javato/activetesting/deterministicscheduler/ApproxDeterministicSchedulerTest.java). With the two changes to the sourcecode, all unit-tests are now able to be successfully executed.
  
