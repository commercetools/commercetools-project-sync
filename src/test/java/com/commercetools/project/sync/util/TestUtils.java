package com.commercetools.project.sync.util;

import static io.vrap.rmf.base.client.utils.json.JsonUtils.fromInputStream;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ByProjectKeyCustomObjectsPost;
import com.commercetools.api.client.ByProjectKeyGraphqlPost;
import com.commercetools.api.client.ByProjectKeyGraphqlRequestBuilder;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.defaultconfig.ApiRootBuilder;
import com.commercetools.api.models.custom_object.CustomObject;
import com.commercetools.api.models.custom_object.CustomObjectDraft;
import com.commercetools.api.models.error.ErrorResponse;
import com.commercetools.api.models.error.ErrorResponseBuilder;
import com.commercetools.api.models.graph_ql.GraphQLRequest;
import com.commercetools.api.models.graph_ql.GraphQLResponse;
import com.commercetools.project.sync.model.response.LastSyncCustomObject;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.valfirst.slf4jtest.LoggingEvent;
import com.github.valfirst.slf4jtest.TestLogger;
import io.vrap.rmf.base.client.ApiHttpMethod;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.BadGatewayException;
import io.vrap.rmf.base.client.utils.json.JsonUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Condition;
import org.assertj.core.util.TriFunction;
import org.slf4j.event.Level;

// This utility class compiles but not used yet
// TODO: Use the utility functions and adjust them
public final class TestUtils {

  public static void assertTypeSyncerLoggingEvents(
      @Nonnull final TestLogger syncerTestLogger, final int numberOfResources) {
    final String typeStatsSummary =
        format(
            "Summary: %d types were processed in total (%d created, 0 updated "
                + "and 0 failed to sync).",
            numberOfResources, numberOfResources);

    assertSyncerLoggingEvents(syncerTestLogger, "TypeSync", typeStatsSummary);
  }

  public static void assertProductTypeSyncerLoggingEvents(
      @Nonnull final TestLogger syncerTestLogger, final int numberOfResources) {
    final String productTypesStatsSummary =
        format(
            "Summary: %d product types were processed in total (%d created, 0 updated, 0 failed to sync and 0 product"
                + " types with at least one NestedType or a Set of NestedType "
                + "attribute definition(s) referencing a missing product type).",
            numberOfResources, numberOfResources);

    assertSyncerLoggingEvents(syncerTestLogger, "ProductTypeSync", productTypesStatsSummary);
  }

  public static void assertCategorySyncerLoggingEvents(
      @Nonnull final TestLogger syncerTestLogger, final int numberOfResources) {
    final String categoryStatsSummary =
        format(
            "Summary: %d categories were processed in total (%d created, 0 updated, "
                + "0 failed to sync and 0 categories with a missing parent).",
            numberOfResources, numberOfResources);

    assertSyncerLoggingEvents(syncerTestLogger, "CategorySync", categoryStatsSummary);
  }

  public static void assertProductSyncerLoggingEvents(
      @Nonnull final TestLogger syncerTestLogger, final int numberOfResources) {
    final String productStatsSummary =
        format(
            "Summary: %d product(s) were processed in total (%d created, 0 updated, "
                + "0 failed to sync and 0 product(s) with missing reference(s)).",
            numberOfResources, numberOfResources);

    assertSyncerLoggingEvents(syncerTestLogger, "ProductSync", productStatsSummary);
  }

  public static void assertInventoryEntrySyncerLoggingEvents(
      @Nonnull final TestLogger syncerTestLogger, final int numberOfResources) {
    final String inventoryStatsSummary =
        format(
            "Summary: %d inventory entries were processed in total (%d created, 0 updated "
                + "and 0 failed to sync).",
            numberOfResources, numberOfResources);

    assertSyncerLoggingEvents(syncerTestLogger, "InventorySync", inventoryStatsSummary);
  }

  public static void assertCartDiscountSyncerLoggingEvents(
      @Nonnull final TestLogger syncerTestLogger, final int numberOfResources) {
    final String cartDiscountStatsSummary =
        format(
            "Summary: %d cart discounts were processed in total (%d created, 0 updated "
                + "and 0 failed to sync).",
            numberOfResources, numberOfResources);

    assertSyncerLoggingEvents(syncerTestLogger, "CartDiscount", cartDiscountStatsSummary);
  }

