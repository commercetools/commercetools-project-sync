package com.commercetools.project.sync.type;

import com.commercetools.project.sync.Syncer;
import com.commercetools.sync.types.TypeSync;
import com.commercetools.sync.types.TypeSyncOptions;
import com.commercetools.sync.types.TypeSyncOptionsBuilder;
import com.commercetools.sync.types.helpers.TypeSyncStatistics;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.TypeDraftBuilder;
import io.sphere.sdk.types.queries.TypeQuery;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TypeSyncer
    extends Syncer<Type, TypeDraft, TypeSyncStatistics, TypeSyncOptions, TypeQuery, TypeSync> {

  private static final Logger LOGGER = LoggerFactory.getLogger(TypeSyncer.class);

  /** Instantiates a {@link Syncer} instance. */
  private TypeSyncer(@Nonnull final TypeSync typeSync, @Nonnull final TypeQuery query) {
    super(typeSync, query);
  }

  @Nonnull
  public static TypeSyncer of(@Nonnull final SphereClient client) {

    final TypeSyncOptions syncOptions =
        TypeSyncOptionsBuilder.of(client)
            .errorCallback(LOGGER::error)
            .warningCallback(LOGGER::warn)
            .build();

    final TypeSync typeSync = new TypeSync(syncOptions);

    return new TypeSyncer(typeSync, TypeQuery.of());
  }

  @Nonnull
  @Override
  protected List<TypeDraft> transformResourcesToDrafts(@Nonnull final List<Type> page) {
    return page.stream().map(TypeSyncer::typeToDraft).collect(Collectors.toList());
  }

  @Nonnull
  private static TypeDraft typeToDraft(@Nonnull final Type type) {
    return TypeDraftBuilder.of(type.getKey(), type.getName(), type.getResourceTypeIds())
        .description(type.getDescription())
        .fieldDefinitions(type.getFieldDefinitions())
        .build();
  }
}
