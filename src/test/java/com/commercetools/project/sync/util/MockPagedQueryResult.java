package com.commercetools.project.sync.util;

import io.sphere.sdk.queries.PagedQueryResult;
import java.util.List;
import javax.annotation.Nonnull;

public final class MockPagedQueryResult<T> implements PagedQueryResult<T> {

  private final List<T> results;

  private MockPagedQueryResult(@Nonnull final List<T> results) {
    this.results = results;
  }

  public static <R> MockPagedQueryResult<R> of(@Nonnull final List<R> results) {
    return new MockPagedQueryResult<>(results);
  }

  @Override
  public Long getCount() {
    return (long) results.size();
  }

  @Override
  public Long getOffset() {
    return null;
  }

  @Override
  public Long getLimit() {
    return null;
  }

  @Deprecated
  @Override
  public Long size() {
    return (long) results.size();
  }

  @Override
  public List<T> getResults() {
    return results;
  }

  @Override
  public Long getTotal() {
    return (long) results.size();
  }
}
