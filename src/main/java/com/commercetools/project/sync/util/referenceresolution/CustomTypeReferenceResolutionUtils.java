package com.commercetools.project.sync.util.referenceresolution;

import io.sphere.sdk.shoppinglists.LineItem;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.TextLineItem;
import io.sphere.sdk.types.Custom;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.CustomFieldsDraftBuilder;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Util class which provides utilities that can be used when syncing resources from a source
 * commercetools project to a target one.
 */
public final class CustomTypeReferenceResolutionUtils {
  /**
   * Given a resource of type {@code T} that extends {@link Custom} (i.e. it has {@link
   * CustomFields}, this method checks if the custom fields are existing (not null) and they are
   * reference expanded. If they are then it returns a {@link CustomFieldsDraft} instance with the
   * custom type key in place of the key of the reference. Otherwise, if it's not reference expanded
   * it returns a {@link CustomFieldsDraft} without the key. If the resource has null {@link
   * Custom}, then it returns {@code null}.
   *
   * @param resource the resource to replace its custom type key, if possible.
   * @param <T> the type of the resource.
   * @param referenceIdToKeyMap the cache contains reference Id to Keys.
   * @return an instance of {@link CustomFieldsDraft} instance with the custom type key, if the
   *     custom type reference was existing and reference cached on the resource. Otherwise, if its
   *     not reference cached it returns a {@link CustomFieldsDraft} without a key. If the resource
   *     has no or null {@link Custom}, then it returns {@code null}.
   */
  // TODO: Update javadocs after implementing the new structure with cache.
  @Nullable
  public static <T extends Custom> CustomFieldsDraft mapToCustomFieldsDraft(
      @Nonnull final T resource, @Nonnull final Map<String, String> referenceIdToKeyMap) {
    final CustomFields custom = resource.getCustom();
    return mapToCustomFieldsDraft(custom, referenceIdToKeyMap);
  }

  /**
   * Given a custom {@link CustomFields}, this method provides checking to certain resources which
   * do not extends {@link Custom}, such as {@link ShoppingList}, {@link LineItem} and {@link
   * TextLineItem}. If the custom fields are existing (not null) and they are reference cached. If
   * they are then it returns a {@link CustomFieldsDraft} instance with the custom type key in place
   * of the key of the reference. Otherwise, if it's not reference cached it returns a {@link
   * CustomFieldsDraft} without the key. If the resource has null {@link Custom}, then it returns
   * {@code null}.
   *
   * @param custom the resource to replace its custom type key, if possible.
   * @param referenceIdToKeyMap the cache contains reference Id to Keys.
   * @return an instance of {@link CustomFieldsDraft} instance with the custom type key, if the
   *     custom type reference was existing and reference is in map(cache). Otherwise, if its not
   *     reference in the map(cache) it returns a {@link CustomFieldsDraft} without a key. If the
   *     resource has no or null {@link Custom}, then it returns {@code null}.
   */
  // TODO: Update javadocs after implementing the new structure with cache.
  @Nullable
  public static CustomFieldsDraft mapToCustomFieldsDraft(
      @Nullable final CustomFields custom, @Nonnull final Map<String, String> referenceIdToKeyMap) {
    if (custom != null) {
      if (referenceIdToKeyMap.containsKey(custom.getType().getId())) {
        return CustomFieldsDraft.ofTypeKeyAndJson(
            referenceIdToKeyMap.get(custom.getType().getId()), custom.getFieldsJsonMap());
      }
      return CustomFieldsDraftBuilder.of(custom).build();
    }
    return null;
  }

  private CustomTypeReferenceResolutionUtils() {}
}
