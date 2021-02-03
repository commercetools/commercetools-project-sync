package com.commercetools.project.sync.util;

import static com.commercetools.sync.commons.utils.CompletableFutureUtils.collectionOfFuturesToFutureOfCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.commercetools.sync.commons.models.GraphQlBaseResource;
import com.commercetools.sync.commons.models.GraphQlBaseResult;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereRequest;
import io.sphere.sdk.queries.PagedQueryResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public class ChunkUtils {

    /**
     * Executes the given {@link List} of {@link SphereRequest}s, and collects results in a list.
     *
     * @param client {@link SphereClient} responsible for interaction with the target CTP project.
     * @param requests A list of {@link SphereRequest} implementation to allow {@link SphereClient} to
     *     execute queries on CTP.
     * @param <T> the type of the underlying model.
     * @param <Q> the type of the request model.
     * @return a list of lists where each list represents the results of passed {@link SphereRequest}.
     */
    public static <T, Q extends SphereRequest<T>> CompletableFuture<List<T>> executeChunks(
        @Nonnull final SphereClient client, @Nonnull final List<Q> requests) {

        final List<CompletableFuture<T>> futures =
            requests.stream()
                    .map(request -> client.execute(request).toCompletableFuture())
                    .collect(toList());

        return collectionOfFuturesToFutureOfCollection(futures, toList());
    }

    /**
     * Flat map the list of lists of {@link PagedQueryResult} to the list of {@link T}.
     *
     * @param pagedQueryResults query responses which contains a subset of the matching values.
     * @param <T> the type of the underlying model.
     * @return a list of {@link T}
     */
    public static <T> List<T> flattenPagedQueryResults(
        @Nonnull final List<PagedQueryResult<T>> pagedQueryResults) {

        return pagedQueryResults.stream()
                                .map(PagedQueryResult::getResults)
                                .flatMap(Collection::stream)
                                .collect(toList());
    }

    /**
     * Flat map the list of lists of {@link GraphQlBaseResult} to the set of {@link U}.
     *
     * @param graphQlBaseResults query responses which contains a subset of the matching values.
     * @param <U> the type of the resource model.
     * @param <T> the type of the generic result type.
     * @return a set of {@link U}
     */
    public static <T extends GraphQlBaseResult<U>, U extends GraphQlBaseResource>
    Set<U> flattenGraphQLBaseResults(@Nonnull final List<T> graphQlBaseResults) {

        return graphQlBaseResults.stream()
                                 .filter(result -> result != null)
                                 .map(GraphQlBaseResult::getResults)
                                 .flatMap(Collection::stream)
                                 .collect(toSet());
    }

    /**
     * Given a collection of items and a {@code chunkSize}, this method chunks the elements into
     * chunks with the {@code chunkSize} represented by a {@link List} of elements.
     *
     * @param elements the list of elements
     * @param chunkSize the size of each chunk.
     * @param <T> the type of the underlying model.
     * @return a list of lists where each list represents a chunk of elements.
     */
    public static <T> List<List<T>> chunk(
        @Nonnull final Collection<T> elements, final int chunkSize) {
        final AtomicInteger index = new AtomicInteger(0);

        return new ArrayList<>(
            elements.stream()
                    .collect(Collectors.groupingBy(x -> index.getAndIncrement() / chunkSize))
                    .values());
    }
}
