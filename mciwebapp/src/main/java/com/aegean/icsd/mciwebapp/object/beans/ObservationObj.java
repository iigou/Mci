package com.aegean.icsd.mciwebapp.object.beans;

import com.aegean.icsd.engine.core.annotations.DataProperty;
import com.aegean.icsd.engine.core.annotations.Entity;
import com.aegean.icsd.engine.core.annotations.Id;

@Entity(ObservationObj.NAME)
public class ObservationObj {
  public static final String NAME = "ObservationObj";

  @Id(autoGenerated = true)
  @DataProperty("hasId")
  private String id;

  @DataProperty("hasTotalImages")
  private int nbOfImages;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public int getNbOfImages() {
    return nbOfImages;
  }

  public void setNbOfImages(int nbOfImages) {
    this.nbOfImages = nbOfImages;
  }

}
