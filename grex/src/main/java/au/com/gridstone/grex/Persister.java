package au.com.gridstone.grex;

import java.util.List;

import rx.Observable;

/**
 * An interface representing a persistence layer.
 */
public interface Persister {
    /**
     * Write a List of objects to disk.
     *
     * @param key  The key to store the List against.
     * @param list The List to store.
     * @param type The class of each item stored in the List.
     * @return An Observable that returns the written list in its onNext().
     */
    <T> Observable<List<T>> putList(String key, List<T> list, Class<T> type);

    /**
     * Reads a List of objects from disk.
     *
     * @param key  The key that the List is stored against.
     * @param type The type of each item stored in the List.
     * @return An Observable that returns the read list in its onNext(). If no list is found, then
     * onCompleted() will be called immediately.
     */
    <T> Observable<List<T>> getList(String key, Class<T> type);

    /**
     * Adds an object to an existing List, or creates and stores a new List.
     *
     * @param key    The key that the List is stored against. (Or will be stored against if its
     *               currently empty).
     * @param object The object to add to the List.
     * @param type   The type of each item in the List.
     * @return An Observable of the new List written to disk.
     */
    <T> Observable<List<T>> addToList(String key, T object, Class<T> type);

    /**
     * Remove an object from an existing List.
     *
     * @param key    The key that the List is stored against.
     * @param object The object to remove from the List.
     * @param type   The type of each item stored in the List.
     * @return An Observable of the new List written to disk after the remove
     * operation has
     * occurred.
     */
    <T> Observable<List<T>> removeFromList(String key, T object, Class<T> type);

    /**
     * Remove an object from an existing List by its index.
     *
     * @param key      The key that the List is stored against.
     * @param position The index of the item to remove.
     * @param type     The type of each item stored in the List.
     * @return An Observable of the new List written to disk after the remove operation has
     * occurred.
     */
    <T> Observable<List<T>> removeFromList(String key, int position,
                                           Class<T> type);

    /**
     * Writes an object to disk.
     *
     * @param key    The key to store the object against.
     * @param object The object to write to disk.
     * @return An Observable of the object written to disk.
     */
    <T> Observable<T> put(String key, T object);

    /**
     * Retrieves an object from disk.
     *
     * @param key  The key that the object is stored against.
     * @param type The type of the object stored on disk.
     * @return An observable of the retrieved object.
     */
    <T> Observable<T> get(String key, Class<T> type);

    /**
     * Clears any data stored at the specified key.
     *
     * @param key The key to clear data at.
     * @return An Observable that calls onNext(true) if data was cleared and
     * then completes.
     */
    Observable<Boolean> clear(String key);
}
