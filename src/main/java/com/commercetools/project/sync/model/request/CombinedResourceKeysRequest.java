package com.commercetools.project.sync.model.request;

import static java.util.stream.Collectors.joining;

import com.commercetools.project.sync.model.response.CombinedResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.client.HttpRequestIntent;
import io.sphere.sdk.client.SphereRequest;
import io.sphere.sdk.http.HttpMethod;
import io.sphere.sdk.http.HttpResponse;
import io.sphere.sdk.json.SphereJsonUtils;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CombinedResourceKeysRequest implements SphereRequest<CombinedResult> {
  private final Set<String> productIds;
  private final Set<String> categoryIds;
  private final Set<String> productTypeIds;

  public CombinedResourceKeysRequest(
      @Nonnull final Set<String> productIds,
      @Nonnull final Set<String> categoryIds,
      @Nonnull final Set<String> productTypeIds) {
    this.productIds = productIds;
    this.categoryIds = categoryIds;
    this.productTypeIds = productTypeIds;
  }

  @Nullable
  @Override
  public CombinedResult deserialize(final HttpResponse httpResponse) {
    final JsonNode rootJsonNode = SphereJsonUtils.parse(httpResponse.getResponseBody());
    final JsonNode results = rootJsonNode.get("data");
    return SphereJsonUtils.readObject(results, new TypeReference<CombinedResult>() {});
  }

  @Override
  public HttpRequestIntent httpRequestIntent() {
    final String body =
        String.format(
            "{\"query\": \"{%s, %s, %s}\"}",
            createProductsGraphQlQuery(productIds),
            createCategoriesGraphQlQuery(categoryIds),
            createProductTypesGraphQlQuery(productTypeIds));

    return HttpRequestIntent.of(HttpMethod.POST, "/graphql", body);
  }

  private static String createProductsGraphQlQuery(@Nonnull final Set<String> productIds) {
    return String.format(
        "products(where: %s) { results { id key } }", createWhereQuery(productIds));
  }

  private static String createCategoriesGraphQlQuery(@Nonnull final Set<String> categoryIds) {
    return String.format(
        "categories(where: %s) { results { id key } }", createWhereQuery(categoryIds));
  }

  private static String createProductTypesGraphQlQuery(@Nonnull final Set<String> productTypeIds) {
    return String.format(
        "productTypes(where: %s) { results { id key } }", createWhereQuery(productTypeIds));
  }

  private static String createWhereQuery(@Nonnull final Set<String> ids) {
    final String commaSeparatedIds =
        ids.stream().distinct().collect(joining("\\\\\\\", \\\\\\\"", "\\\\\\\"", "\\\\\\\""));

    return createWhereQuery(commaSeparatedIds);
  }

  private static String createWhereQuery(@Nonnull final String commaSeparatedIds) {
    return String.format("\\\"id in (%s)\\\"", commaSeparatedIds);
  }
}
