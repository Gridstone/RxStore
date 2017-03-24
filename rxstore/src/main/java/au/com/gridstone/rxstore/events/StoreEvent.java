package au.com.gridstone.rxstore.events;

public class StoreEvent<T> {
  private final T item;
  private final Type type;

  private StoreEvent(Type type, T item) {
    this.type = type;
    this.item = item;
  }

  public enum Type {
    STORE_DELETED,
    STORE_CLEARED,
    ITEM_PUT,
  }

  public static <T> StoreEvent<T> createStoreDeletedEvent() {
    return new StoreEvent<T>(Type.STORE_DELETED, null);
  }

  public static <T> StoreEvent<T> createStoreClearedEvent() {
    return new StoreEvent<T>(Type.STORE_CLEARED, null);
  }

  public static <T> StoreEvent<T> createItemPutEvent(T item) {
    return new StoreEvent<T>(Type.ITEM_PUT, item);
  }

  /**
   * @return the item
   * @throws IllegalStateException if {{@link #getType()}} is not {@link Type#ITEM_PUT}
   */
  public T getItem() {
    if (getType() == Type.ITEM_PUT)
      return item;

    throw new IllegalStateException();
  }

  public Type getType() {
    return type;
  }
}
