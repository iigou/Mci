package com.aegean.icsd.mciwebapp.logicalorder.beans;

import com.aegean.icsd.engine.core.annotations.DataProperty;
import com.aegean.icsd.engine.core.annotations.Entity;
import com.aegean.icsd.engine.common.beans.BaseGame;

@Entity(LogicalOrder.NAME)
public class LogicalOrder extends BaseGame {
  public static final String NAME = "LogicalOrder";

  @DataProperty("hasStep")
  private Integer step;

  @DataProperty("hasSquareColumns")
  private Integer columns;

  @DataProperty("hasSquareRows")
  private Integer rows;

  @DataProperty("hasTotalMovingBlocks")
  private Integer totalMovingBlocks;

  @DataProperty("hasMovement")
  private String movement;

  @DataProperty("choices")
  private Integer choices;

  public Integer getStep() {
    return step;
  }

  public void setStep(Integer step) {
    this.step = step;
  }

  public Integer getColumns() {
    return columns;
  }

  public void setColumns(Integer columns) {
    this.columns = columns;
  }

  public Integer getRows() {
    return rows;
  }

  public void setRows(Integer rows) {
    this.rows = rows;
  }

  public Integer getTotalMovingBlocks() {
    return totalMovingBlocks;
  }

  public void setTotalMovingBlocks(Integer totalMovingBlocks) {
    this.totalMovingBlocks = totalMovingBlocks;
  }

  public void setMovement(String movement) {
    this.movement = movement;
  }

  public String getMovement() {
    return this.movement;
  }

  public void setChoices(Integer choices) {
    this.choices = choices;
  }

  public Integer getChoices() {
    return this.choices;
  }
}
