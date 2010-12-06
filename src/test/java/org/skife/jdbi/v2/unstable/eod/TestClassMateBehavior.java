package org.skife.jdbi.v2.unstable.eod;

import com.fasterxml.classmate.MemberResolver;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.classmate.members.ResolvedMethod;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

public class TestClassMateBehavior extends TestCase
{
    public static final TypeResolver tr = new TypeResolver();

    public static class WoofList<T, E> extends ArrayList<T>
    {
        public WoofList(Class<E> eclass) {

        }
    }

    public static interface Wiff {
        WoofList<String, Integer> goorp();
    }

    public void testFoo() throws Exception
    {
        MemberResolver mr = new MemberResolver(tr);

        ResolvedTypeWithMembers mrr = mr.resolve(tr.resolve(Wiff.class), null, null);

        ResolvedMethod[] methods = mrr.getMemberMethods();
        for (ResolvedMethod method : methods) {
            method.getReturnType().findSupertype(List.class);
        }

    }


    public static interface UnplC {
        List foo();
    }

    public void testUnparameterizedList() throws Exception
    {
        MemberResolver mr = new MemberResolver(tr);

        ResolvedTypeWithMembers mrr = mr.resolve(tr.resolve(UnplC.class), null, null);

        ResolvedMethod[] methods = mrr.getMemberMethods();
        for (ResolvedMethod method : methods) {
            System.out.println(method.getReturnType().findSupertype(List.class).getTypeParameters());
        }
    }
}
