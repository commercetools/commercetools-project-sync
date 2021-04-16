package com.commercetools.project.sync.model;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.commercetools.sync.commons.helpers.ResourceKeyIdGraphQlRequest;
import com.commercetools.sync.commons.models.GraphQlBaseRequest;
import com.commercetools.sync.commons.models.GraphQlQueryResources;
import com.commercetools.sync.commons.models.ResourceIdsGraphQlRequest;
import com.commercetools.sync.commons.models.ResourceKeyIdGraphQlResult;
import io.sphere.sdk.client.HttpRequestIntent;
import io.sphere.sdk.http.HttpMethod;
import io.sphere.sdk.http.HttpResponse;
import io.sphere.sdk.http.StringHttpRequestBody;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ResourceIdsGraphQlRequestTest {

  @Test
  void newResourceIdsGraphQlRequest_WithEmptySet_ShouldCreateAnInstance() {
    // preparation
    final Set<String> productIds = new HashSet<>();

    // test
    final ResourceIdsGraphQlRequest resourceIdsGraphQlRequest =
        new ResourceIdsGraphQlRequest(productIds, GraphQlQueryResources.PRODUCTS);

    // assertions
    assertThat(resourceIdsGraphQlRequest).isExactlyInstanceOf(ResourceIdsGraphQlRequest.class);
  }

  @Test
  void newResourceIdsGraphQlRequest_WithNullSet_ShouldThrowNPE() {
    // preparation
    final Set<String> productIds = null;

    // test & assertion
    assertThatThrownBy(
            () -> new ResourceIdsGraphQlRequest(productIds, GraphQlQueryResources.PRODUCTS))
        .isExactlyInstanceOf(NullPointerException.class);
  }

  @Test
  void httpRequestIntent_WithIds_ShouldReturnCorrectQueryString() {
    // preparation
    final Set<String> idsToSearch = new HashSet<>();
    idsToSearch.add("product1");
    idsToSearch.add("product2");
    final ResourceIdsGraphQlRequest resourceIdsGraphQlRequest =
        new ResourceIdsGraphQlRequest(idsToSearch, GraphQlQueryResources.PRODUCTS);

    // test
    final HttpRequestIntent httpRequestIntent = resourceIdsGraphQlRequest.httpRequestIntent();

    // assertions
    assertThat(httpRequestIntent.getBody()).isExactlyInstanceOf(StringHttpRequestBody.class);
    assertThat(httpRequestIntent.getHttpMethod()).isEqualByComparingTo(HttpMethod.POST);
    final StringHttpRequestBody requestBody = (StringHttpRequestBody) httpRequestIntent.getBody();
    assertThat(requestBody).isNotNull();
    assertThat(requestBody.getString())
        .isEqualTo(
            "{\"query\": \"{products(limit: 500, where: \\\"id"
                + " in (\\\\\\\"product2\\\\\\\", \\\\\\\"product1\\\\\\\")\\\", sort: [\\\"id asc\\\"]) { results { "
                + "id "
                + "key } "
                + "}}\"}");
  }

  @Test
  void httpRequestIntent_WithSomeEmptyAndNullIds_ShouldReturnCorrectQueryString() {
    // preparation
    final Set<String> idsToSearch = new HashSet<>();
    idsToSearch.add("product1");
    idsToSearch.add("");
    idsToSearch.add("product2");
    idsToSearch.add(null);
    final ResourceIdsGraphQlRequest resourceIdsGraphQlRequest =
        new ResourceIdsGraphQlRequest(idsToSearch, GraphQlQueryResources.PRODUCTS);

    // test
    final HttpRequestIntent httpRequestIntent = resourceIdsGraphQlRequest.httpRequestIntent();

    // assertions
    assertThat(httpRequestIntent.getBody()).isExactlyInstanceOf(StringHttpRequestBody.class);
    assertThat(httpRequestIntent.getHttpMethod()).isEqualByComparingTo(HttpMethod.POST);
    final StringHttpRequestBody requestBody = (StringHttpRequestBody) httpRequestIntent.getBody();
    assertThat(requestBody).isNotNull();
    assertThat(requestBody.getString())
        .isEqualTo(
            "{\"query\": \"{products(limit: 500, where: \\\"id"
                + " in (\\\\\\\"product2\\\\\\\", \\\\\\\"product1\\\\\\\")\\\", sort: [\\\"id asc\\\"]) { results { "
                + "id "
                + "key } "
                + "}}\"}");
  }

  @Test
  void httpRequestIntent_WithKeyAndExplicitLimit_ShouldReturnCorrectQueryString() {
    // preparation
    final GraphQlBaseRequest<ResourceKeyIdGraphQlResult> resourceIdGraphQlRequest =
        new ResourceIdsGraphQlRequest(singleton("product1"), GraphQlQueryResources.PRODUCTS)
            .withLimit(10);

    // test
    final HttpRequestIntent httpRequestIntent = resourceIdGraphQlRequest.httpRequestIntent();

    // assertions
    assertThat(httpRequestIntent.getBody()).isExactlyInstanceOf(StringHttpRequestBody.class);
    assertThat(httpRequestIntent.getHttpMethod()).isEqualByComparingTo(HttpMethod.POST);
    final StringHttpRequestBody requestBody = (StringHttpRequestBody) httpRequestIntent.getBody();
    assertThat(requestBody).isNotNull();
    assertThat(requestBody.getString())
        .isEqualTo(
            "{\"query\": \"{products(limit: 10, where: \\\"id"
                + " in (\\\\\\\"product1\\\\\\\")\\\", sort: [\\\"id asc\\\"]) { results { id key } "
                + "}}\"}");
  }

  @Test
  void httpRequestIntent_WithIdAndPredicate_ShouldReturnCorrectQueryString() {
    // preparation
    final GraphQlBaseRequest<ResourceKeyIdGraphQlResult> resourceIdsGraphQlRequest =
        new ResourceIdsGraphQlRequest(singleton("product1"), GraphQlQueryResources.PRODUCTS)
            .withPredicate("id > \\\\\\\"id" + "\\\\\\\"");

    // test
    final HttpRequestIntent httpRequestIntent = resourceIdsGraphQlRequest.httpRequestIntent();

    // assertions
    assertThat(httpRequestIntent.getBody()).isExactlyInstanceOf(StringHttpRequestBody.class);
    assertThat(httpRequestIntent.getHttpMethod()).isEqualByComparingTo(HttpMethod.POST);
    final StringHttpRequestBody requestBody = (StringHttpRequestBody) httpRequestIntent.getBody();
    assertThat(requestBody).isNotNull();
    assertThat(requestBody.getString())
        .isEqualTo(
            "{\"query\": \"{products(limit: 500, where: \\\"id"
                + " in (\\\\\\\"product1\\\\\\\") AND id > \\\\\\\"id\\\\\\\"\\\", sort: [\\\"id asc\\\"])"
                + " { results { id key } }}\"}");
  }

  @Test
  void deserialize_WithEmptyResult_ShouldReturnNull() {
    // preparation
    final HttpResponse httpResponse = HttpResponse.of(200, "null");
    final ResourceIdsGraphQlRequest resourceIdsGraphQlRequest =
        new ResourceIdsGraphQlRequest(singleton("product1"), GraphQlQueryResources.PRODUCTS);

    // test
    final ResourceKeyIdGraphQlResult result = resourceIdsGraphQlRequest.deserialize(httpResponse);

    // assertions
    assertThat(result).isNull();
  }

  @Test
  void deserialize_WithEmptyResult_ShouldDeserializeCorrectly() {
    // preparation
    String jsonAsString = "{\"data\":{\"products\":{\"results\":[]}}}";

    final HttpResponse httpResponse = HttpResponse.of(200, jsonAsString);

    final ResourceIdsGraphQlRequest resourceIdsGraphQlRequest =
        new ResourceIdsGraphQlRequest(singleton("product1"), GraphQlQueryResources.PRODUCTS);

    // test
    final ResourceKeyIdGraphQlResult result = resourceIdsGraphQlRequest.deserialize(httpResponse);

    // assertions
    assertThat(result).isNotNull();
    assertThat(result.getResults()).isEmpty();
  }

  @Test
  void deserialize_WithSingleResult_ShouldReturnSingletonMap() {
    // preparation
    String jsonAsString =
        "{\"data\":{\"products\":{\"results\":[{\"id\":\"product1\",\"key\":\"key-1\"}]}}}";

    final HttpResponse httpResponse = HttpResponse.of(200, jsonAsString);

    final ResourceKeyIdGraphQlRequest resourceKeyIdGraphQlRequest =
        new ResourceKeyIdGraphQlRequest(singleton("product1"), GraphQlQueryResources.PRODUCTS);

    // test
    final ResourceKeyIdGraphQlResult result = resourceKeyIdGraphQlRequest.deserialize(httpResponse);

    // assertions
    assertThat(result).isNotNull();
    assertThat(result.getResults()).hasSize(1);
    assertThat(result.getResults()).extracting("key").containsExactly("key-1");
    assertThat(result.getResults()).extracting("id").containsExactly("product1");
  }

  @Test
  void deserialize_WithMultipleResults_ShouldReturnCorrectResult() {
    // preparation
    String jsonAsString =
        "{\"data\":{\"products\":{\"results\":[{\"id\":\"product1\",\"key\":\"key-1\"},"
            + "{\"id\":\"product2\",\"key\":\"key-2\"},{\"id\":\"product3\",\"key\":\"key-3\"}]}}}";

    final HttpResponse httpResponse = HttpResponse.of(200, jsonAsString);

    final ResourceIdsGraphQlRequest resourceIdsGraphQlRequest =
        new ResourceIdsGraphQlRequest(singleton("key-1"), GraphQlQueryResources.PRODUCTS);

    // test
    final ResourceKeyIdGraphQlResult result = resourceIdsGraphQlRequest.deserialize(httpResponse);

    // assertions
    assertThat(result).isNotNull();
    assertThat(result.getResults()).hasSize(3);
    assertThat(result.getResults())
        .extracting("key")
        .containsExactlyInAnyOrder("key-1", "key-2", "key-3");
    assertThat(result.getResults())
        .extracting("id")
        .containsExactlyInAnyOrder("product1", "product2", "product3");
  }
}
