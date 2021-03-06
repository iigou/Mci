package com.aegean.icsd.engine.rules.beans;

public class EntityRestriction {
  /**
   * The name of the property the restriction is on
   */
  private EntityProperty onProperty;

  /**
   * The type of restriction
   */
  private RestrictionType type;

  /**
   * The cardinality value
   */
  private int cardinality;

  /**
   * The acceptable range of values that must be given, if any
   */
  private ValueRangeRestriction dataRange;

  public EntityProperty getOnProperty() {
    return onProperty;
  }

  public void setOnProperty(EntityProperty onProperty) {
    this.onProperty = onProperty;
  }

  public RestrictionType getType() {
    return type;
  }

  public void setType(RestrictionType type) {
    this.type = type;
  }

  public int getCardinality() {
    return cardinality;
  }

  public void setCardinality(int cardinality) {
    this.cardinality = cardinality;
  }

  public ValueRangeRestriction getDataRange() {
    return dataRange;
  }

  public void setDataRange(ValueRangeRestriction dataRange) {
    this.dataRange = dataRange;
  }
}
