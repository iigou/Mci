package com.aegean.icsd.ontology;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.jena.ontology.HasValueRestriction;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.ontology.Restriction;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryException;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ModelMaker;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.aegean.icsd.ontology.beans.CardinalitySchema;
import com.aegean.icsd.ontology.beans.ClassSchema;
import com.aegean.icsd.ontology.beans.DataRangeRestrinctionSchema;
import com.aegean.icsd.ontology.beans.DatasetProperties;
import com.aegean.icsd.ontology.beans.FusekiResponse;
import com.aegean.icsd.ontology.beans.OntologyException;
import com.aegean.icsd.ontology.beans.PropertySchema;
import com.aegean.icsd.ontology.beans.RestrictionSchema;
import com.aegean.icsd.ontology.queries.AskQuery;
import com.aegean.icsd.ontology.queries.InsertQuery;
import com.aegean.icsd.ontology.queries.SelectQuery;
import com.aegean.icsd.ontology.queries.beans.InsertParam;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import openllet.jena.PelletReasonerFactory;

@Service
public class FusekiOntology implements IOntology {

  private static Logger LOGGER = Logger.getLogger(FusekiOntology.class);

  @Autowired
  private DatasetProperties ontologyProps;

  private OntModel model;
  private HttpClient client;

  @Override
  public boolean ask(AskQuery ask) throws OntologyException {
    ParameterizedSparqlString sparql = getPrefixedSparql(new HashMap<>());
    sparql.setCommandText(ask.getCommand());

    for (Map.Entry<String, String> entry : ask.getIriParams().entrySet()) {
      sparql.setIri(entry.getKey(), entry.getValue());
    }

    for (Map.Entry<String, String> entry : ask.getStrLiteralParams().entrySet()) {
      sparql.setLiteral(entry.getKey(), entry.getValue());
    }

    for (Map.Entry<String, Integer> entry : ask.getIntLiteralParams().entrySet()) {
      sparql.setLiteral(entry.getKey(), entry.getValue());
    }

    String query;
    try {
      query = sparql.asQuery().toString();
    } catch (QueryException ex) {
      throw new OntologyException("ASK.1", "Error when constructing the query", ex);
    }

    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(ontologyProps.getDatasetLocation() + "/query"))
      .timeout(Duration.ofMinutes(5))
      .header("Content-Type", "application/x-www-form-urlencoded")
      .header("Accept", "application/sparql-results+json,*/*;q=0.9")
      .POST(HttpRequest.BodyPublishers.ofString("query=" + URLEncoder.encode(query, StandardCharsets.UTF_8)))
      .build();

