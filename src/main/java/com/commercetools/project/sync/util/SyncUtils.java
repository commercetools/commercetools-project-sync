package com.commercetools.project.sync.util;

import com.commercetools.sync.commons.BaseSync;

import javax.annotation.Nonnull;

public final class SyncUtils {


    @Nonnull
    public static String getSyncModuleName(@Nonnull final Class<? extends BaseSync> syncClass) {
        return syncClass.getSimpleName();
    }

    private SyncUtils() {
    }
}
