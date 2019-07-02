package com.aegean.icsd.ontology;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.jena.ontology.HasValueRestriction;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.Restriction;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.springframework.beans.factory.annotation.Autowired;

import com.aegean.icsd.connection.ConnectionException;
import com.aegean.icsd.connection.ITdbConnection;
import com.aegean.icsd.ontology.beans.Cardinality;
import com.aegean.icsd.ontology.beans.DataRangeRestrinction;
import com.aegean.icsd.ontology.beans.DatasetProperties;
import com.aegean.icsd.ontology.beans.Individual;
import com.aegean.icsd.ontology.beans.IndividualRestriction;
import com.aegean.icsd.ontology.beans.IndividualProperty;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class Ontology implements IOntology {

  @Autowired
  private DatasetProperties ontologyProps;

  @Autowired
  private ITdbConnection conProvider;

  private Dataset dataset;

  @Override
  public JsonArray executeSelect(ParameterizedSparqlString sparql, List<String> colNames) throws OntologyException {
    Connection con = null;
    JsonArray result = new JsonArray();
    try {
      con = conProvider.connect(ontologyProps.getDatasetLocation());
      java.sql.Statement sel = con.createStatement();
      ResultSet raw = sel.executeQuery(sparql.asQuery().toString());
      while (raw.next()) {
        JsonObject row = new JsonObject();
        for(String col : colNames) {
          row.addProperty(col, raw.getString(col));
        }
      }
      raw.close();
      return result;
    } catch (ConnectionException | SQLException e) {
      throw new OntologyException("ONT.EX1", "Cannot execute sparql query", e);
    } finally {
      try {
        if (con != null && !con.isClosed()) {
          con.close();
        }
      } catch (SQLException e) {
        //noinspection ThrowFromFinallyBlock
        throw new OntologyException("ONT.EX2", "Cannot close connection", e);
      }
    }
  }

  @Override
  public boolean executeUpdate(ParameterizedSparqlString sparql) throws OntologyException {
    Connection con = null;
    try{
      con = conProvider.connect(ontologyProps.getDatasetLocation());
      java.sql.Statement sel = con.createStatement();
      int res = sel.executeUpdate(sparql.asUpdate().toString());
      return res == 0;
    } catch (SQLException | ConnectionException e) {
      throw new OntologyException("ONT.EX3", "Cannot execute sparql query", e);
    } finally {
      try {
        if (con != null && !con.isClosed()) {
          con.close();
        }
      } catch (SQLException e) {
        //noinspection ThrowFromFinallyBlock
        throw new OntologyException("ONT.EX4", "Cannot close connection", e);
      }
    }
  }

  @Override
  public Individual generateIndividual(String className) throws OntologyException {
    Individual result = new Individual();
    result.setClassName(className);
    result.setId(UUID.randomUUID());
    OntClass entity = getOntClass(className);

    List<IndividualProperty> properties = generateDeclaredProperties(entity);
    List<IndividualRestriction> restrictions = generateRestrictions(entity);
    List<IndividualRestriction> equalityRestrictions = generateEqualityRestrictions(entity);

    result.setProperties(properties);
    result.setRestrictions(restrictions);
    result.setEqualityRestrictions(equalityRestrictions);
    return result;
  }

  List<IndividualProperty> generateDeclaredProperties(OntClass ontClass) {
    List<IndividualProperty> properties = new ArrayList<>();
    ExtendedIterator<OntProperty> propIt = ontClass.listDeclaredProperties();
    while (propIt.hasNext()) {
      IndividualProperty propertyDesc = generateProperty(propIt.next());
      properties.add(propertyDesc);
    }
    return properties;
  }

  List<IndividualRestriction> generateRestrictions(OntClass ontClass) throws OntologyException {
    List<IndividualRestriction> restrictions = new ArrayList<>();
    ExtendedIterator<OntClass> superClassesIt = ontClass.listSuperClasses();
    while (superClassesIt.hasNext()) {
      OntClass superClass = superClassesIt.next();
      if (superClass.isRestriction()) {
        Restriction resClass = superClass.asRestriction();
        IndividualRestriction restriction = generateRestriction(resClass);
        restrictions.add(restriction);
      }
    }
    return restrictions;
  }

  List<IndividualRestriction> generateEqualityRestrictions(OntClass entity) throws OntologyException {
    List<IndividualRestriction> equalityRestrictions = new ArrayList<>();
    OntClass equivalentClass = entity.getEquivalentClass();
    Resource intersectionOf = equivalentClass.getPropertyResourceValue(OWL2.intersectionOf);
    generateEqualityRestriction(intersectionOf, equalityRestrictions);
    return equalityRestrictions;
  }

  void generateEqualityRestriction(Resource intersectionOf, List<IndividualRestriction> equalityRestrictions)
          throws OntologyException {
    Resource first = intersectionOf.getPropertyResourceValue(RDF.first);
    if (first != null ) {
      if (first.canAs(OntClass.class)) {
        OntClass firstAsClass = first.as(OntClass.class);
        if (firstAsClass.isRestriction()) {
          Restriction restriction = firstAsClass.asRestriction();
          IndividualRestriction eqRestriction = generateRestriction(restriction);
          equalityRestrictions.add(eqRestriction);
        }
      }
      Resource rest = first.getPropertyResourceValue(RDF.rest);
      if (rest != null) {
        generateEqualityRestriction(rest, equalityRestrictions);
      }
    }
  }


  IndividualRestriction generateRestriction(Restriction restriction) throws OntologyException {
    IndividualRestriction result = new IndividualRestriction();
    OntProperty resProp = restriction.getOnProperty();
    result.setOnIndividualProperty(generateProperty(resProp));

    if (restriction.isAllValuesFromRestriction()) {
      result.setType("only");
    } else if (restriction.isHasValueRestriction()) {
      result.setType("value");
      HasValueRestriction valueRes = restriction.asHasValueRestriction();
      result.setExactValue(valueRes.getHasValue().asLiteral().getString());
    } else if (restriction.isSomeValuesFromRestriction()) {
      result.setType("some");
    } else {
      result.setType("cardinality");
      result.setCardinality(generateOwl2Cardinality(restriction));
    }
    return result;
  }

  IndividualProperty generateProperty(OntProperty property) {
    IndividualProperty descriptor = new IndividualProperty();
    descriptor.setName(property.getLocalName());
    descriptor.setType(property.isObjectProperty()? "ObjectProperty": "DataTypeProperty");
    descriptor.setRange(property.getRange().asClass().getLocalName());
    return descriptor;
  }

  Cardinality generateOwl2Cardinality(Restriction restriction) throws OntologyException {
    RDFNode qualifiedCardinality = restriction.getPropertyValue(OWL2.qualifiedCardinality);
    RDFNode maxQualifiedCardinality = restriction.getPropertyValue(OWL2.maxQualifiedCardinality);
    RDFNode minQualifiedCardinality = restriction.getPropertyValue(OWL2.minQualifiedCardinality);
    String type = null;
    String occurrences = null;

    Cardinality cardinality = new Cardinality();

    if (qualifiedCardinality != null) {
      type = "exactly";
      occurrences = qualifiedCardinality.asLiteral().getString();
    } else if (maxQualifiedCardinality != null) {
      type = "max";
      occurrences = maxQualifiedCardinality.asLiteral().getString();
    } else if (minQualifiedCardinality !=null) {
      type = "min";
      occurrences = minQualifiedCardinality.asLiteral().getString();
    } else {
      throw new OntologyException("CRDL.1", "Cannot calculate cardinality");
    }
    cardinality.setType(type);
    cardinality.setOccurrence(occurrences);
    cardinality.setDataRangeRestrictions(generateDataRangeRestrictions(restriction));

    return cardinality;
  }

  List<DataRangeRestrinction> generateDataRangeRestrictions(OntClass ont) {
    List<DataRangeRestrinction> dataRanges = new ArrayList<>();
    List<Statement> ranges = readDataRangeRestrictions(ont);
    for(Statement stmt : ranges) {
      DataRangeRestrinction dataRange = new DataRangeRestrinction();
      dataRange.setPredicate(stmt.getPredicate().getLocalName());
      Literal value = stmt.getLiteral();
      dataRange.setValue(value.getString());
      dataRange.setDatatype(value.getDatatypeURI());
      dataRanges.add(dataRange);
    }

    return dataRanges;
  }

  List<Statement> readDataRangeRestrictions (OntClass ont) {
    Resource dataRangeResource = ont.getPropertyResourceValue(OWL2.onDataRange);
    if(dataRangeResource != null) {
      Resource withRestrictionResource = dataRangeResource.getPropertyResourceValue(OWL2.withRestrictions);
      if (withRestrictionResource != null) {
        Resource restrictions = withRestrictionResource.getPropertyResourceValue(RDF.first);
        if (restrictions != null) {
          return  restrictions.listProperties().toList();
        }
      }
    }
    return new ArrayList<>();
  }

  OntClass getOntClass(String className) throws OntologyException {
    OntModel model = ModelFactory.createOntologyModel();
    try {
      FileInputStream inputStream = new FileInputStream(ontologyProps.getOntologyLocation());
      model.read(inputStream, null, this.ontologyProps.getOntologyType());
      return  model.getOntClass(ontologyProps.getNamespace() + className);
    } catch (FileNotFoundException e) {
      throw new OntologyException("ONT.GETCLASS.1", "Cannot load ontology model", e);
    }
  }

  @PostConstruct
  void setupDataset() throws OntologyException {
    String ontologyName = this.ontologyProps.getOntologyName();
    if (this.dataset == null) {
      this.dataset = TDBFactory.createDataset(this.ontologyProps.getDatasetLocation());
    }
    this.dataset.begin(ReadWrite.READ);
    boolean init = !this.dataset.containsNamedModel(ontologyName) || this.dataset.isEmpty();
    this.dataset.end();
    if (init) {
      this.dataset.begin(ReadWrite.WRITE);
      OntModel model = ModelFactory.createOntologyModel();
      try {
        FileInputStream is = new FileInputStream(this.ontologyProps.getOntologyLocation());
        model.read(is, null, this.ontologyProps.getOntologyType());
      } catch (FileNotFoundException e) {
        throw new OntologyException("ONT.LOAD.1", "Cannot load ontology model", e);
      }
      dataset.addNamedModel(ontologyName, model);
      dataset.commit();
      dataset.end();
    }
  }

}