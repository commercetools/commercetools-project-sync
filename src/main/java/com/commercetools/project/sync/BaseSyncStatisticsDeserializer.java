package com.commercetools.project.sync;

import com.commercetools.sync.commons.helpers.BaseSyncStatistics;
import com.commercetools.sync.customers.helpers.CustomerSyncStatistics;
import com.fasterxml.jackson.core.JsonParser;
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
      throws IOException {
    final ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
    final JsonNode syncStatisticsNode = mapper.readTree(jsonParser);
    try {
      final String syncStatisticsClassName =
          syncStatisticsNode.get("syncStatisticsClassName").asText();
      final Class<? extends BaseSyncStatistics> c =
          Class.forName(syncStatisticsClassName).asSubclass(BaseSyncStatistics.class);
      return mapper.treeToValue(syncStatisticsNode, c);
    } catch (ClassNotFoundException | ClassCastException e) {
      final Class<CustomerSyncStatistics> customerSyncStatisticsClass =
          CustomerSyncStatistics.class;
      return mapper.treeToValue(syncStatisticsNode, customerSyncStatisticsClass);
    }
  }
}
