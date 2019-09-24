package com.commercetools.project.sync.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;

public interface ReferencesService {

  @Nonnull
  CompletionStage<Map<String, String>> getReferenceKeys(
      @Nonnull final List<String> productIds,
      @Nonnull final List<String> categoryIds,
      @Nonnull final List<String> productTypeIds);
}
