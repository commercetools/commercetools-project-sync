package com.commercetools.project.sync.service;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

public interface ReferencesService {

    @Nonnull
    CompletionStage<Map<String, String>> getReferenceKeys(
        @Nonnull final List<String> productIds,
        @Nonnull final List<String> categoryIds,
        @Nonnull final List<String> productTypeIds);
}
