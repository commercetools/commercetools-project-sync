package com.commercetools.project.sync.model.request;

import static io.sphere.sdk.utils.SphereInternalUtils.asSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.project.sync.model.response.CombinedResult;
import com.commercetools.project.sync.model.response.ReferenceIdKey;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sphere.sdk.client.HttpRequestIntent;
import io.sphere.sdk.http.HttpMethod;
import io.sphere.sdk.http.HttpResponse;
import io.sphere.sdk.http.StringHttpRequestBody;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CombinedResourceKeysRequestTest {

  @Test
  void newCombinedResourceKeysRequest_WithAllEmptySets_ShouldCreateAnInstance() {
    // preparation
    final Set<String> productIds = new HashSet<>();
    final Set<String> categoryIds = new HashSet<>();
    final Set<String> productTypeIds = new HashSet<>();

    // test
    final CombinedResourceKeysRequest combinedResourceKeysRequest =
        new CombinedResourceKeysRequest(productIds, categoryIds, productTypeIds);

    // assertions
    assertThat(combinedResourceKeysRequest).isExactlyInstanceOf(CombinedResourceKeysRequest.class);
  }

  @Test
  void newCombinedResourceKeysRequest_WithSomeNullSets_ShouldThrowNPE() {
    // preparation
    final Set<String> productIds = null;
    final Set<String> categoryIds = new HashSet<>();
    final Set<String> productTypeIds = new HashSet<>();

    // test & assertion
    assertThatThrownBy(
            () -> new CombinedResourceKeysRequest(productIds, categoryIds, productTypeIds))
        .isExactlyInstanceOf(NullPointerException.class);
  }

  @Test
  void httpRequestIntent_WithAllEmptyIdSets_ShouldThrowIllegalArgumentException() {
    // preparation
    final Set<String> productIds = new HashSet<>();
    final Set<String> categoryIds = new HashSet<>();
    final Set<String> productTypeIds = new HashSet<>();

    final CombinedResourceKeysRequest combinedResourceKeysRequest =
        new CombinedResourceKeysRequest(productIds, categoryIds, productTypeIds);

    // test & assertion
    assertThatThrownBy(combinedResourceKeysRequest::httpRequestIntent)
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessage("No ids passed to build the graphql request body.");
  }

  @Test
  void httpRequestIntent_withSomeIds_ShouldBuildPostGraphQLRequest() {
    // preparation
    final Set<String> productIds = new HashSet<>();
    final Set<String> categoryIds = asSet("foo", "bar");
    final Set<String> productTypeIds = asSet("foo", "bar");

    final CombinedResourceKeysRequest combinedResourceKeysRequest =
        new CombinedResourceKeysRequest(productIds, categoryIds, productTypeIds);

    // test
    final HttpRequestIntent httpRequestIntent = combinedResourceKeysRequest.httpRequestIntent();

    // assertion
    assertThat(httpRequestIntent.getPath()).isEqualTo("/graphql");
    assertThat(httpRequestIntent.getHttpMethod()).isEqualTo(HttpMethod.POST);
  }

  @Test
  void httpRequestIntent_WithOnlyEmptyProductIds_ShouldBuildRequestCorrectly() {
    // preparation
    final Set<String> productIds = new HashSet<>();
    final Set<String> categoryIds = asSet("foo", "bar");
    final Set<String> productTypeIds = asSet("foo", "bar");

    final CombinedResourceKeysRequest combinedResourceKeysRequest =
        new CombinedResourceKeysRequest(productIds, categoryIds, productTypeIds);

    // test
    final HttpRequestIntent httpRequestIntent = combinedResourceKeysRequest.httpRequestIntent();

    // assertion
    assertThat(httpRequestIntent.getBody()).isExactlyInstanceOf(StringHttpRequestBody.class);
    final StringHttpRequestBody requestBody = (StringHttpRequestBody) httpRequestIntent.getBody();
    assertThat(requestBody).isNotNull();
    assertThat(requestBody.getString())
        .isEqualTo(
            "{\"query\": \"{ "
                + "categories(limit: 500, where: \\\"id in (\\\\\\\"foo\\\\\\\", \\\\\\\"bar\\\\\\\")\\\") { results { id key } }, "
                + "productTypes(limit: 500, where: \\\"id in (\\\\\\\"foo\\\\\\\", \\\\\\\"bar\\\\\\\")\\\") { results { id key } }"
                + " }\"}");
  }

  @Test
  void httpRequestIntent_WithOnlyCategoryIds_ShouldBuildRequestCorrectly() {
    // preparation
    final Set<String> productIds = new HashSet<>();
    final Set<String> categoryIds = asSet("foo", "bar");
    final Set<String> productTypeIds = new HashSet<>();

    final CombinedResourceKeysRequest combinedResourceKeysRequest =
        new CombinedResourceKeysRequest(productIds, categoryIds, productTypeIds);

    // test
    final HttpRequestIntent httpRequestIntent = combinedResourceKeysRequest.httpRequestIntent();

    // assertion
    assertThat(httpRequestIntent.getBody()).isExactlyInstanceOf(StringHttpRequestBody.class);
    final StringHttpRequestBody requestBody = (StringHttpRequestBody) httpRequestIntent.getBody();
    assertThat(requestBody).isNotNull();
    assertThat(requestBody.getString())
        .isEqualTo(
            "{\"query\": \"{ "
                + "categories(limit: 500, where: \\\"id in (\\\\\\\"foo\\\\\\\", \\\\\\\"bar\\\\\\\")\\\") { results { id key } }"
                + " }\"}");
  }

  @Test
  void httpRequestIntent_WithOnlyProductIds_ShouldBuildRequestCorrectly() {
    // preparation
    final Set<String> productIds = asSet("foo", "bar");
    final Set<String> categoryIds = new HashSet<>();
    final Set<String> productTypeIds = new HashSet<>();

    final CombinedResourceKeysRequest combinedResourceKeysRequest =
        new CombinedResourceKeysRequest(productIds, categoryIds, productTypeIds);

    // test
    final HttpRequestIntent httpRequestIntent = combinedResourceKeysRequest.httpRequestIntent();

    // assertion
    assertThat(httpRequestIntent.getBody()).isExactlyInstanceOf(StringHttpRequestBody.class);
    final StringHttpRequestBody requestBody = (StringHttpRequestBody) httpRequestIntent.getBody();
    assertThat(requestBody).isNotNull();
    assertThat(requestBody.getString())
        .isEqualTo(
            "{\"query\": \"{ "
                + "products(limit: 500, where: \\\"id in (\\\\\\\"foo\\\\\\\", \\\\\\\"bar\\\\\\\")\\\") { results { id key } }"
                + " }\"}");
  }

  @Test
  void httpRequestIntent_WithNoEmptyIds_ShouldBuildRequestCorrectly() {
    // preparation
    final Set<String> productIds = asSet("foo", "bar");
    final Set<String> categoryIds = asSet("foo", "bar");
    final Set<String> productTypeIds = asSet("foo", "bar");

    final CombinedResourceKeysRequest combinedResourceKeysRequest =
        new CombinedResourceKeysRequest(productIds, categoryIds, productTypeIds);

    // test
    final HttpRequestIntent httpRequestIntent = combinedResourceKeysRequest.httpRequestIntent();

    // assertion
    assertThat(httpRequestIntent.getBody()).isExactlyInstanceOf(StringHttpRequestBody.class);
    final StringHttpRequestBody requestBody = (StringHttpRequestBody) httpRequestIntent.getBody();
    assertThat(requestBody).isNotNull();
    assertThat(requestBody.getString())
        .isEqualTo(
            "{\"query\": \"{ "
                + "products(limit: 500, where: \\\"id in (\\\\\\\"foo\\\\\\\", \\\\\\\"bar\\\\\\\")\\\") { results { id key } }, "
                + "categories(limit: 500, where: \\\"id in (\\\\\\\"foo\\\\\\\", \\\\\\\"bar\\\\\\\")\\\") { results { id key } }, "
                + "productTypes(limit: 500, where: \\\"id in (\\\\\\\"foo\\\\\\\", \\\\\\\"bar\\\\\\\")\\\") { results { id key } }"
                + " }\"}");
  }

  @Test
  void deserialize_WithCompleteCombinedResult_ShouldDeserializeCorrectly() throws IOException {
    // preparation
    final ObjectMapper objectMapper = new ObjectMapper();

    final JsonNode jsonNode =
        objectMapper.readTree(getClass().getClassLoader().getResource("full-combined-result.json"));

    final HttpResponse response = HttpResponse.of(200, jsonNode.toString());

    final CombinedResourceKeysRequest combinedResourceKeysRequest =
        mock(CombinedResourceKeysRequest.class);

    when(combinedResourceKeysRequest.deserialize(any())).thenCallRealMethod();

    // test
    final CombinedResult result = combinedResourceKeysRequest.deserialize(response);

    // assertion
    final String uuid = "c327eb8d-4924-4d47-9b31-d0caed2425ed";
    assertThat(result).isNotNull();
    assertThat(result.getCategories()).isNotNull();
    assertThat(result.getCategories().getResults())
        .containsExactlyInAnyOrder(
            new ReferenceIdKey(uuid, "cat-1"),
            new ReferenceIdKey(uuid, "cat-2"),
            new ReferenceIdKey(uuid, "cat-3"));

    assertThat(result.getProducts()).isNotNull();
    assertThat(result.getProducts().getResults())
        .containsExactlyInAnyOrder(
            new ReferenceIdKey(uuid, "one"),
            new ReferenceIdKey(uuid, "two"),
            new ReferenceIdKey(uuid, "three"));

    assertThat(result.getProductTypes()).isNotNull();
    assertThat(result.getProductTypes().getResults())
        .containsExactlyInAnyOrder(
            new ReferenceIdKey(uuid, "productType-1"),
            new ReferenceIdKey(uuid, "productType-2"),
            new ReferenceIdKey(uuid, "productType-3"));
  }

  @Test
  void deserialize_WithProductsOnlyCombinedResult_ShouldDeserializeCorrectly() throws IOException {
    // preparation
    final ObjectMapper objectMapper = new ObjectMapper();

    final JsonNode jsonNode =
        objectMapper.readTree(
            getClass().getClassLoader().getResource("combined-result-products-only.json"));

    final HttpResponse response = HttpResponse.of(200, jsonNode.toString());

    final CombinedResourceKeysRequest combinedResourceKeysRequest =
        mock(CombinedResourceKeysRequest.class);

    when(combinedResourceKeysRequest.deserialize(any())).thenCallRealMethod();

    // test
    final CombinedResult result = combinedResourceKeysRequest.deserialize(response);

    // assertion
    final String uuid = "c327eb8d-4924-4d47-9b31-d0caed2425ed";
    assertThat(result).isNotNull();
    assertThat(result.getCategories()).isNull();
    assertThat(result.getProductTypes()).isNull();
    assertThat(result.getProducts()).isNotNull();
    assertThat(result.getProducts().getResults())
        .containsExactlyInAnyOrder(
            new ReferenceIdKey(uuid, "one"),
            new ReferenceIdKey(uuid, "two"),
            new ReferenceIdKey(uuid, "three"));
  }

  @Test
  void deserialize_WithEmptyResultsCombinedResult_ShouldDeserializeCorrectly() throws IOException {
    // preparation
    final ObjectMapper objectMapper = new ObjectMapper();

    final JsonNode jsonNode =
        objectMapper.readTree(
            getClass().getClassLoader().getResource("combined-result-empty-results.json"));

    final HttpResponse response = HttpResponse.of(200, jsonNode.toString());

    final CombinedResourceKeysRequest combinedResourceKeysRequest =
        mock(CombinedResourceKeysRequest.class);

    when(combinedResourceKeysRequest.deserialize(any())).thenCallRealMethod();

    // test
    final CombinedResult result = combinedResourceKeysRequest.deserialize(response);

    // assertion
    assertThat(result).isNotNull();
    assertThat(result.getCategories()).isNull();
    assertThat(result.getProductTypes()).isNull();
    assertThat(result.getProducts()).isNotNull();
    assertThat(result.getProducts().getResults()).isEmpty();
  }

  @Test
  void deserialize_WithNullResponseBody_ShouldDeserializeCorrectly() {
    // preparation
    final HttpResponse response = HttpResponse.of(200, "null");

    final CombinedResourceKeysRequest combinedResourceKeysRequest =
        mock(CombinedResourceKeysRequest.class);

    when(combinedResourceKeysRequest.deserialize(any())).thenCallRealMethod();

    // test
    final CombinedResult result = combinedResourceKeysRequest.deserialize(response);

    // assertion
    assertThat(result).isNull();
  }
}
