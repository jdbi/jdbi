package org.skife.jdbi.v2.sqlobject;


/**
 * @deprecated use {@link org.skife.jdbi.v2.sqlobject.mixins.Transactional}
 */
@Deprecated
public interface Transactional<SelfType extends Transactional<SelfType>> extends org.skife.jdbi.v2.sqlobject.mixins.Transactional<SelfType>
{
}
