package org.springframework.boot.autoconfigure.domain;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * Binary-compatibility stand-in for Spring Boot 3 {@code @EntityScan}.
 * yaci-store 2.0.7 places this annotation on its store configuration classes. The annotation
 * is meta-annotated with Boot 4's {@link org.springframework.boot.persistence.autoconfigure.EntityScan}
 * so those packages are still scanned.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@org.springframework.boot.persistence.autoconfigure.EntityScan
public @interface EntityScan {

    @AliasFor(annotation = org.springframework.boot.persistence.autoconfigure.EntityScan.class, attribute = "value")
    String[] value() default {};

    @AliasFor(annotation = org.springframework.boot.persistence.autoconfigure.EntityScan.class, attribute = "basePackages")
    String[] basePackages() default {};

    @AliasFor(annotation = org.springframework.boot.persistence.autoconfigure.EntityScan.class, attribute = "basePackageClasses")
    Class<?>[] basePackageClasses() default {};
}
