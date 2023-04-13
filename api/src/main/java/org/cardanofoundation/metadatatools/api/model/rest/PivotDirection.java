package org.cardanofoundation.metadatatools.api.model.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PivotDirection {
  BEFORE("before"),

  AFTER("after");

  private final String value;

  PivotDirection(final String value) {
    this.value = value;
  }

  @JsonValue
  public final String getValue() {
    return value;
  }

  @Override
  public final String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static PivotDirection fromValue(final String value) {
    for (final PivotDirection b : PivotDirection.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
