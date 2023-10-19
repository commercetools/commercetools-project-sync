// package com.commercetools.project.sync.util;
//
// import com.commercetools.api.client.ProjectApiRoot;
// import com.commercetools.api.models.common.BaseResource;
//
// import java.util.List;
// import java.util.concurrent.CompletableFuture;
// import java.util.concurrent.CompletionStage;
// import java.util.function.Consumer;
// import java.util.function.Function;
// import javax.annotation.Nonnull;
//
// public final class QueryUtils {
//  /**
//   * Applies the {@code resourceToRequestMapper} function on each page, resulting from the {@code
//   * query} executed by the {@code ctpClient}, to map each resource to a {@link SphereRequest} and
//   * then executes these requests in parallel within each page.
//   *
//   * @param ctpClient defines the CTP project to apply the query on.
//   * @param query query that should be made on the CTP project.
//   * @param resourceToRequestMapper defines a mapper function that should be applied on each
//   *     resource, in the fetched page from the query on the specified CTP project, to map it to a
//   *     {@link SphereRequest}.
//   */
//  public static <T extends BaseResource, C extends QueryDsl<T, C>>
//      CompletableFuture<Void> queryAndExecute(
//          @Nonnull final ProjectApiRoot ctpClient,
//          @Nonnull final QueryDsl<T, C> query,
//          @Nonnull final Function<T, T> resourceToRequestMapper) {
//
//    return queryAndCompose(
//        ctpClient, query, resource -> ctpClient.execute(resourceToRequestMapper.apply(resource)));
//  }
//
//  /**
//   * Applies the {@code resourceToStageMapper} function on each page, resulting from the {@code
//   * query} executed by the {@code ctpClient}, to map each resource to a {@link CompletionStage}
// and
//   * then executes these stages in parallel within each page.
//   *
//   * @param ctpClient defines the CTP project to apply the query on.
//   * @param query query that should be made on the CTP project.
//   * @param resourceToStageMapper defines a mapper function that should be applied on each
// resource,
//   *     in the fetched page from the query on the specified CTP project, to map it to a {@link
//   *     CompletionStage} which will be executed (in a blocking fashion) after every page fetch.
//   */
//  private static <T extends ResourceView, C extends QueryDsl<T, C>, S>
//      CompletableFuture<Void> queryAndCompose(
//          @Nonnull final ProjectApiRoot ctpClient,
//          @Nonnull final QueryDsl<T, C> query,
//          @Nonnull final Function<T, CompletionStage<S>> resourceToStageMapper) {
//
//    final Consumer<List<T>> pageConsumer =
//        pageElements ->
//            CompletableFuture.allOf(
//                    pageElements.stream()
//                        .map(resourceToStageMapper)
//                        .map(CompletionStage::toCompletableFuture)
//                        .toArray(CompletableFuture[]::new))
//                .join();
//
//    return com.commercetools.api.client.QueryUtils.queryAll(ctpClient, query,
// pageConsumer).toCompletableFuture();
//  }
//
//  private QueryUtils() {}
// }
