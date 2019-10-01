package com.commercetools.project.sync.model.response;

import static io.sphere.sdk.utils.SphereInternalUtils.asSet;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CombinedResultTest {

  @Test
  void newCombinedResult_WithAllNullContainers_ShouldInstantiateCorrectly() {
    // test
    final CombinedResult combinedResult = new CombinedResult(null, null, null);

    // assertion
    assertThat(combinedResult.getCategories()).isNull();
    assertThat(combinedResult.getProducts()).isNull();
    assertThat(combinedResult.getProductTypes()).isNull();
  }

  @Test
  void newCombinedResult_WithNonNullContainers_ShouldInstantiateCorrectly() {
    // test
    final CombinedResult combinedResult =
        new CombinedResult(
            new ResultingResourcesContainer(emptySet()),
            new ResultingResourcesContainer(asSet(new ReferenceIdKey("id", "key"))),
            null);

    // assertion
    assertThat(combinedResult.getCategories()).isNotNull();
    assertThat(combinedResult.getCategories().getResults())
        .containsExactly(new ReferenceIdKey("id", "key"));
    assertThat(combinedResult.getProducts()).isNotNull();
    assertThat(combinedResult.getProducts().getResults()).isEmpty();
    assertThat(combinedResult.getProductTypes()).isNull();
  }
}
