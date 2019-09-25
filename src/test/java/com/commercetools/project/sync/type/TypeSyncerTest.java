package com.commercetools.project.sync.type;

import static com.commercetools.project.sync.util.TestUtils.getMockedClock;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.commercetools.sync.types.TypeSync;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.TypeDraftBuilder;
import io.sphere.sdk.types.queries.TypeQuery;
import java.util.List;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;

class TypeSyncerTest {
  @Test
  void of_ShouldCreateTypeSyncerInstance() {
    // test
    final TypeSyncer typeSyncer =
        TypeSyncer.of(mock(SphereClient.class), mock(SphereClient.class), getMockedClock());

    // assertions
    assertThat(typeSyncer).isNotNull();
    assertThat(typeSyncer.getQuery()).isEqualTo(TypeQuery.of());
    assertThat(typeSyncer.getSync()).isInstanceOf(TypeSync.class);
  }

  @Test
  void transform_ShouldConvertResourcesToDrafts() {
    // preparation
    final TypeSyncer typeSyncer =
        TypeSyncer.of(mock(SphereClient.class), mock(SphereClient.class), getMockedClock());
    final List<Type> typePage =
        asList(
            readObjectFromResource("type-key-1.json", Type.class),
            readObjectFromResource("type-key-2.json", Type.class));

    // test
    final CompletionStage<List<TypeDraft>> draftsFromPageStage = typeSyncer.transform(typePage);

    // assertions
    assertThat(draftsFromPageStage)
        .isCompletedWithValue(
            typePage
                .stream()
                .map(
                    type ->
                        TypeDraftBuilder.of(
                                type.getKey(), type.getName(), type.getResourceTypeIds())
                            .fieldDefinitions(type.getFieldDefinitions())
                            .description(type.getDescription()))
                .map(TypeDraftBuilder::build)
                .collect(toList()));
  }

  @Test
  void getQuery_ShouldBuildTypeQuery() {
    // preparation
    final TypeSyncer typeSyncer =
        TypeSyncer.of(mock(SphereClient.class), mock(SphereClient.class), getMockedClock());

    // test
    final TypeQuery query = typeSyncer.getQuery();

    // assertion
    assertThat(query).isEqualTo(TypeQuery.of());
  }
}
