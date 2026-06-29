package com.practiq.test;   // a small test-support package

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)   // JUnit/Micronaut read it at runtime
@Target(ElementType.TYPE)             // it goes on test classes
@MicronautTest(transactional = false, environments = "ctslice")
public @interface ComponentTest {
}