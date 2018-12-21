package com.commercetools.project.sync.type;

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
import org.junit.Test;

public class TypeSyncerTest {
  @Test
  public void of_ShouldCreateTypeSyncerInstance() {
    // preparation
    final SphereClient client = mock(SphereClient.class);

    // test
    final TypeSyncer typeSyncer = TypeSyncer.of(client);

    // assertions
    assertThat(typeSyncer).isNotNull();
    assertThat(typeSyncer.getQuery()).isEqualTo(TypeQuery.of());
    assertThat(typeSyncer.getSync()).isInstanceOf(TypeSync.class);
  }

  @Test
  public void transformResourcesToDrafts_ShouldConvertResourcesToDrafts() {
    // preparation
    final SphereClient client = mock(SphereClient.class);
    final TypeSyncer typeSyncer = TypeSyncer.of(client);
    final List<Type> typePage =
        asList(
            readObjectFromResource("type-key-1.json", Type.class),
            readObjectFromResource("type-key-2.json", Type.class));

    // test
    final List<TypeDraft> draftsFromPage = typeSyncer.transformResourcesToDrafts(typePage);

    // assertions
    assertThat(draftsFromPage)
        .isEqualTo(
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
}
