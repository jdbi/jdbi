package org.skife.jdbi.v2.sqlobject;

import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.exceptions.CallbackFailedException;
import org.skife.jdbi.v2.tweak.HandleCallback;

class WithHandleHandler implements Handler
{
    public Object invoke(HandleDing h, Object target, Object[] args)
    {
        final Handle handle = h.getHandle();
        final HandleCallback<?> callback = (HandleCallback<?>) args[0];
        try {
            return callback.withHandle(handle);
        }
        catch (Exception e) {
            throw new CallbackFailedException(e);
        }
    }
}
