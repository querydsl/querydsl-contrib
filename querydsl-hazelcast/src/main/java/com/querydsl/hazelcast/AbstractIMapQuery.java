package com.querydsl.hazelcast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import com.hazelcast.query.Predicate;
import com.hazelcast.query.PredicateBuilder;
import com.mysema.commons.lang.CloseableIterator;
import com.querydsl.core.*;
import com.querydsl.core.support.QueryMixin;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.ParamExpression;
import com.querydsl.core.types.Path;
import com.querydsl.hazelcast.impl.HazelcastSerializer;

/**
 *
 * {@code AbstractIMapQuery} is the base class for Hazelcast queries
 *
 * @param <Q> result type
 */
public abstract class AbstractIMapQuery<Q> implements SimpleQuery<AbstractIMapQuery<Q>>, Fetchable<Q> {

    protected final HazelcastSerializer serializer;

    protected final QueryMixin<AbstractIMapQuery<Q>> queryMixin;

    public AbstractIMapQuery() {
        super();
        this.queryMixin = new QueryMixin<AbstractIMapQuery<Q>>(this,
                new DefaultQueryMetadata().noValidate());
        this.serializer = HazelcastSerializer.DEFAULT;
    }

    @Override
    public AbstractIMapQuery<Q> where(com.querydsl.core.types.Predicate... e) {
        return queryMixin.where(e);
    }

    @Override
    public CloseableIterator<Q> iterate() {
        final Iterator<? extends Q> iterator = query().iterator();
        return new CloseableIterator<Q>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Q next() {
                return iterator.next();
            }

            @Override
            public void remove() {
                iterator.remove();
            }

            @Override
            public void close() {
            }
        };
    }

    private Collection<Q> query() {
        Predicate<?, Q> query = createQuery(queryMixin.getMetadata());
        return query(query);
    }

    protected abstract Collection<Q> query(Predicate<?, Q> query);

    public List<Q> fetch(Path<?>... paths) {
        queryMixin.setProjection(paths);
        return fetch();
    }

    @Override
    public List<Q> fetch() {
        return new ArrayList<Q>(query());
    }

    @SuppressWarnings("unchecked")
    protected Predicate<?, Q> createQuery(@Nullable QueryMetadata queryMetadata) {
        if (queryMetadata.getWhere() != null) {
            return serializer.handle(queryMetadata.getWhere());
        } else {
            return new PredicateBuilder();
        }
    }

    @Override
    public Q fetchFirst() {
        return limit(1).fetchOne();
    }

    @Override
    public Q fetchOne() {
        List<Q> result = fetch();
        if (result.size() == 0) {
            return null;
        }

        if (result.size() != 1) {
            throw new NonUniqueResultException();
        }

        return result.get(0);
    }

    @Override
    public QueryResults<Q> fetchResults() {
        long total = fetchCount();
        if (total > 0L) {
            return new QueryResults<Q>(fetch(), queryMixin.getMetadata().getModifiers(), total);
        } else {
            return QueryResults.emptyResults();
        }
    }

    @Override
    public long fetchCount() {
        return query().size();
    }

    @Override
    public AbstractIMapQuery<Q> limit(long limit) {
        return queryMixin.limit(limit);
    }

    @Override
    public AbstractIMapQuery<Q> offset(long offset) {
        return queryMixin.offset(offset);
    }

    @Override
    public AbstractIMapQuery<Q> restrict(QueryModifiers modifiers) {
        return queryMixin.restrict(modifiers);
    }

    @Override
    public AbstractIMapQuery<Q> orderBy(OrderSpecifier<?>... o) {
        return queryMixin.orderBy(o);
    }

    @Override
    public <T> AbstractIMapQuery<Q> set(ParamExpression<T> param, T value) {
        return queryMixin.set(param, value);
    }

    @Override
    public AbstractIMapQuery<Q> distinct() {
        return queryMixin.distinct();
    }

}