package com.commercetools.project.sync.type;

import com.commercetools.project.sync.Syncer;
import com.commercetools.project.sync.service.CustomObjectService;
import com.commercetools.project.sync.service.impl.CustomObjectServiceImpl;
import com.commercetools.sync.types.TypeSync;
import com.commercetools.sync.types.TypeSyncOptions;
import com.commercetools.sync.types.TypeSyncOptionsBuilder;
import com.commercetools.sync.types.helpers.TypeSyncStatistics;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.TypeDraftBuilder;
import io.sphere.sdk.types.queries.TypeQuery;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public final class TypeSyncer
    extends Syncer<Type, TypeDraft, TypeSyncStatistics, TypeSyncOptions, TypeQuery, TypeSync> {

  private static final Logger LOGGER = LoggerFactory.getLogger(TypeSyncer.class);

  /** Instantiates a {@link Syncer} instance. */
  private TypeSyncer(
      @Nonnull final TypeSync typeSync,
      @Nonnull final SphereClient sourceClient,
      @Nonnull final SphereClient targetClient,
      @Nonnull final CustomObjectService customObjectService,
      @Nonnull final Clock clock) {
    super(typeSync, sourceClient, targetClient, customObjectService, clock);
  }

  @Nonnull
  public static TypeSyncer of(
      @Nonnull final SphereClient sourceClient,
      @Nonnull final SphereClient targetClient,
      @Nonnull final Clock clock) {

    final TypeSyncOptions syncOptions =
        TypeSyncOptionsBuilder.of(targetClient)
            .errorCallback((exception, newResourceDraft, oldResource, updateActions) -> {
              LOGGER.error(format(
                      "Error when trying to sync types. Existing type key: %s. Update actions: %s",
                      oldResource.map(Type::getKey).orElse(""),
                      updateActions.stream()
                                   .map(Object::toString)
                                   .collect(Collectors.joining(","))
                      )
                      , exception);
            })
            .warningCallback((exception, newResourceDraft, oldResource) -> {
              LOGGER.warn(format(
                      "Warning when trying to sync types. Existing type key: %s",
                      oldResource.map(Type::getKey).orElse("")
              ), exception);
            })
            .build();

    final TypeSync typeSync = new TypeSync(syncOptions);

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(targetClient);

    return new TypeSyncer(typeSync, sourceClient, targetClient, customObjectService, clock);
  }

  @Nonnull
  @Override
  protected TypeQuery getQuery() {
    return TypeQuery.of();
  }

  @Nonnull
  @Override
  protected CompletionStage<List<TypeDraft>> transform(@Nonnull final List<Type> page) {
    return CompletableFuture.completedFuture(
        page.stream().map(TypeSyncer::typeToDraft).collect(Collectors.toList()));
  }

  @Nonnull
  private static TypeDraft typeToDraft(@Nonnull final Type type) {
    return TypeDraftBuilder.of(type.getKey(), type.getName(), type.getResourceTypeIds())
        .description(type.getDescription())
        .fieldDefinitions(type.getFieldDefinitions())
        .build();
  }
}
