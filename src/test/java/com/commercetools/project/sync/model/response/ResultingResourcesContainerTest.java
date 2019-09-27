package com.commercetools.project.sync.model.response;

import static io.sphere.sdk.utils.SphereInternalUtils.asSet;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ResultingResourcesContainerTest {
  @Test
  void newResultingResourcesContainer_WithEmptySet_ShouldInstantiateCorrectly() {
    // test
    final ResultingResourcesContainer resultingResourcesContainer =
        new ResultingResourcesContainer(emptySet());

    // assertion
    assertThat(resultingResourcesContainer.getResults()).isEmpty();
  }

  @Test
  void newResultingResourcesContainer_WithNonEmptySet_ShouldInstantiateCorrectly() {
    // test
    final ResultingResourcesContainer resultingResourcesContainer =
        new ResultingResourcesContainer(asSet(new ReferenceIdKey("id", "key")));

    // assertion
    assertThat(resultingResourcesContainer.getResults())
        .containsExactly(new ReferenceIdKey("id", "key"));
  }
}
