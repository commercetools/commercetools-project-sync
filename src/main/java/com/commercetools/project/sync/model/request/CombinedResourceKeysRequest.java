package com.commercetools.project.sync.model.request;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

import com.commercetools.project.sync.model.response.CombinedResult;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.client.HttpRequestIntent;
import io.sphere.sdk.client.SphereRequest;
import io.sphere.sdk.http.HttpMethod;
import io.sphere.sdk.http.HttpResponse;
import io.sphere.sdk.json.SphereJsonUtils;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;

public class CombinedResourceKeysRequest implements SphereRequest<CombinedResult> {
  private final Set<String> productIds;
  private final Set<String> categoryIds;
  private final Set<String> productTypeIds;
  private static final int QUERY_LIMIT = 500;

  public CombinedResourceKeysRequest(
      @Nonnull final Set<String> productIds,
      @Nonnull final Set<String> categoryIds,
      @Nonnull final Set<String> productTypeIds) {
    this.productIds = requireNonNull(productIds);
    this.categoryIds = requireNonNull(categoryIds);
    this.productTypeIds = requireNonNull(productTypeIds);
  }

  @Nullable
  @Override
  public CombinedResult deserialize(final HttpResponse httpResponse) {
    final JsonNode rootJsonNode = SphereJsonUtils.parse(httpResponse.getResponseBody());
    if (rootJsonNode.isNull()) {
      return null;
    }
    final JsonNode results = rootJsonNode.get("data");
    return SphereJsonUtils.readObject(results, CombinedResult.class);
  }

  @Override
  public HttpRequestIntent httpRequestIntent() {

    if (productTypeIds.isEmpty() && productIds.isEmpty() && categoryIds.isEmpty()) {
      throw new IllegalArgumentException("No ids passed to build the graphql request body.");
    }

    final String productQuery = productIds.isEmpty() ? "" : createProductsGraphQlQuery(productIds);
    final String categoryQuery =
        categoryIds.isEmpty() ? "" : createCategoriesGraphQlQuery(categoryIds);
    final String productTypeQuery =
        productTypeIds.isEmpty() ? "" : createProductTypesGraphQlQuery(productTypeIds);

    final String queryValue =
        Stream.of(productQuery, categoryQuery, productTypeQuery)
            .filter(StringUtils::isNotBlank)
            .collect(joining(", ", "{ ", " }"));

    final String body = format("{\"query\": \"%s\"}", queryValue);

    return HttpRequestIntent.of(HttpMethod.POST, "/graphql", body);
  }

  @Nonnull
  private static String createProductsGraphQlQuery(@Nonnull final Set<String> productIds) {
    return format(
        "products(limit: %d, where: %s) { results { id key } }",
        QUERY_LIMIT, createWhereQuery(productIds));
  }

  @Nonnull
  private static String createCategoriesGraphQlQuery(@Nonnull final Set<String> categoryIds) {
    return format(
        "categories(limit: %d, where: %s) { results { id key } }",
        QUERY_LIMIT, createWhereQuery(categoryIds));
  }

  @Nonnull
  private static String createProductTypesGraphQlQuery(@Nonnull final Set<String> productTypeIds) {
    return format(
        "productTypes(limit: %d, where: %s) { results { id key } }",
        QUERY_LIMIT, createWhereQuery(productTypeIds));
  }

  @Nonnull
  private static String createWhereQuery(@Nonnull final Set<String> ids) {
    // The where in the graphql query should look like this in the end =>  `where: "id in (\"id1\",
    // \"id2\")"`
    // So we need an escaping backslash before the quote. So to add this:
    // We need 1 backslash (2 in java) to escape the quote in the graphql query.
    // We need 2 backslashes (4 in java) to escape the backslash in the JSON payload string.
    // We need 1 extra backslash to escape the quote in the java string
    // hence: 7 backslashes:
    final String backslashQuote = "\\\\\\\"";
    final String commaSeparatedIds =
        ids.stream()
            .collect(
                joining(
                    format("%s, %s", backslashQuote, backslashQuote),
                    backslashQuote,
                    backslashQuote));

    return createWhereQuery(commaSeparatedIds);
  }

  @Nonnull
  private static String createWhereQuery(@Nonnull final String commaSeparatedIds) {
    return format("\\\"id in (%s)\\\"", commaSeparatedIds);
  }
}
