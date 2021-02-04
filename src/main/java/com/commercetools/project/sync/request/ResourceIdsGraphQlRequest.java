package com.commercetools.project.sync.request;

import com.commercetools.sync.commons.helpers.GraphQlBaseRequestImpl;
import com.commercetools.sync.commons.models.GraphQlQueryResources;
import com.commercetools.sync.commons.models.ResourceKeyIdGraphQlResult;
import io.sphere.sdk.http.HttpResponse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class ResourceIdsGraphQlRequest extends GraphQlBaseRequestImpl<ResourceKeyIdGraphQlResult> {
  protected final Set<String> idsToSearch;
  protected final GraphQlQueryResources resource;

  public ResourceIdsGraphQlRequest(
      @Nonnull final Set<String> idsToSearch, @Nonnull final GraphQlQueryResources resource) {
    this.idsToSearch = requireNonNull(idsToSearch);
    this.resource = resource;
  }

  @Nullable
  @Override
  public ResourceKeyIdGraphQlResult deserialize(final HttpResponse httpResponse) {
    return deserializeWithResourceName(
        httpResponse, resource.getName(), ResourceKeyIdGraphQlResult.class);
  }

  /**
   * This method builds a string matching the required format to query a set of ids matching given
   * keys of a resource using the CTP graphql API
   *
   * @return a string representing a graphql query
   */
  @Nonnull
  @Override
  protected String buildQueryString() {

    return format(
        "%s(limit: %d, where: \\\"%s\\\", sort: [\\\"id asc\\\"]) { results { id key } }",
        this.resource.getName(), this.limit, createWhereQuery(idsToSearch));
  }

  @Nonnull
  private String createWhereQuery(@Nonnull final Set<String> ids) {
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
            .filter(id -> !isBlank(id))
            .collect(
                joining(
                    format("%s, %s", backslashQuote, backslashQuote),
                    backslashQuote,
                    backslashQuote));

    String whereQuery = createWhereQuery(commaSeparatedIds);
    return isBlank(this.queryPredicate)
        ? whereQuery
        : format("%s AND %s", whereQuery, queryPredicate);
  }

  @Nonnull
  private String createWhereQuery(@Nonnull final String commaSeparatedIds) {
    return format("id in (%s)", commaSeparatedIds);
  }
}
