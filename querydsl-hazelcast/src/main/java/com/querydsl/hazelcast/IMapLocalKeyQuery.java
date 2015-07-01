package com.querydsl.hazelcast;

import java.util.Collection;

import com.hazelcast.core.IMap;
import com.hazelcast.query.Predicate;
import com.querydsl.core.SimpleQuery;

/**
 * IMapLocalKeyQuery is the implementation of the {@link SimpleQuery} for Hazelcast {@link IMap#localKeySet(Predicate)}
 *
 * @param <Q> result type
 *
 * @author velo
 */
public class IMapLocalKeyQuery<Q> extends AbstractIMapQuery<Q> {

    private IMap<Q, ?> map;

    public IMapLocalKeyQuery(IMap<Q, ?> map) {
        super();
        this.map = map;
    }

    @Override
    protected Collection<Q> query(Predicate<?, Q> query) {
        return map.localKeySet(query);
    }

}
