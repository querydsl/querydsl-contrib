package com.querydsl.dynamodb;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.mysema.commons.lang.CloseableIterator;
import com.querydsl.core.*;
import com.querydsl.core.support.QueryMixin;
import com.querydsl.core.types.*;
import com.querydsl.dynamodb.impl.DynamodbSerializer;

/**
 * DynamoDBQuery is the implementation of the {@link SimpleQuery} for DynamoDB
 *
 * @param <Q> result type
 * @author velo
 */
public class DynamoDBQuery<Q> implements SimpleQuery<DynamoDBQuery<Q>>, Fetchable<Q> {

    private AmazonDynamoDB client;
    private DynamoDBMapper mapper;
    private final DynamodbSerializer serializer;
    private final QueryMixin<DynamoDBQuery<Q>> queryMixin;
    private EntityPath<Q> entityPath;

    public DynamoDBQuery(AmazonDynamoDB client, EntityPath<Q> entityPath) {
        this.queryMixin = new QueryMixin<DynamoDBQuery<Q>>(this,
                new DefaultQueryMetadata().noValidate());
        this.client = client;
        this.mapper = new DynamoDBMapper(this.client);
        this.serializer = DynamodbSerializer.DEFAULT;
        this.entityPath = entityPath;
    }

    @Override
    public DynamoDBQuery<Q> where(Predicate... e) {
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

    public List<Q> fetch(Path<?>... paths) {
        queryMixin.setProjection(paths);
        return fetch();
    }

    @Override
    public List<Q> fetch() {
        PaginatedScanList<? extends Q> result = query();
        return cast(result);
    }

    private PaginatedScanList<? extends Q> query() {
        DynamoDBScanExpression query = createQuery(queryMixin.getMetadata());
        PaginatedScanList<? extends Q> result = mapper.scan(entityPath.getType(), query);
        return result;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private List<Q> cast(PaginatedScanList result) {
        return new ArrayList<Q>(result);
    }

    private DynamoDBScanExpression createQuery(@Nullable QueryMetadata queryMetadata) {
        if (queryMetadata.getWhere() != null) {
            return serializer.handle(queryMetadata.getWhere());
        } else {
            return new DynamoDBScanExpression();
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
        DynamoDBScanExpression query = createQuery(queryMixin.getMetadata());
        return mapper.count(entityPath.getType(), query);
    }

    @Override
    public DynamoDBQuery<Q> limit(long limit) {
        return queryMixin.limit(limit);
    }

    @Override
    public DynamoDBQuery<Q> offset(long offset) {
        return queryMixin.offset(offset);
    }

    @Override
    public DynamoDBQuery<Q> restrict(QueryModifiers modifiers) {
        return queryMixin.restrict(modifiers);
    }

    @Override
    public DynamoDBQuery<Q> orderBy(OrderSpecifier<?>... o) {
        return queryMixin.orderBy(o);
    }

    @Override
    public <T> DynamoDBQuery<Q> set(ParamExpression<T> param, T value) {
        return queryMixin.set(param, value);
    }

    @Override
    public DynamoDBQuery<Q> distinct() {
        return queryMixin.distinct();
    }

}
