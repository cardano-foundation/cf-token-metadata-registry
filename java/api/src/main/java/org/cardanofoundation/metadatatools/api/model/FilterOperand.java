package org.cardanofoundation.metadatatools.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Gets or Sets FilterOperand
 */
public enum FilterOperand {
  
  EQ("eq"),
  
  NEQ("neq"),
  
  LT("lt"),
  
  LTE("lte"),
  
  GT("gt"),
  
  GTE("gte");

  private String value;

  FilterOperand(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static FilterOperand fromValue(String value) {
    for (FilterOperand b : FilterOperand.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

