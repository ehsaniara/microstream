package net.jadoth.collections.old;

import java.util.Collection;
import java.util.Iterator;

import net.jadoth.collections.types.XCollection;
import net.jadoth.collections.types.XGettingCollection;
import net.jadoth.collections.types.XSet;
import net.jadoth.functional.JadothFunctional;
import net.jadoth.util.JadothTypes;

public class BridgeXCollection<E> implements OldCollection<E>
{
	///////////////////////////////////////////////////////////////////////////
	// instance fields  //
	/////////////////////

	final XCollection<E> subject;



	///////////////////////////////////////////////////////////////////////////
	// constructors     //
	/////////////////////

	protected BridgeXCollection(final XCollection<E> collection)
	{
		super();
		this.subject = collection;
	}



	///////////////////////////////////////////////////////////////////////////
	// override methods //
	/////////////////////

	@Override
	public XCollection<E> parent()
	{
		return this.subject;
	}

	@Override
	public boolean add(final E e)
	{
		return ((XSet<E>)this.subject).add(e);
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean addAll(final Collection<? extends E> c)
	{
		if(c instanceof XGettingCollection<?>)
		{
			((XSet<E>)this.subject).addAll((XGettingCollection<? extends E>)c);
			return true;
		}

		final XSet<E> list = (XSet<E>)this.subject;
		for(final E e : c)
		{
			list.add(e);
		}
		return true;
	}

	@Override
	public void clear()
	{
		((XSet<E>)this.subject).clear();
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean contains(final Object o)
	{
		return this.subject.containsSearched(JadothFunctional.isEqualTo((E)o));
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean containsAll(final Collection<?> c)
	{
		for(final Object o : c)
		{
			if(!this.subject.containsSearched(JadothFunctional.isEqualTo((E)o)))
			{
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isEmpty()
	{
		return this.subject.isEmpty();
	}

	@Override
	public Iterator<E> iterator()
	{
		return this.subject.iterator();
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean remove(final Object o)
	{
		return ((XSet<E>)this.subject).removeBy(JadothFunctional.isEqualTo((E)o)) > 0;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean removeAll(final Collection<?> c)
	{
		int removeCount = 0;
		final XSet<E> list = (XSet<E>)this.subject;

		// even xcollections have to be handled that way because of the missing type info (argh)
		for(final Object o : c)
		{
			removeCount += list.removeBy(JadothFunctional.isEqualTo((E)o));
		}
		return removeCount > 0;
	}

	@Override
	public boolean retainAll(final Collection<?> c)
	{
		final int oldSize = JadothTypes.to_int(this.subject.size());
		((XSet<E>)this.subject).removeBy(e -> !c.contains(e));
		return oldSize - JadothTypes.to_int(this.subject.size()) > 0;
	}

	@Override
	public int size()
	{
		return JadothTypes.to_int(this.subject.size());
	}

	@Override
	public Object[] toArray()
	{
		return this.subject.toArray();
	}

}
