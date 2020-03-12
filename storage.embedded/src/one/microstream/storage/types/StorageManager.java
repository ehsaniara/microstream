package one.microstream.storage.types;

import one.microstream.persistence.types.PersistenceObjectRegistry;
import one.microstream.persistence.types.PersistenceRootsView;
import one.microstream.persistence.types.Storer;
import one.microstream.reference.Reference;


/**
 * Central managing type of a native Java database's storage layer.
 * <p>
 * For all intents and purposes, a {@link StorageManager} instance represents the storage of a database in the
 * Java application that uses it. It is used for starting and stopping storage managements threads,
 * call storage-level utility functionality like clearing the low-level data cache, cleaning up / condensing
 * storage files or calling the storage-level garbage collector to remove data that has become unreachable in the
 * entity graph. This type also allows querying the used {@link StorageConfiguration} or the
 * {@link StorageTypeDictionary} that defines the persistent structure of all handled types.
 * <p>
 * For the most cases, only the methods {@link #root()}, {@link #setRoot(Object)}, {@link #start()} and
 * {@link #shutdown()} are important. Everything else is used for moreless advanced purposes and should only be used
 * with good knowledge about the effects caused by it.
 * <p>
 * A {@link StorageManager} instance is also implicitely a {@link StorageConnection}, so that developers don't
 * need to care about connections at all if a single connection suffices.
 * 
 * @author TM
 *
 */
public interface StorageManager extends StorageController, StorageConnection, DatabasePart
{
	/**
	 * Returns the {@link StorageConfiguration} used to initialize this {@link StorageManager} instance.
	 * 
	 * @return the used configuration.
	 */
	public StorageConfiguration configuration();
	
	public StorageTypeDictionary typeDictionary();

	public void initialize();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public StorageManager start();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean shutdown();

	public StorageConnection createConnection();
	
	/**
	 * Return the persistent object graph's root object, without specific typing.
	 * <p>
	 * If a specifically typed root instance reference is desired, it is preferable to hold a properly typed constant
	 * reference to it and let the storage initialization use that instance as the root.<br>
	 * See the following code snippet on how to do that:
	 * <pre>{@code
	 *static final MyAppRoot      ROOT    = new MyAppRoot();
	 *static final StorageManager STORAGE = EmbeddedStorage.start(ROOT);
	 * }</pre>
	 * 
	 * @return the persistent object graph's root object.
	 */
	public Object root();
	
	/**
	 * Sets the passed instance as the new root for the persistent object graph.<br>
	 * Note that this will replace the old root instance, potentially resulting in wiping the whole database.
	 * 
	 * @param newRoot the new root instance to be set.
	 * 
	 * @return the passed {@literal newRoot} to allow fluent usage of this method.
	 */
	public Object setRoot(Object newRoot);
	
	/**
	 * Stores the registered root instance (as returned by {@link #root()}) by using the default storing logic
	 * by calling {@link #createStorer()} to create the {@link Storer} to be used.<br>
	 * Depending on the storer logic, storing the root instance can cause many other object to be stored, as well.
	 * For example for the default behavior (as implemented in {@link #createLazyStorer()}, all recursively referenced
	 * instances that are not yet known to the persistent context (i.e. have an associated objectId registered in the
	 * context's {@link PersistenceObjectRegistry}) are stored as well.
	 * 
	 * @return the root instance's objectId.
	 */
	public long storeRoot();
	
	public PersistenceRootsView viewRoots();
	
	/**
	 * This method is deprecated due to simplified root handling and will be removed in a future version.<br>
	 * It is advised to use {@link #root()} and {@link #setRoot(Object)} instead.
	 * 
	 * @deprecated
	 * 
	 * @return a mutable {@link Reference} to the root object.
	 */
	@Deprecated
	public Reference<Object> defaultRoot();

	/**
	 * This method is deprecated due to simplified root handling and will be removed in a future version.<br>
	 * It is advised to use {@link #root()} instead, for which this method is an alias.
	 * 
	 * @deprecated
	 * 
	 * @return the root object.
	 */
	@Deprecated
	public default Object customRoot()
	{
		return this.root();
	}
	
	/**
	 * This method is deprecated due to simplified root handling and will be removed in a future version.<br>
	 * It is advised to use {@link #storeRoot()} instead, for which this method is an alias.
	 * 
	 * @deprecated
	 * 
	 * @return stores the root object and returns its objectId.
	 */
	@Deprecated
	public default long storeDefaultRoot()
	{
		return this.storeRoot();
	}
	
	public Database database();
	
	@Override
	public default String databaseName()
	{
		return this.database().databaseName();
	}

}
