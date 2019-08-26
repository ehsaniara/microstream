package one.microstream.persistence.types;

import one.microstream.collections.BulkList;
import one.microstream.reflect.XReflect;
import one.microstream.util.similarity.Similarity;

public interface PersistenceLegacyTypeHandlerCreator<M>
{
	public <T> PersistenceLegacyTypeHandler<M, T> createLegacyTypeHandler(
		PersistenceLegacyTypeMappingResult<M, T> mappingResult
	);
	
	
	
	
	
	public abstract class Abstract<M> implements PersistenceLegacyTypeHandlerCreator<M>
	{
		@Override
		public <T> PersistenceLegacyTypeHandler<M, T> createLegacyTypeHandler(
			final PersistenceLegacyTypeMappingResult<M, T> result
		)
		{
			if(PersistenceLegacyTypeMappingResult.isUnchangedInstanceStructure(result))
			{
				/*
				 * special case: structure didn't change, only namings, so the current type handler can be used.
				 * Note that this applies to custom handlers, too. Even ones with variable length instances.
				 */
				return this.createTypeHandlerUnchangedInstanceStructure(result);
			}
			
			if(result.currentTypeHandler() instanceof PersistenceTypeHandlerReflective<?, ?>)
			{
				return this.deriveReflectiveHandler(
					result,
					(PersistenceTypeHandlerReflective<M, T>)result.currentTypeHandler()
				);
			}

			return this.deriveCustomWrappingHandler(result);
		}
		
		protected <T> PersistenceLegacyTypeHandler<M, T> createTypeHandlerUnchangedInstanceStructure(
			final PersistenceLegacyTypeMappingResult<M, T> result
		)
		{
			if(XReflect.isEnum(result.currentTypeHandler().type()))
			{
				return this.createTypeHandlerUnchangedInstanceStructureGenericEnum(result);
			}

			return this.createTypeHandlerUnchangedInstanceStructureGenericType(result);
		}
		
		protected <T> PersistenceLegacyTypeHandler<M, T> createTypeHandlerUnchangedInstanceStructureGenericEnum(
			final PersistenceLegacyTypeMappingResult<M, T> result
		)
		{
			if(PersistenceLegacyTypeMappingResult.isUnchangedFullStructure(result))
			{
				// current enum type handler is generically wrapped
				return PersistenceLegacyTypeHandler.Wrap(
					result.legacyTypeDefinition(),
					result.currentTypeHandler()
				);
			}
			
			final PersistenceTypeDefinition legacyTypeDef = result.legacyTypeDefinition();
			final BulkList<PersistenceTypeDefinitionMember> legacyConstantMembers = legacyTypeDef.allMembers()
				.filterTo(BulkList.New(), PersistenceTypeDefinitionMember::isEnumConstant)
			;
			
			final PersistenceTypeDefinition currentTypeDef = result.legacyTypeDefinition();
			final BulkList<PersistenceTypeDefinitionMember> currentConstantMembers = currentTypeDef.allMembers()
				.filterTo(BulkList.New(), PersistenceTypeDefinitionMember::isEnumConstant)
			;
			
			final Integer[] ordinalMap = new Integer[legacyConstantMembers.intSize()];
			
			int ordinal = 0;
			for(final PersistenceTypeDefinitionMember legacyMember : legacyConstantMembers)
			{
				final Similarity<PersistenceTypeDefinitionMember> match =
					result.legacyToCurrentMembers().get(legacyMember)
				;
				if(match == null)
				{
					if(result.discardedLegacyMembers().contains(legacyMember))
					{
						ordinalMap[ordinal] = null;
					}
					else
					{
						// (26.08.2019 TM)EXCP: proper exception
						throw new RuntimeException(
							"Unmapped legacy enum constant: " + legacyTypeDef + "#" + legacyMember.name()
						);
					}
				}
				else
				{
					final PersistenceTypeDefinitionMember targetCurrentConstant = match.targetElement();
					final long targetOrdinal = currentConstantMembers.indexOf(targetCurrentConstant);
					if(targetOrdinal >= 0)
					{
						ordinalMap[ordinal] = Integer.valueOf((int)targetOrdinal);
					}
					else
					{
						// (26.08.2019 TM)EXCP: proper exception
						throw new RuntimeException(
							"Inconsistent target enum constant: " + currentTypeDef + "#" + targetCurrentConstant.name()
						);
					}
				}
				ordinal++;
			}
			
			// (23.08.2019 TM)FIXME: priv#23: legacyEnum handler with identical structure but ordinalMap mapping.
			throw new one.microstream.meta.NotImplementedYetError();
		}
		
		protected <T> PersistenceLegacyTypeHandler<M, T> createTypeHandlerUnchangedInstanceStructureGenericType(
			final PersistenceLegacyTypeMappingResult<M, T> result
		)
		{
			return PersistenceLegacyTypeHandler.Wrap(
				result.legacyTypeDefinition(),
				result.currentTypeHandler()
			);
		}
							
		protected abstract <T> PersistenceLegacyTypeHandler<M, T> deriveCustomWrappingHandler(
			PersistenceLegacyTypeMappingResult<M, T> mappingResult
		);
		
		protected abstract <T> PersistenceLegacyTypeHandler<M, T> deriveReflectiveHandler(
			PersistenceLegacyTypeMappingResult<M, T> mappingResult,
			PersistenceTypeHandlerReflective<M, T>   typeHandler
		);
	}
	
}
