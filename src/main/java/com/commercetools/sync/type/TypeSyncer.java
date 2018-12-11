package com.commercetools.sync.type;

import static com.commercetools.sync.utils.SphereClientUtils.CTP_TARGET_CLIENT;

import com.commercetools.sync.Syncer;
import com.commercetools.sync.types.TypeSync;
import com.commercetools.sync.types.TypeSyncOptions;
import com.commercetools.sync.types.TypeSyncOptionsBuilder;
import com.commercetools.sync.types.helpers.TypeSyncStatistics;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.TypeDraftBuilder;
import io.sphere.sdk.types.queries.TypeQuery;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TypeSyncer
        extends Syncer<Type, TypeDraft, TypeSyncStatistics, TypeSyncOptions, TypeQuery, TypeSync> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TypeSyncer.class);

    /** Instantiates a {@link Syncer} instance. */
    public TypeSyncer() {
        super(
                new TypeSync(
                        TypeSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                .errorCallback(LOGGER::error)
                                .warningCallback(LOGGER::warn)
                                .build()),
                TypeQuery.of());
    }

    @Nonnull
    @Override
    protected List<TypeDraft> getDraftsFromPage(@Nonnull final List<Type> page) {
        return page.stream().map(TypeSyncer::typeToDraft).collect(Collectors.toList());
    }

    private static TypeDraft typeToDraft(@Nonnull final Type type) {
        return TypeDraftBuilder.of(type.getKey(), type.getName(), type.getResourceTypeIds())
                .description(type.getDescription())
                .fieldDefinitions(type.getFieldDefinitions())
                .build();
    }
}
