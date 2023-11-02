package com.commercetools.project.sync.type;

import static com.commercetools.project.sync.util.TestUtils.getMockedClock;
import static com.commercetools.project.sync.util.TestUtils.readObjectFromResource;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ByProjectKeyTypesGet;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.defaultconfig.ApiRootBuilder;
import com.commercetools.api.models.type.Type;
import com.commercetools.api.models.type.TypeDraft;
import com.commercetools.api.models.type.TypeDraftBuilder;
import com.commercetools.api.models.type.TypePagedQueryResponse;
import com.commercetools.api.models.type.TypePagedQueryResponseBuilder;
import com.commercetools.sync.types.TypeSync;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
    // preparation
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    when(sourceClient.types()).thenReturn(mock());
    when(sourceClient.types().get()).thenReturn(mock());

    // test
    final TypeSyncer typeSyncer =
        TypeSyncer.of(sourceClient, mock(ProjectApiRoot.class), getMockedClock());

    // assertions
    assertThat(typeSyncer).isNotNull();
    assertThat(typeSyncer.getSync()).isInstanceOf(TypeSync.class);
  }

  @Test
  void transform_ShouldConvertResourcesToDrafts() {
    // preparation
    final TypeSyncer typeSyncer =
        TypeSyncer.of(mock(ProjectApiRoot.class), mock(ProjectApiRoot.class), getMockedClock());
    final List<Type> typePage =
        List.of(
            readObjectFromResource("type-key-1.json", Type.class),
            readObjectFromResource("type-key-2.json", Type.class));

    // test
    final List<TypeDraft> draftsFromPageStage =
        typeSyncer.transform(typePage).toCompletableFuture().join();

    // assertions
    assertThat(draftsFromPageStage)
        .isEqualTo(
            typePage.stream()
                .map(
                    type ->
                        TypeDraftBuilder.of()
                            .key(type.getKey())
                            .name(type.getName())
                            .resourceTypeIds(type.getResourceTypeIds())
                            .fieldDefinitions(type.getFieldDefinitions())
                            .description(type.getDescription()))
                .map(TypeDraftBuilder::build)
                .collect(toList()));
  }

  @Test
  void getQuery_ShouldBuildTypeQuery() {
    // preparation
    final ProjectApiRoot apiRoot =
        ApiRootBuilder.of().withApiBaseUrl("baseURl").build("testProjectKey");
    final TypeSyncer typeSyncer =
        TypeSyncer.of(apiRoot, mock(ProjectApiRoot.class), getMockedClock());

    // test + assertion
    assertThat(typeSyncer.getQuery()).isInstanceOf(ByProjectKeyTypesGet.class);
  }

  @Test
  void transform_WhenNoKeyIsProvided_ShouldLogErrorAndContinue() {
    // preparation
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    final List<Type> typePage =
        List.of(readObjectFromResource("type-without-key.json", Type.class));

    // test
    final TypeSyncer typeSyncer =
        TypeSyncer.of(sourceClient, mock(ProjectApiRoot.class), getMockedClock());
    final List<TypeDraft> typeDrafts = typeSyncer.transform(typePage).toCompletableFuture().join();

    // assertion
    assertThat(typeDrafts).isEqualTo(Collections.emptyList());
    assertThat(syncerTestLogger.getAllLoggingEvents())
        .anySatisfy(
            loggingEvent -> {
              assertThat(loggingEvent.getMessage()).contains("TypeDraft: key is missing");
              assertThat(loggingEvent.getThrowable().isPresent()).isTrue();
              assertThat(loggingEvent.getThrowable().get())
                  .isInstanceOf(NullPointerException.class);
            });
  }

  @Test
  void sync_whenKeyIsBlank_shouldCallErrorCallback() {
    // preparation
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    final Type type = readObjectFromResource("type-key-1.json", Type.class);
    type.setKey("");

    final TypePagedQueryResponse response =
        TypePagedQueryResponseBuilder.of()
            .results(List.of(type))
            .limit(20L)
            .offset(0L)
            .count(1L)
            .build();

    final ApiHttpResponse apiHttpResponse = mock(ApiHttpResponse.class);
    when(apiHttpResponse.getBody()).thenReturn(response);
    final ByProjectKeyTypesGet byProjectKeyTypesGet = mock(ByProjectKeyTypesGet.class);
    when(byProjectKeyTypesGet.execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(byProjectKeyTypesGet.withLimit(anyInt())).thenReturn(byProjectKeyTypesGet);
    when(byProjectKeyTypesGet.withWithTotal(anyBoolean())).thenReturn(byProjectKeyTypesGet);
    when(byProjectKeyTypesGet.withSort(anyString())).thenReturn(byProjectKeyTypesGet);
    when(sourceClient.types()).thenReturn(mock());
    when(sourceClient.types().get()).thenReturn(byProjectKeyTypesGet);

    // test
    final TypeSyncer typeSyncer =
        TypeSyncer.of(sourceClient, mock(ProjectApiRoot.class), getMockedClock());
    typeSyncer.sync(null, true);

    // assertions
    final LoggingEvent errorLog = syncerTestLogger.getAllLoggingEvents().get(1);
    assertThat(errorLog.getMessage())
        .isEqualTo(
            "Error when trying to sync type. Existing key: <<not present>>. Update actions: []");
    assertThat(errorLog.getThrowable().get().getMessage())
        .isEqualTo(
            format(
                "TypeDraft with name: %s doesn't have a key. Please make sure all type drafts have keys.",
                type.getName()));
  }
}
