package com.commercetools.sync.utils;

import com.commercetools.sync.commons.helpers.BaseSyncStatistics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.annotation.Nonnull;
import org.slf4j.Logger;

public class StatisticsUtils {

    /**
     * Builds a JSON String that represents the fields of the supplied instance of {@link
     * BaseSyncStatistics} and then logs the string with the supplied {@code logger} instance.
     *
     * @param statistics the instance of {@link BaseSyncStatistics} from which to create a JSON
     *     String.
     * @param logger the logger
     */
    public static void logStatistics(
            @Nonnull final BaseSyncStatistics statistics, @Nonnull final Logger logger) {
        try {
            final String statisticsAsJSONString = getStatisticsAsJSONString(statistics);
            logger.info(statisticsAsJSONString);
        } catch (final JsonProcessingException exception) {
            logger.error("Invalid statistics JSON string..", exception);
        }
    }

    /**
     * Builds a JSON String that represents the fields of the supplied instance of {@link
     * BaseSyncStatistics}.
     *
     * @param statistics the instance of {@link BaseSyncStatistics} from which to create a JSON
     *     String.
     * @return a JSON String representation of the statistics object.
     */
    static String getStatisticsAsJSONString(@Nonnull final BaseSyncStatistics statistics)
            throws JsonProcessingException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(statistics);
    }
}
