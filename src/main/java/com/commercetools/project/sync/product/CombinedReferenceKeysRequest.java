package com.commercetools.project.sync.product;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.client.HttpRequestIntent;
import io.sphere.sdk.client.SphereRequest;
import io.sphere.sdk.http.HttpMethod;
import io.sphere.sdk.http.HttpResponse;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.models.Base;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static java.util.stream.Collectors.joining;

public class CombinedReferenceKeysRequest extends Base implements SphereRequest<CombinedReferenceKeys> {
    private final List<String> productIds;
    private final List<String> categoryIds;
    private final List<String> productTypeIds;

    public CombinedReferenceKeysRequest(@Nonnull final List<String> productIds,
                                        @Nonnull final List<String> categoryIds,
                                        @Nonnull final List<String> productTypeIds) {
        this.productIds = Validate.notEmpty(productIds);
        this.categoryIds = Validate.notEmpty(categoryIds);
        this.productTypeIds = Validate.notEmpty(productTypeIds);
    }

    @Nullable
    @Override
    public CombinedReferenceKeys deserialize(final HttpResponse httpResponse) {
        final JsonNode rootJsonNode = SphereJsonUtils.parse(httpResponse.getResponseBody());
        final JsonNode results = rootJsonNode.get("data");
        return SphereJsonUtils.readObject(results, new TypeReference<CombinedReferenceKeys>() {
        });
    }

    @Override
    public HttpRequestIntent httpRequestIntent() {

        final String body = String.format("%s\n,%s\n,%s",
            createProductsGraphQlQuery(productIds),
            createCategoriesGraphQlQuery(categoryIds),
            createProductTypesGraphQlQuery(productTypeIds));

        return HttpRequestIntent.of(HttpMethod.POST, "/graphql", body);
    }


    private static String createProductsGraphQlQuery(@Nonnull final List<String> productIds) {
        return String.format("{\n" +
            "       products(where: %s) {\n" +
            "           results {\n" +
            "               key\n" +
            "           }\n" +
            "       }\n" +
            "   }", createWhereQuery(productIds));

    }

    private static String createCategoriesGraphQlQuery(@Nonnull final List<String> categoryIds) {
        return String.format("{\n" +
            "       categories(where: %s) {\n" +
            "           results {\n" +
            "               key\n" +
            "           }\n" +
            "       }\n" +
            "   }", createWhereQuery(categoryIds));

    }

    private static String createProductTypesGraphQlQuery(@Nonnull final List<String> productTypeIds) {
        return String.format("{\n" +
            "       productTypes(where: %s) {\n" +
            "           results {\n" +
            "               key\n" +
            "           }\n" +
            "       }\n" +
            "   }", createWhereQuery(productTypeIds));

    }

    private static String createWhereQuery(@Nonnull final List<String> ids) {
        final String commaSeparatedIds = ids
            .stream()
            .distinct()
            .collect(joining("\", \"", "\"", "\""));

        return createWhereQuery(commaSeparatedIds);

    }

    private static String createWhereQuery(@Nonnull final String commaSeparatedIds) {
        return String.format("id in (%s)", commaSeparatedIds);
    }

}
