package com.commercetools.project.sync.service;

import com.commercetools.project.sync.model.LastSyncCustomObject;
import io.sphere.sdk.customobjects.CustomObject;

import javax.annotation.Nonnull;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface CustomObjectService {
    @Nonnull
    CompletionStage<ZonedDateTime> getCurrentCtpTimestamp();

    @Nonnull
    CompletionStage<Optional<CustomObject<LastSyncCustomObject>>> getLastSyncCustomObject(
        @Nonnull final String sourceProjectKey,
        @Nonnull final String syncModuleName);

    @Nonnull
    CompletionStage<CustomObject<LastSyncCustomObject>> persistLastSyncCustomObject(
        @Nonnull final String sourceProjectKey,
        @Nonnull final String syncModuleName,
        @Nonnull final LastSyncCustomObject lastSyncCustomObject);
}
