package com.aegean.icsd.mcidatabase.ontology;

import com.aegean.icsd.mcidatabase.MciDatabaseException;

public interface IMciOntology {
  String getEntityUri(String entityName) throws MciDatabaseException;
}
