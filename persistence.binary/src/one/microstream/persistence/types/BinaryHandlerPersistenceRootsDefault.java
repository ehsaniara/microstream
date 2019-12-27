package one.microstream.persistence.types;

import static one.microstream.X.notNull;

import one.microstream.collections.EqHashTable;
import one.microstream.collections.types.XGettingTable;
import one.microstream.persistence.binary.internal.AbstractBinaryHandlerCustom;
import one.microstream.persistence.binary.types.Binary;
import one.microstream.typing.KeyValue;


public final class BinaryHandlerPersistenceRootsDefault
extends AbstractBinaryHandlerCustom<PersistenceRoots.Default>
{
	///////////////////////////////////////////////////////////////////////////
	// static methods //
	///////////////////
	
	public static BinaryHandlerPersistenceRootsDefault New(
		final PersistenceRootResolverProvider rootResolverProvider,
		final PersistenceObjectRegistry       globalRegistry
	)
	{
		return new BinaryHandlerPersistenceRootsDefault(
			notNull(rootResolverProvider),
			notNull(globalRegistry)
		);
	}

	
	
	///////////////////////////////////////////////////////////////////////////
	// instance fields //
	////////////////////

	private final PersistenceRootResolverProvider rootResolverProvider;

	/**
	 * The handler instance directly knowing the global registry might suprise at first and seem like a shortcut hack.
	 * However, when taking a closer look at the task of this handler: (globally) resolving global root instances,
	 * it becomes clear that a direct access for registering resolved global instances at the global registry is
	 * indeed part of this handler's task.
	 */
	final PersistenceObjectRegistry globalRegistry;

	// (25.12.2019 TM)FIXME: priv#94: remove if not required
//	final PersistenceObjectManager objectManager;



	///////////////////////////////////////////////////////////////////////////
	// constructors //
	/////////////////

	BinaryHandlerPersistenceRootsDefault(
		final PersistenceRootResolverProvider rootResolverProvider,
		final PersistenceObjectRegistry       globalRegistry
//		final PersistenceObjectManager        objectManager
	)
	{
		super(
			PersistenceRoots.Default.class,
			CustomFields( // instances first to easy ref-only loading in storage
				Complex("instances",
					CustomField(Object.class, "instance")
				),
				Complex("identifiers",
					chars("identifier")
				)
			)
		);
		this.rootResolverProvider = rootResolverProvider;
//		this.objectManager        = objectManager       ;
		this.globalRegistry       = globalRegistry      ;
	}



	///////////////////////////////////////////////////////////////////////////
	// methods //
	////////////

	@Override
	public final void store(
		final Binary                   bytes   ,
		final PersistenceRoots.Default instance,
		final long                     objectId,
		final PersistenceStoreHandler  handler
	)
	{
		bytes.storeRoots(this.typeId(), objectId, instance.entries(), handler);
	}

	@Override
	public final PersistenceRoots.Default create(final Binary bytes, final PersistenceLoadHandler handler)
	{
		// The identifier -> objectId root id mapping is created (and validated) from the loaded data.
		final EqHashTable<String, Long> rootIdMapping = bytes.buildRootMapping(EqHashTable.New());
		
		this.ensureRefactoredCustomRootLink(rootIdMapping, handler);
		
		/* (10.12.2019 TM)TODO: PersistenceRoots constants instance oid association
		 * This method could collect all oids per identifer in the binary data and associate all
		 * linkable constants instances with their oid at the objectRegistry very easily and elegantly, here.
		 * Then there wouldn't be unnecessarily created instances that get discarded later on in update().
		 */
		return PersistenceRoots.Default.New(
			this.rootResolverProvider.provideRootResolver(),
			rootIdMapping
		);
	}
	
	private void ensureRefactoredCustomRootLink(
		final EqHashTable<String, Long> rootIdMapping,
		final PersistenceLoadHandler    handler
	)
	{
		final Long customRootOid = rootIdMapping.get(Persistence.customRootIdentifier());
		if(customRootOid == null)
		{
			return;
		}
		
		final Object rootInstance = this.rootResolverProvider.rootReference().get();
		if(rootInstance == null)
		{
			return;
		}

		handler.requireRoot(rootInstance, customRootOid);
		
		// default root cannot be handled at creation time. See #update.
	}

	@Override
	public final void update(
		final Binary                   bytes   ,
		final PersistenceRoots.Default instance,
		final PersistenceLoadHandler   handler
	)
	{
		final PersistenceRootResolver   rootResolver  = instance.rootResolver ;
		final EqHashTable<String, Long> rootIdMapping = instance.rootIdMapping;
		
		final EqHashTable<String, PersistenceRootEntry> resolvedRootEntries = EqHashTable.New();
		this.ensureRefactoredOldRoots(rootIdMapping, resolvedRootEntries, handler);

		// Root identifiers are resolved to root entries (with potentially mapped (= different) identifiers internally)
		rootResolver.resolveRootEntries(resolvedRootEntries, rootIdMapping.keys());
		
		// The entries are resolved to a mapping of current (= potentially mapped) identifiers to root instances.
		final XGettingTable<String, Object> resolvedRoots = rootResolver.resolveRootInstances(resolvedRootEntries);
		
		// The root instance's entries are updated (replaced) with the ones resolved in here.
		instance.loadingUpdateEntries(resolvedRoots);
		
		// The resolved instances need to be registered for their objectIds. Properly mapped to consider removed ones.
		this.registerInstancesPerObjectId(resolvedRootEntries, rootIdMapping);
	}
	
	private boolean ensureRefactoredOldRoots(
		final EqHashTable<String, Long>                 rootIdMapping,
		final EqHashTable<String, PersistenceRootEntry> resolvedRoots,
		final PersistenceLoadHandler                    handler
	)
	{
		final Object root = this.rootResolverProvider.rootReference().get();
		
		
		final Long defaultRootOid = rootIdMapping.get(Persistence.defaultRootIdentifier());
		if(defaultRootOid != null)
		{
			final Object defaultRoot = handler.lookupObject(defaultRootOid);
			
			// (27.12.2019 TM)FIXME: priv#194: register existing root instance for default root's root oid
			
			// FIXME BinaryHandlerPersistenceRootsDefault#ensureRefactoredOldRoots()
			throw new one.microstream.meta.NotImplementedYetError();
//			return true;
		}
		
		final Long customRootOid = rootIdMapping.get(Persistence.customRootIdentifier());
		if(customRootOid != null)
		{
			final Object customRoot = handler.lookupObject(customRootOid);
			this.rootResolverProvider.rootReference().set(customRoot);
			// (26.12.2019 TM)FIXME: must update existing root instance in root reference. will be tricky...
			resolvedRoots.add(Persistence.customRootIdentifier(), null);
			
			// FIXME BinaryHandlerPersistenceRootsDefault#ensureRefactoredOldRoots()
			throw new one.microstream.meta.NotImplementedYetError();
//			return true;
		}
		
		// FIXME BinaryHandlerPersistenceRootsDefault#ensureRefactoredOldRoots()
		throw new one.microstream.meta.NotImplementedYetError();
//		return false;
	}

	private void registerInstancesPerObjectId(
		final XGettingTable<String, PersistenceRootEntry> resolvedRootEntries,
		final XGettingTable<String, Long>                 rootIdMapping
	)
	{
		final PersistenceObjectRegistry registry = this.globalRegistry;
//		final PersistenceObjectManager objectManager = this.objectManager;

		// lock the whole registry for the complete registration process because it might be used by other threads.
		synchronized(registry)
		{
			for(final KeyValue<String, PersistenceRootEntry> rootEntry : resolvedRootEntries)
			{
				final Object rootInstance = rootEntry.value().instance();
				
				// instances can be null when either explicitly registered to be null in the refactoring or legacy enum
				if(rootInstance != null)
				{
					// must be the original identifier, not the potentially re-mapped identifier of the entry!
					final Long rootObjectId = rootIdMapping.get(rootEntry.key());
					
					// all live instances are registered for their OID.
					registry.registerConstant(rootObjectId.longValue(), rootInstance);
				}
			}
		}
	}
	
	@Override
	public final void complete(
		final Binary                   data    ,
		final PersistenceRoots.Default instance,
		final PersistenceLoadHandler   handler
	)
	{
		// temporary id mapping is no longer required
		instance.rootIdMapping = null;
	}
		
	@Override
	public final void iterateInstanceReferences(
		final PersistenceRoots.Default instance,
		final PersistenceFunction      iterator
	)
	{
		// root identifiers are actually stored a an array of string values, not as string instances.
		for(final Object object : instance.entries().values())
		{
			iterator.apply(object);
		}
	}

	@Override
	public final boolean hasInstanceReferences()
	{
		return true;
	}

	@Override
	public final void iterateLoadableReferences(final Binary bytes, final PersistenceReferenceLoader iterator)
	{
		// the nice thing about this layout is: the references can be accessed directly as if it was a simple list
		bytes.iterateListElementReferences(0, iterator);
	}

	@Override
	public final boolean hasVaryingPersistedLengthInstances()
	{
		return true;
	}

	@Override
	public final boolean hasPersistedReferences()
	{
		return true;
	}

	@Override
	public final boolean hasPersistedVariableLength()
	{
		return true;
	}

}
