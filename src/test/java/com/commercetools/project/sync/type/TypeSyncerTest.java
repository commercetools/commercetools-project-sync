package com.commercetools.project.sync.type;

import static com.commercetools.project.sync.util.TestUtils.getMockedClock;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.types.TypeSync;
import io.sphere.sdk.client.SphereApiConfig;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.TypeDraftBuilder;
import io.sphere.sdk.types.queries.TypeQuery;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.org.lidalia.slf4jtest.LoggingEvent;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

class TypeSyncerTest {
  private final TestLogger syncerTestLogger = TestLoggerFactory.getTestLogger(TypeSyncer.class);

  @BeforeEach
  void setup() {
    syncerTestLogger.clearAll();
  }

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

  @Test
  void syncWithError_ShouldCallErrorCallback() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    final SphereClient targetClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereApiConfig.of("source-project"));
    when(targetClient.getConfig()).thenReturn(SphereApiConfig.of("target-project"));

    final List<Type> typePage =
        Collections.singletonList(readObjectFromResource("type-without-key.json", Type.class));

    final PagedQueryResult<Type> pagedQueryResult = mock(PagedQueryResult.class);
    when(pagedQueryResult.getResults()).thenReturn(typePage);
    when(sourceClient.execute(any(TypeQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(pagedQueryResult));

    // test
    final TypeSyncer typeSyncer = TypeSyncer.of(sourceClient, targetClient, mock(Clock.class));
    typeSyncer.sync(null, true).toCompletableFuture().join();

    // assertion
    final LoggingEvent errorLog = syncerTestLogger.getAllLoggingEvents().get(0);
    assertThat(errorLog.getMessage())
        .isEqualTo(
            "Error when trying to sync type. Existing key: <<not present>>. Update actions: []");
    assertThat(errorLog.getThrowable().get().getMessage())
        .isEqualTo(
            "TypeDraft with name: LocalizedString(en -> typeName) doesn't have a key. Please make sure all type drafts have keys.");
  }
}
