package com.commercetools.project.sync;

import com.commercetools.sync.commons.helpers.BaseSyncStatistics;
import com.commercetools.sync.customers.helpers.CustomerSyncStatistics;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

public class BaseSyncStatisticsDeserializer extends StdDeserializer<BaseSyncStatistics> {

  public BaseSyncStatisticsDeserializer() {
    this(null);
  }

  public BaseSyncStatisticsDeserializer(final Class<?> vc) {
    super(vc);
  }

  @Override
  public BaseSyncStatistics deserialize(JsonParser jsonParser, DeserializationContext ctxt)
      throws IOException, JsonProcessingException {
    final ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
    final JsonNode syncStatisticsNode = mapper.readTree(jsonParser);
    // since BaseSyncStatistics is abstract and there's no difference in the Subclasses except
    // implementation of
    // method getReportMessage(), additionally there's no field which could be used to identify the
    // type of the
    // subclass, the jsonNode is mapped to any concrete class
    // this is a workaround, to fix that BaseSyncStatistics in java-sync library needs to be
    // adjusted declaring
    // the subtypes with Json annotations: JsonTypeInfo and JsonSubTypes
    // more details: https://www.baeldung.com/jackson-inheritance#2-per-class-annotations
    return mapper.treeToValue(syncStatisticsNode, CustomerSyncStatistics.class);
  }
}
