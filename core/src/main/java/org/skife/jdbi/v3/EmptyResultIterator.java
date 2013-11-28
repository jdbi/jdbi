package org.skife.jdbi.v3;

import java.util.NoSuchElementException;

class EmptyResultIterator<T> implements ResultIterator<T>
{
    @Override
    public boolean hasNext()
    {
        return false;
    }

    @Override
    public T next()
    {
        throw new NoSuchElementException();
    }

    @Override
    public void close()
    {
    }
}
