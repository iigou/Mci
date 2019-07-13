package com.aegean.icsd.ontology.queries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class InsertQuery {
  private String command;
  private Map<String, String> prefixes = new HashMap<>();
  private List<InsertParam> params = new LinkedList<>();

  private InsertQuery () { }

  public String getCommand() {
    return command;
  }

  public Map<String, String> getPrefixes() {
    return prefixes;
  }

  public List<InsertParam> getParams() {
    return params;
  }


  public static class Builder {
    private Map<String, String> prefixes = new HashMap<>();
    private InsertParam subject;
    private Map<InsertParam, List<InsertParam>> relations = new LinkedHashMap<>();
    private List<InsertParam> params = new LinkedList<>();


    public Builder() { }

    public Builder addPrefix (String prefix, String Uri) {
      prefixes.put(prefix, Uri);
      return this;
    }

    public Builder insertEntry (InsertParam subject, String type) {
      this.subject = subject;
      this.subject.setIriParam(true);

      InsertParam rdfType = new InsertParam();
      rdfType.setIriParam(true);
      rdfType.setName("?rdfType");
      rdfType.setValue("rdf:type");
      params.add(rdfType);

      InsertParam typeToAssociate = new InsertParam();
      typeToAssociate.setIriParam(true);
      typeToAssociate.setName("?typeToAssociate");
      typeToAssociate.setValue(type);

      params.add(typeToAssociate);
      params.add(this.subject);

      List<InsertParam> typeList = new LinkedList<>();
      typeList.add(typeToAssociate);

      relations.put(rdfType, typeList);

      return this;
    }

    public Builder addRelation (InsertParam predicate, InsertParam object) {
      boolean found = false;
      for (InsertParam existingPred : this.relations.keySet()) {
        if (existingPred.getValue().equals(predicate.getValue())) {
          this.relations.get(existingPred).add(object);
          found = true;
          break;
        }
      }
      if (!found) {
        List<InsertParam> objects = new ArrayList<>();
        objects.add(object);
        this.relations.put(predicate, objects);
      }

      return this;
    }


    public InsertQuery build() {
      StringBuilder builder = new StringBuilder();
      InsertQuery query = new InsertQuery();
      builder.append("INSERT DATA {\n\t");
      builder.append("?").append(removeParamChars(subject.getName())).append(" ");

      Iterator<Map.Entry<InsertParam, List<InsertParam>>> relationIt = relations.entrySet().iterator();
      while (relationIt.hasNext()) {
        Map.Entry<InsertParam, List<InsertParam>> relation = relationIt.next();
        Iterator<InsertParam> objectsIt = relation.getValue().iterator();
        params.add(relation.getKey());
        params.addAll(relation.getValue());

        while (objectsIt.hasNext()) {
          InsertParam object = objectsIt.next();
          builder.append("?").append(removeParamChars(relation.getKey().getName())).append(" ")
            .append("?").append(removeParamChars(object.getName())).append(" ");
          if(objectsIt.hasNext()) {
            builder.append(";\n\t\t");
          }
        }
        if(relationIt.hasNext()) {
          builder.append(";\n\t\t");
        }
      }

      builder.append(".\n}\n");

      query.command = builder.toString();
      query.prefixes = prefixes;
      query.params = params;
      return query;
    }

    String removeParamChars(String entry) {
      return entry.replace("?", "").replace("$", "");
    }
  }
}