package com.aegean.icsd.mciobjects.blocks.beans;

import com.aegean.icsd.engine.core.annotations.DataProperty;
import com.aegean.icsd.engine.core.annotations.Entity;
import com.aegean.icsd.engine.core.annotations.Key;
import com.aegean.icsd.engine.generator.beans.BaseGameObject;

@Entity(Block.NAME)
public class Block extends BaseGameObject {
  public static final String NAME = "Block";

  @Key
  @DataProperty("hasRowNumber")
  private Integer row;

  @Key
  @DataProperty("hasColumnNumber")
  private Integer column;

  @DataProperty("isMovingBlock")
  private Boolean moving;

  public void setRow(Integer row) {
    this.row = row;
  }

  public Integer getRow() {
    return this.row;
  }

  public void setColumn(Integer column) {
    this.column = column;
  }

  public Integer getColumn() {
    return this.column;
  }

  public void setMoving(Boolean moving) {
    this.moving = moving;
  }

  public Boolean isMoving() {
    return this.moving;
  }
}