  public static void assertStateSyncerLoggingEvents(
      @Nonnull final TestLogger syncerTestLogger, final int numberOfResources) {
    final String stateSyncerStatsSummary =
        format(
            "Summary: %d state(s) were processed in total (%d created, 0 updated, 0 failed to sync and 0 state(s) with missing transition(s)).",
            numberOfResources,
            // 1 state is automatically built-in in all projects and processed, but it's not created
            Math.max(0, numberOfResources - 1));

    assertSyncerLoggingEvents(syncerTestLogger, "State", stateSyncerStatsSummary);
  }

  public static void assertTaxCategorySyncerLoggingEvents(
      @Nonnull final TestLogger syncerTestLogger, final int numberOfResources) {
    final String taxCategoryStatsSummary =
        format(
            "Summary: %d tax categories were processed in total (%d created, 0 updated and "
                + "0 failed to sync).",
            numberOfResources, numberOfResources);

    assertSyncerLoggingEvents(syncerTestLogger, "TaxCategorySync", taxCategoryStatsSummary);
  }

  public static void assertCustomerSyncerLoggingEvents(
      @Nonnull final TestLogger syncerTestLogger, final int numberOfResources) {
    final String customerStatsSummary =
        format(
            "Summary: %d customers were processed in total (%d created, 0 updated and "
                + "0 failed to sync).",
            numberOfResources, numberOfResources);

    assertSyncerLoggingEvents(syncerTestLogger, "CustomerSync", customerStatsSummary);
  }

  public static void assertShoppingListSyncerLoggingEvents(
      @Nonnull final TestLogger syncerTestLogger, final int numberOfResources) {
    final String shoppingListStatsSummary =
        format(
            "Summary: %d shopping lists were processed in total (%d created, 0 updated and "
                + "0 failed to sync).",
            numberOfResources, numberOfResources);

    assertSyncerLoggingEvents(syncerTestLogger, "ShoppingListSync", shoppingListStatsSummary);
  }

  public static void assertCustomObjectSyncerLoggingEvents(
      @Nonnull final TestLogger syncerTestLogger, final int numberOfResources) {
    final String customObjectsStatsSummary =
        format(
            "Summary: %d custom objects were processed in total (%d created, 0 updated and "
                + "0 failed to sync).",
            numberOfResources, numberOfResources);

    assertSyncerLoggingEvents(syncerTestLogger, "CustomObjectSync", customObjectsStatsSummary);
  }

  public static void assertSyncerLoggingEvents(
      @Nonnull final TestLogger testLogger,
      @Nonnull final String syncModuleName,
      @Nonnull final String statisticsSummary) {

    final Condition<LoggingEvent> startLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent.getMessage().contains(format("Starting %s", syncModuleName)),
            format("%s start log", syncModuleName));

