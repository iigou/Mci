package com.aegean.icsd.mciwebapp.wordpuzzle.dao;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.aegean.icsd.mciwebapp.common.beans.MciException;
import com.aegean.icsd.ontology.IOntology;
import com.aegean.icsd.ontology.beans.OntologyException;
import com.aegean.icsd.ontology.queries.AskQuery;
import com.aegean.icsd.ontology.queries.SelectQuery;

import com.google.gson.JsonArray;

@Repository
public class WordPuzzleDao implements IWordPuzzleDao {

  @Autowired
  private IOntology ont;

  @Override
  public
  boolean solveGame(String id, String player, String word) throws MciException {
    AskQuery q = new AskQuery.Builder()
      .is("wp", "hasId", "id")
      .is("wp", "hasPlayer", "player")
      .is("wp", "hasWord","wordId")
      .is("wordId", "hasStringValue", "word")
      .addIriParam("hasId", ont.getPrefixedEntity("hasId"))
      .addIriParam("hasPlayer", ont.getPrefixedEntity("hasPlayer"))
      .addIriParam("hasWord", ont.getPrefixedEntity("hasWord"))
      .addIriParam("hasStringValue", ont.getPrefixedEntity("hasStringValue"))
      .addLiteralParam("id", id)
      .addLiteralParam("player", player)
      .addLiteralParam("word", word)
      .build();

    try {
      boolean result = ont.ask(q);
      return result;
    } catch (OntologyException e) {
      throw Exceptions.FailedToAskTheSolution(id, player, word, e);
    }

  }
}