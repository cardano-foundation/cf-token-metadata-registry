package org.springframework.boot.actuate.health;

import java.util.Objects;

/**
 * Binary-compatibility stand-in for {@code org.springframework.boot.actuate.health.Status}
 * from Spring Boot 3. yaci-store 2.0.7 returns this type from its health indicators.
 */
public final class Status {

    public static final Status UNKNOWN = new Status("UNKNOWN");
    public static final Status UP = new Status("UP");
    public static final Status DOWN = new Status("DOWN");
    public static final Status OUT_OF_SERVICE = new Status("OUT_OF_SERVICE");

    private final String code;
    private final String description;

    public Status(String code) {
        this(code, "");
    }

    public Status(String code, String description) {
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.description = description == null ? "" : description;
    }

    public String getCode() {
        return this.code;
    }

    public String getDescription() {
        return this.description;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Status status)) {
            return false;
        }
        return this.code.equals(status.code);
    }

    @Override
    public int hashCode() {
        return this.code.hashCode();
    }

    @Override
    public String toString() {
        return this.code;
    }
}
