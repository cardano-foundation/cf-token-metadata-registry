package org.springframework.boot.actuate.health;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Binary-compatibility stand-in for {@code org.springframework.boot.actuate.health.Health}
 * from Spring Boot 3. yaci-store 2.0.7 health indicators return this type. Spring Boot 4
 * moved the actuator type to {@code org.springframework.boot.health.contributor.Health}.
 */
public final class Health {

    private final Status status;
    private final Map<String, Object> details;

    private Health(Status status, Map<String, Object> details) {
        this.status = status;
        this.details = Collections.unmodifiableMap(new LinkedHashMap<>(details));
    }

    public Status getStatus() {
        return this.status;
    }

    public Map<String, Object> getDetails() {
        return this.details;
    }

    public Health withoutDetails() {
        if (this.details.isEmpty()) {
            return this;
        }
        return status(getStatus()).build();
    }

    public static Builder unknown() {
        return status(Status.UNKNOWN);
    }

    public static Builder up() {
        return status(Status.UP);
    }

    public static Builder down(Throwable exception) {
        return down().withException(exception);
    }

    public static Builder down() {
        return status(Status.DOWN);
    }

    public static Builder outOfService() {
        return status(Status.OUT_OF_SERVICE);
    }

    public static Builder status(String code) {
        return status(new Status(code));
    }

    public static Builder status(Status status) {
        return new Builder(status);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Health health)) {
            return false;
        }
        return this.status.equals(health.status) && this.details.equals(health.details);
    }

    @Override
    public int hashCode() {
        return this.status.hashCode() * 31 + this.details.hashCode();
    }

    @Override
    public String toString() {
        return "Health{status=" + this.status + ", details=" + this.details + "}";
    }

    public static class Builder {

        private Status status;
        private final Map<String, Object> details = new LinkedHashMap<>();

        public Builder() {
            this.status = Status.UNKNOWN;
        }

        public Builder(Status status) {
            this.status = status;
        }

        public Builder(Status status, Map<String, ?> details) {
            this.status = status;
            this.details.putAll(details);
        }

        public Builder withException(Throwable exception) {
            return withDetail("error", exception.getClass().getName() + ": " + exception.getMessage());
        }

        public Builder withDetail(String key, Object value) {
            this.details.put(key, value);
            return this;
        }

        public Builder withDetails(Map<String, ?> details) {
            this.details.putAll(details);
            return this;
        }

        public Builder unknown() {
            this.status = Status.UNKNOWN;
            return this;
        }

        public Builder up() {
            this.status = Status.UP;
            return this;
        }

        public Builder down(Throwable exception) {
            return down().withException(exception);
        }

        public Builder down() {
            this.status = Status.DOWN;
            return this;
        }

        public Builder outOfService() {
            this.status = Status.OUT_OF_SERVICE;
            return this;
        }

        public Builder status(String code) {
            return status(new Status(code));
        }

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public Health build() {
            return new Health(this.status, this.details);
        }
    }
}
