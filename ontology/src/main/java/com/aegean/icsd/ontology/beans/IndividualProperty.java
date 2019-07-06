package com.aegean.icsd.ontology.beans;

public class IndividualProperty {
  /**
   * The name of the property
   */
  private String name;

  /**
   * The type of the property. Either ObjectProperty or DataTypeProperty
   */
  private String type;

  /**
   * The class name that is the range of the property values
   */
  private String range;

  /**
   * If this property marked as functional
   */
  private boolean mandatory;

  /**
   * If this property marked as symmetric
   */
  private boolean symmetric;

  /**
   * If this property marked as reflexive
   */
  private boolean reflexive;

  /**
   * If this property marked as irreflexive
   */
  private boolean irreflexive;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getRange() {
    return range;
  }

  public void setRange(String range) {
    this.range = range;
  }

  public boolean isMandatory() {
    return mandatory;
  }

  public void setMandatory(boolean mandatory) {
    this.mandatory = mandatory;
  }

  public boolean isSymmetric() {
    return symmetric;
  }

  public void setSymmetric(boolean symmetric) {
    this.symmetric = symmetric;
  }

  public boolean isReflexive() {
    return reflexive;
  }

  public void setReflexive(boolean reflexive) {
    this.reflexive = reflexive;
  }

  public boolean isIrreflexive() {
    return irreflexive;
  }

  public void setIrreflexive(boolean irreflexive) {
    this.irreflexive = irreflexive;
  }
}
