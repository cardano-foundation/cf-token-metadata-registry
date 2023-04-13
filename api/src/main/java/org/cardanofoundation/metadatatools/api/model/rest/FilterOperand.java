package org.cardanofoundation.metadatatools.api.model.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Gets or Sets FilterOperand */
public enum FilterOperand {
  EQ("eq"),

  NEQ("neq"),

  LT("lt"),

  LTE("lte"),

  GT("gt"),

  GTE("gte");

  private final String value;

  FilterOperand(final String value) {
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
  public static FilterOperand fromValue(final String value) {
    for (final FilterOperand b : FilterOperand.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
