package com.aegean.icsd.mciwebapp.observations.dao;

import java.util.List;

import com.aegean.icsd.engine.common.beans.Difficulty;
import com.aegean.icsd.mciwebapp.common.beans.MciException;

public interface IObservationDao {

  int getLastCompletedLevel(Difficulty difficulty, String playerName) throws MciException;

  List<String> getAssociatedSubjects(String id) throws MciException;

  String getImagePath(String id) throws MciException;
}
