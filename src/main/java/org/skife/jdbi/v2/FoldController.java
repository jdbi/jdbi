package org.skife.jdbi.v2;

public class FoldController
{
    private boolean abort = false;

    public void abort() {
        this.abort  = true;
    }

    boolean isAborted()
    {
        return abort;
    }
}