    final Condition<LoggingEvent> statisticsLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent.getMessage().contains(statisticsSummary),
            format("%s statistics log", syncModuleName));

    assertThat(testLogger.getAllLoggingEvents())
        .haveExactly(1, startLog)
        .haveExactly(1, statisticsLog);
  }

  public static void verifyInteractionsWithClientAfterSync(
      @Nonnull final ProjectApiRoot client, final int numberOfGetConfigInvocations) {

    verify(client, times(1)).close();
    // Verify config is accessed for the success message after sync:
    // " example: Syncing products from CTP project with key 'x' to project with key 'y' is done","
    verify(client, times(numberOfGetConfigInvocations)).getProjectKey();
    verifyNoMoreInteractions(client);
  }

  public static <T> T readObjectFromResource(final String resourcePath, final Class<T> objectType) {
    final InputStream resourceAsStream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
    return fromInputStream(resourceAsStream, objectType);
  }

  public static <T> T readObject(final String jsonString, final Class<T> objectType)
      throws JsonProcessingException {
    final ObjectMapper objectMapper = JsonUtils.getConfiguredObjectMapper();
    return objectMapper.readValue(jsonString, objectType);
  }

  public static String readStringFromFile(final String resourcePath) {
    final InputStream resourceAsStream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
    try {
      return new String(resourceAsStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      return StringUtils.EMPTY;
    }
  }

  @SuppressWarnings("unchecked")
  public static void stubClientsCustomObjectService(
      @Nonnull final ProjectApiRoot client, @Nonnull final ZonedDateTime currentCtpTimestamp) {

    final CustomObject customObject = mockLastSyncCustomObject(currentCtpTimestamp);
    final ApiHttpResponse apiHttpResponse = mock(ApiHttpResponse.class);
    when(apiHttpResponse.getBody()).thenReturn(customObject);
    final ByProjectKeyCustomObjectsPost customObjectsPost =
        mock(ByProjectKeyCustomObjectsPost.class);
    when(customObjectsPost.execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(client.customObjects()).thenReturn(mock());
    when(client.customObjects().post(any(CustomObjectDraft.class))).thenReturn(customObjectsPost);
    when(client.customObjects().withContainerAndKey(anyString(), anyString())).thenReturn(mock());
    when(client.customObjects().withContainerAndKey(anyString(), anyString()).get())
        .thenReturn(mock());
    when(client.customObjects().withContainerAndKey(anyString(), anyString()).get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
  }

  public static ProjectApiRoot withTestClient(
      final String projectKey,
      final TriFunction<String, ApiHttpMethod, String, CompletableFuture<ApiHttpResponse<byte[]>>>
          fn) {
    return ApiRootBuilder.of(
            request -> {
              final String uri = request.getUri() != null ? request.getUri().toString() : "";
              final ApiHttpMethod method = request.getMethod();
              final String encodedRequestBody =
                  uri.contains("graphql")
                      ? new String(request.getBody(), StandardCharsets.UTF_8)
                      : "";
              final CompletableFuture<ApiHttpResponse<byte[]>> response =
                  fn.apply(uri, method, encodedRequestBody);
              if (response != null) {
                return response;
              }
              return null;
            })
        .withApiBaseUrl("testBaseUri")
        .build(projectKey);
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  public static CustomObject mockLastSyncCustomObject(@Nonnull ZonedDateTime currentCtpTimestamp) {
    final CustomObject customObject = mock(CustomObject.class);

    final LastSyncCustomObject<ProductSyncStatistics> lastSyncCustomObject =
        LastSyncCustomObject.of(ZonedDateTime.now(), new ProductSyncStatistics(), 100);

    when(customObject.getLastModifiedAt()).thenReturn(currentCtpTimestamp);
    when(customObject.getValue()).thenReturn(lastSyncCustomObject);
    return customObject;
  }

  @Nonnull
  public static Clock getMockedClock() {
    final Clock clock = mock(Clock.class);
    when(clock.millis()).thenReturn(0L);
    return clock;
  }

  public static void mockResourceIdsGraphQlRequest(
      ProjectApiRoot client, String resource, String id, String key) {
    final String jsonResponseString =
        "{\"data\":{\""
            + resource
            + "\":{\"results\":[{\"id\":\""
            + id
            + "\","
            + "\"key\":\""
            + key
            + "\"}]}}}";
    final GraphQLResponse result =
        JsonUtils.fromJsonString(jsonResponseString, GraphQLResponse.class);

    final ApiHttpResponse<GraphQLResponse> apiHttpResponse = mock(ApiHttpResponse.class);

    when(apiHttpResponse.getBody()).thenReturn(result);
    final ByProjectKeyGraphqlRequestBuilder byProjectKeyGraphqlRequestBuilder = mock();
    when(client.graphql()).thenReturn(byProjectKeyGraphqlRequestBuilder);
    final ByProjectKeyGraphqlPost byProjectKeyGraphqlPost = mock();
    when(byProjectKeyGraphqlRequestBuilder.post(any(GraphQLRequest.class)))
        .thenReturn(byProjectKeyGraphqlPost);
    when(byProjectKeyGraphqlPost.execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
  }

  public static BadGatewayException createBadGatewayException() {
    final String json = getErrorResponseJsonString(500);
    return new BadGatewayException(
        500, "", null, "", new ApiHttpResponse<>(500, null, json.getBytes(StandardCharsets.UTF_8)));
  }

  private static String getErrorResponseJsonString(Integer errorCode) {
    final ErrorResponse errorResponse =
        ErrorResponseBuilder.of()
            .statusCode(errorCode)
            .errors(Collections.emptyList())
            .message("test")
            .build();

    final ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    String json;
    try {
      json = ow.writeValueAsString(errorResponse);
    } catch (JsonProcessingException e) {
      // ignore the error
      json = null;
    }
    return json;
  }

  private TestUtils() {}
}
