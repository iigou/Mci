package com.aegean.icsd.mciwebapp.object.dao;


import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.aegean.icsd.mciwebapp.object.beans.ProviderException;
import com.aegean.icsd.mciwebapp.object.beans.Word;
import com.aegean.icsd.ontology.IOntology;
import com.aegean.icsd.ontology.beans.OntologyException;
import com.aegean.icsd.ontology.queries.SelectQuery;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

@Repository
public class ObjectsDao implements IObjectsDao {

  @Autowired
  private IOntology ont;

   @Override
  public List<String> getNewWordIdsFor(String forEntity) throws ProviderException {
    SelectQuery q = new SelectQuery.Builder()
      .select("wordId")
      .whereHasType("s", ont.getPrefixedEntity(forEntity))
      .whereHasType("word", ont.getPrefixedEntity(Word.NAME))
      .where("word", "hasId", "wordId")
      .minus("s", "p", "word")
      .addIriParam("hasId", ont.getPrefixedEntity("hasId"))
      .build();

    try {
      JsonArray results = ont.select(q);
      List<String> ids = new ArrayList<>();
      for (JsonElement result : results) {
        ids.add(result.getAsJsonObject().get("wordId").getAsString());
      }
      return ids;
    } catch (OntologyException e) {
      throw Exceptions.FailedToRetrieveWords(Word.NAME, e);
    }
  }

  @Override
  public List<String> getAssociatedWordOfId(String id) throws ProviderException {
    SelectQuery q = new SelectQuery.Builder()
      .select("associatedId")
      .where("s", "p", "id")
      .where("s", "pAll", "w")
      .whereHasType("w",  ont.getPrefixedEntity(Word.NAME))
      .where("w", "hasId", "associatedId")
      .addIriParam("hasId", ont.getPrefixedEntity("hasId"))
      .addLiteralParam("id", id)
      .build();

    try {
      JsonArray results = ont.select(q);
      List<String> ids = new ArrayList<>();
      for (JsonElement result : results) {
        ids.add(result.getAsJsonObject().get("associatedId").getAsString());
      }
      return ids;
    } catch (OntologyException e) {
      throw Exceptions.FailedToRetrieveWords(id, e);
    }
  }
}