    try {

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() >= 400) {
        throw new OntologyException("SEL.400", "Error when executing the query");
      }
      String body = response.body();
      FusekiResponse res = new Gson().fromJson(body, FusekiResponse.class);
      return res.getAskResponse();
    } catch (IOException | InterruptedException e) {
      throw new OntologyException("ASK.999", "Error when executing the query", e);
    }
  }

  @Override
  public JsonArray select(SelectQuery selectQuery) throws OntologyException {
    JsonArray array = new JsonArray();

    ParameterizedSparqlString sparql = getPrefixedSparql(selectQuery.getPrefixes());
    sparql.setCommandText(selectQuery.getCommand());

    for(Map.Entry<String, String> entry : selectQuery.getIriParams().entrySet()) {
      sparql.setIri(entry.getKey(), entry.getValue());
    }
    for(Map.Entry<String, String> entry : selectQuery.getLiteralParams().entrySet()) {
      sparql.setLiteral(entry.getKey(), entry.getValue());
    }
    for(Map.Entry<String, Long> entry : selectQuery.getLongLiteralParams().entrySet()) {
      sparql.setLiteral(entry.getKey(), entry.getValue());
    }
    for(Map.Entry<String, Boolean> entry : selectQuery.getBoolLiteralParams().entrySet()) {
      sparql.setLiteral(entry.getKey(), entry.getValue());
    }

    String query;
    try {
      query = sparql.asQuery().toString();
    } catch (QueryException ex ) {
      throw new OntologyException("SEL.1", "Error when constructing the query", ex);
    }

    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(ontologyProps.getDatasetLocation() + "/query"))
      .timeout(Duration.ofMinutes(5))
      .header("Content-Type", "application/x-www-form-urlencoded")
      .header("Accept", "application/sparql-results+json,*/*;q=0.9")
      .POST(HttpRequest.BodyPublishers.ofString("query=" + URLEncoder.encode(query, StandardCharsets.UTF_8)))
      .build();

    try {
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() >= 400) {
        throw new OntologyException("SEL.400", "Error when executing the query");
      }

     String body = response.body();
     FusekiResponse res = new Gson().fromJson(body, FusekiResponse.class);
     List<String> varNames = res.getHead().getVars();
     for (JsonElement elem : res.getResults().getBindings()) {
       JsonObject resultObj = new JsonObject();
       for (String varName : varNames) {
         JsonObject currentResult = elem.getAsJsonObject();
         if (currentResult.has(varName)) {
           String value = currentResult.get(varName).getAsJsonObject().get("value").getAsString();
           resultObj.addProperty(varName, value);
         }
       }
       if (resultObj.entrySet().size() > 0) {
         array.add(resultObj);
       }
     }
    } catch (IOException | InterruptedException e) {
      throw new OntologyException("SEL.1", "Error when executing the query", e);
    }
    return array;
  }

  @Override
  public boolean insert(InsertQuery insertQuery) throws OntologyException {
    ParameterizedSparqlString sparql = getPrefixedSparql(insertQuery.getPrefixes());
    sparql.setCommandText(insertQuery.getCommand());

    for (InsertParam param : insertQuery.getParams()) {
      if (param.isIriParam()) {
        sparql.setIri(param.getName(), param.getValue().toString());
      } else {
        if (String.class.equals(param.getValueClass())) {
          sparql.setLiteral(param.getName(), param.getValue().toString());
        } else if (Long.class.equals(param.getValueClass())) {
          sparql.setLiteral(param.getName(), Long.parseLong(param.getValue().toString()));
        } else if (Boolean.class.equals(param.getValueClass())) {
          sparql.setLiteral(param.getName(), (Boolean) param.getValue());
        }
      }
    }

    String query;
    try {
      query = sparql.asUpdate().toString();
    } catch (QueryException ex ) {
      throw new OntologyException("INS.1", "Error when constructing the query", ex);
    }

    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(ontologyProps.getDatasetLocation() + "/update"))
      .timeout(Duration.ofMinutes(5))
      .header("Content-Type", "application/x-www-form-urlencoded")
      .POST(HttpRequest.BodyPublishers.ofString("update=" + URLEncoder.encode(query, StandardCharsets.UTF_8)))
      .build();

    try {
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new OntologyException("INS.2", "Error when inserting the data");
      }
      return true;
    } catch (IOException | InterruptedException e) {
      throw new OntologyException("INS.99", "Error when executing the query", e);
    }
  }

  @Override
  public ClassSchema getClassSchema(String className) throws OntologyException {
    LOGGER.info(String.format("Reading ontology schema for class %s", className));
    ClassSchema result = new ClassSchema();
    result.setClassName(className);
    OntClass entity = getOntClass(className);

    List<PropertySchema> properties = getDeclaredPropertiesSchemas(entity);
    List<RestrictionSchema> restrictions = getRestrictionSchemas(entity);
    List<RestrictionSchema> equalityRestrictions = getEqualityRestrictionSchemas(entity);

    result.setProperties(properties);
    result.setRestrictions(restrictions);
    result.setEqualityRestrictions(equalityRestrictions);
    return result;
  }

  @Override
  public String getPrefixedEntity(String entityName) {
    return ontologyProps.getPrefix() + ":" + entityName;
  }

  @Override
  public Class<?> getJavaClassFromOwlType(String owlType) {
    Class<?> rangeClass = null;
    switch (owlType) {
      case "string" :
      case "anyURI" :
        rangeClass = String.class;
        break;
      case "positiveInteger":
        rangeClass = Long.class;
        break;
      case "boolean":
        rangeClass = Boolean.class;
        break;
      default:
        if (owlType.contains(";")) {
          rangeClass = String.class;
        }
        break;
    }
    return rangeClass;
  }

  @Override
  public String removePrefix(String prefixedEntity) {
    String[] chunks = prefixedEntity.split(":");
    String entity = "";
    if (chunks.length == 1) {
      entity = prefixedEntity;
    } else {
      entity = chunks[1].replace(">", "");
    }
    return entity;
  }

  List<PropertySchema> getDeclaredPropertiesSchemas(OntClass ontClass) {
    List<PropertySchema> properties = new ArrayList<>();

    ExtendedIterator<OntProperty> propIt = ontClass.listDeclaredProperties();
    while (propIt.hasNext()) {
      PropertySchema propertyDesc = getPropertySchema(propIt.next());
      if(propertyDesc.getName() != null) {
        properties.add(propertyDesc);
      }
    }
    return properties;
  }

  List<RestrictionSchema> getRestrictionSchemas(OntClass ontClass) throws OntologyException {
    List<RestrictionSchema> restrictions = new ArrayList<>();
    ExtendedIterator<OntClass> superClassesIt = ontClass.listSuperClasses();
    while (superClassesIt.hasNext()) {
      OntClass superClass = superClassesIt.next();
      if (superClass.isRestriction()) {
        Restriction resClass = superClass.asRestriction();
        RestrictionSchema restriction = getRestrictionSchema(resClass);
        restrictions.add(restriction);
      } else if (superClass.isClass()) {
        restrictions.addAll(getRestrictionSchemas(superClass));
      }
    }
    return restrictions;
  }

  List<RestrictionSchema> getEqualityRestrictionSchemas(OntClass entity) throws OntologyException {
    List<RestrictionSchema> equalityRestrictions = new ArrayList<>();
    ExtendedIterator<OntClass> eqIt = entity.listEquivalentClasses();
    while (eqIt.hasNext()) {
      OntClass eqClass = eqIt.next();
      if (eqClass != null && eqClass.isAnon()) {
        Resource intersectionOf = eqClass.getPropertyResourceValue(OWL2.intersectionOf);
        if (intersectionOf != null) {
          getEqualityRestrictionSchema(intersectionOf, equalityRestrictions);
        }
      }
    }
    return equalityRestrictions;
  }

  void getEqualityRestrictionSchema(Resource intersectionOf, List<RestrictionSchema> equalityRestrictions)
          throws OntologyException {
    Resource first = intersectionOf.getPropertyResourceValue(RDF.first);
    if (first != null ) {
      if (first.canAs(OntClass.class)) {
        OntClass firstAsClass = first.as(OntClass.class);
        if (firstAsClass.isRestriction()) {
          Restriction restriction = firstAsClass.asRestriction();
          RestrictionSchema eqRestriction = getRestrictionSchema(restriction);
          equalityRestrictions.add(eqRestriction);
        }
        Resource rest = intersectionOf.getPropertyResourceValue(RDF.rest);
        if (rest != null) {
          getEqualityRestrictionSchema(rest, equalityRestrictions);
        }
      }
    }
  }

  RestrictionSchema getRestrictionSchema(Restriction restriction) throws OntologyException {
    RestrictionSchema result = new RestrictionSchema();
    OntProperty resProp = restriction.getOnProperty();
    result.setOnPropertySchema(getPropertySchema(resProp));

    if (resProp.isObjectProperty()) {
      Resource onClass = restriction.getPropertyResourceValue(OWL2.onClass);
      if (onClass == null) {
        onClass = restriction.getPropertyResourceValue(OWL2.someValuesFrom);
      }
      result.getOnPropertySchema().setRange(onClass.getLocalName());
    }

    if (restriction.isAllValuesFromRestriction()) {
      result.setType(RestrictionSchema.ONLY_TYPE);
    } else if (restriction.isHasValueRestriction()) {
      result.setType(RestrictionSchema.VALUE_TYPE);
      HasValueRestriction valueRes = restriction.asHasValueRestriction();
      result.setExactValue(valueRes.getHasValue().asLiteral().getString());
    } else if (restriction.isSomeValuesFromRestriction()) {
      result.setType(RestrictionSchema.SOME_TYPE);
    } else {
      result.setType(getOwl2RestrictionType(restriction));
      result.setCardinalitySchema(getOwl2CardinalitySchema(restriction));
    }
    return result;
  }

  PropertySchema getPropertySchema(OntProperty property) {
    PropertySchema descriptor = new PropertySchema();
    if (property.isOntLanguageTerm()) {
      return descriptor;
    }
    descriptor.setName(property.getLocalName());
    descriptor.setObjectProperty(property.isObjectProperty());
    OntResource rangeResource = property.getRange();
    OntClass rangeClass = rangeResource.asClass();
    if (rangeClass.isEnumeratedClass()) {
      ListIterator<RDFNode> possibleValues = rangeClass.asEnumeratedClass().getOneOf().asJavaList().listIterator();
      String possibleValue = "";
      while (possibleValues.hasNext()) {
        possibleValue += possibleValues.next().asLiteral().getString();
        if (possibleValues.hasNext()) {
          possibleValue += ";";
        }
      }
      descriptor.setRange(possibleValue);
    } else {
      descriptor.setRange(rangeClass.getLocalName());
    }
    descriptor.setMandatory(property.isFunctionalProperty());
    descriptor.setSymmetric(property.isSymmetricProperty());
    descriptor.setReflexive(property.hasRDFType(OWL2.ReflexiveProperty));
    descriptor.setIrreflexive(property.hasRDFType(OWL2.IrreflexiveProperty));
    return descriptor;
  }

  CardinalitySchema getOwl2CardinalitySchema(Restriction restriction) throws OntologyException {
    RDFNode qualifiedCardinality = restriction.getPropertyValue(OWL2.qualifiedCardinality);
    RDFNode maxQualifiedCardinality = restriction.getPropertyValue(OWL2.maxQualifiedCardinality);
    RDFNode minQualifiedCardinality = restriction.getPropertyValue(OWL2.minQualifiedCardinality);
    String occurrences;

    CardinalitySchema cardinalitySchema = new CardinalitySchema();

    if (qualifiedCardinality != null) {
      occurrences = qualifiedCardinality.asLiteral().getString();
    } else if (maxQualifiedCardinality != null) {
      occurrences = maxQualifiedCardinality.asLiteral().getString();
    } else if (minQualifiedCardinality !=null) {
      occurrences = minQualifiedCardinality.asLiteral().getString();
    } else {
      throw new OntologyException("CRDL.1", "Cannot calculate cardinalitySchema");
    }

    cardinalitySchema.setOccurrence(occurrences);
    cardinalitySchema.setDataRangeRestrictions(generateDataRangeRestrictions(restriction));

    return cardinalitySchema;
  }

  String getOwl2RestrictionType(Restriction restriction) throws OntologyException {
    RDFNode qualifiedCardinality = restriction.getPropertyValue(OWL2.qualifiedCardinality);
    RDFNode maxQualifiedCardinality = restriction.getPropertyValue(OWL2.maxQualifiedCardinality);
    RDFNode minQualifiedCardinality = restriction.getPropertyValue(OWL2.minQualifiedCardinality);
    String type;
    if (qualifiedCardinality != null) {
      type = RestrictionSchema.EXACTLY_TYPE;
    } else if (maxQualifiedCardinality != null) {
      type = RestrictionSchema.MAX_TYPE;
    } else if (minQualifiedCardinality !=null) {
      type = RestrictionSchema.MIN_TYPE;
    } else {
      throw new OntologyException("RESTYP.1", "Cannot calculate restriction type");
    }

    return type;
  }

  List<DataRangeRestrinctionSchema> generateDataRangeRestrictions(OntClass ont) {
    List<DataRangeRestrinctionSchema> dataRanges = new ArrayList<>();
    Resource dataRangeResource = ont.getPropertyResourceValue(OWL2.onDataRange);
    if(dataRangeResource != null) {
      Resource withRestrictionResource = dataRangeResource.getPropertyResourceValue(OWL2.withRestrictions);
      if (withRestrictionResource != null) {
        getDataRanges(withRestrictionResource, dataRanges);
      }
    }

    return dataRanges;
  }

  void getDataRanges(Resource resource, List<DataRangeRestrinctionSchema> dataRanges) {
    Resource first = resource.getPropertyResourceValue(RDF.first);
    if (first != null ) {
      StmtIterator it = first.listProperties();
      while (it.hasNext()) {
        Statement stmt = it.nextStatement();
        DataRangeRestrinctionSchema eqRestriction = getDataRangeSchema(stmt);
        dataRanges.add(eqRestriction);
      }
      Resource rest = resource.getPropertyResourceValue(RDF.rest);
      if (rest != null) {
        getDataRanges(rest, dataRanges);
      }
    }
  }

  DataRangeRestrinctionSchema getDataRangeSchema(Statement stmt) {
    DataRangeRestrinctionSchema dataRange = new DataRangeRestrinctionSchema();
    dataRange.setPredicate(stmt.getPredicate().getLocalName());
    Literal value = stmt.getLiteral();
    dataRange.setValue(value.getString());
    dataRange.setDatatype(value.getDatatypeURI());
    return dataRange;
  }

  OntClass getOntClass(String className) {
    OntClass result = this.model.getOntClass(ontologyProps.getNamespace() + className);
    return result;
  }

  ParameterizedSparqlString getPrefixedSparql(Map<String, String> prefixes) {
    Map<String, String> defaultPrefixes = new HashMap<>();
    defaultPrefixes.put(ontologyProps.getPrefix(), ontologyProps.getNamespace());
    defaultPrefixes.put("owl", "http://www.w3.org/2002/07/owl#");
    defaultPrefixes.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
    defaultPrefixes.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
    defaultPrefixes.put("xsd", "http://www.w3.org/2001/XMLSchema#");

    defaultPrefixes.putAll(prefixes);

    ParameterizedSparqlString sparql = new ParameterizedSparqlString();
    sparql.setNsPrefixes(defaultPrefixes);
    return sparql;
  }

  @PostConstruct
  void setupModel () {
    LOGGER.info("START: Setting up the model");
    String ontologyName = ontologyProps.getOntologyName();

    ModelMaker maker= ModelFactory.createMemModelMaker();
    OntModelSpec spec = new OntModelSpec(PelletReasonerFactory.THE_SPEC);
    spec.setBaseModelMaker(maker);
    spec.setImportModelMaker(maker);
    Model base = maker.createModel( ontologyName );
    model = ModelFactory.createOntologyModel(spec, base);
    LOGGER.info("START: Reading the model from :" + ontologyProps.getOntologyLocation());
    model.read(ontologyProps.getOntologyLocation(), ontologyProps.getOntologyType());
    LOGGER.info("END: Reading the model from :" + ontologyProps.getOntologyLocation());
    client = HttpClient.newBuilder()
      .version(HttpClient.Version.HTTP_2)
      .followRedirects(HttpClient.Redirect.NEVER)
      .build();
  }

}