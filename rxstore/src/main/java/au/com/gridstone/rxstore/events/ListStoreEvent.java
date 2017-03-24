package au.com.gridstone.rxstore.events;

import java.util.ArrayList;
import java.util.List;

public class ListStoreEvent<T> {
  private final List<T> list;
  private final Type type;

  private ListStoreEvent(Type type, List<T> list) {
    this.type = type;
    if (list != null)
      this.list = new ArrayList<T>(list);
    else
      this.list = null;
  }

  public enum Type {
    STORE_DELETED,
    STORE_CLEARED,
    LIST_PUT,
    ITEM_ADDED,
    ITEM_REMOVED,
    ITEM_REPLACED
  }

  public static <T> ListStoreEvent<T> createStoreDeletedEvent() {
    return new ListStoreEvent<T>(Type.STORE_DELETED, null);
  }

  public static <T> ListStoreEvent<T> createStoreClearedEvent() {
    return new ListStoreEvent<T>(Type.STORE_CLEARED, null);
  }

  public static <T> ListStoreEvent<T> createListPutEvent(List<T> list) {
    return new ListStoreEvent<T>(Type.LIST_PUT, list);
  }

  public static <T> ListStoreEvent<T> createItemAddedEvent(List<T> list) {
    return new ListStoreEvent<T>(Type.ITEM_ADDED, list);
  }

  public static <T> ListStoreEvent<T> createItemRemovedEvent(List<T> list) {
    return new ListStoreEvent<T>(Type.ITEM_REMOVED, list);
  }

  public static <T> ListStoreEvent createItemReplacedEvent(List<T> list) {
    return new ListStoreEvent<T>(Type.ITEM_REPLACED, list);
  }

  /**
   * @return the new list
   * @throws IllegalStateException if {{@link #getType()}} is not {@link Type#LIST_PUT}, {@link Type#ITEM_ADDED},
   *                               {@link Type#ITEM_REMOVED} or {@link Type#ITEM_REPLACED}
   */
  public List<T> getList() {
    if (isGetListAllowed())
      return list;

    throw new IllegalStateException();
  }

  private boolean isGetListAllowed() {
    return type == Type.LIST_PUT || type == Type.ITEM_ADDED || type == Type.ITEM_REMOVED || type == Type.ITEM_REPLACED;
  }

  public Type getType() {
    return type;
  }
}
