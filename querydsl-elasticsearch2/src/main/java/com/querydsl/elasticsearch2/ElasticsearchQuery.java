/*
 * Copyright 2014, Mysema Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.querydsl.elasticsearch2;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnegative;
import javax.annotation.Nullable;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import com.google.common.base.Function;
import com.mysema.commons.lang.CloseableIterator;
import com.mysema.commons.lang.IteratorAdapter;
import com.querydsl.core.*;
import com.querydsl.core.support.QueryMixin;
import com.querydsl.core.types.*;

/**
 * ElasticsearchQuery provides a general Querydsl query implementation with a pluggable String to Bean transformation
 *
 * @param <K> result type
 * @author Kevin Leturc
 */
public abstract class ElasticsearchQuery<K> implements SimpleQuery<ElasticsearchQuery<K>>, Fetchable<K> {

    private final QueryMixin<ElasticsearchQuery<K>> queryMixin;

    private final Client client;

    private final Function<SearchHit, K> transformer;

    private final ElasticsearchSerializer serializer;

    public ElasticsearchQuery(Client client, Function<SearchHit, K> transformer, ElasticsearchSerializer serializer) {
        this.queryMixin = new QueryMixin<ElasticsearchQuery<K>>(this, new DefaultQueryMetadata().noValidate(), false);
        this.client = client;
        this.transformer = transformer;
        this.serializer = serializer;
    }

    @Override
    public CloseableIterator<K> iterate() {
        return new IteratorAdapter<K>(fetch().iterator());
    }

    public List<K> fetch(Path<?>... paths) {
        queryMixin.setProjection(paths);
        return fetch();
    }

    @Override
    public List<K> fetch() {
        // Test if there're limit or offset, and if not, set them to retrieve all results
        // because by default elasticsearch2 returns only 10 results
        QueryMetadata metadata = queryMixin.getMetadata();
        QueryModifiers modifiers = metadata.getModifiers();
        if (modifiers.getLimit() == null && modifiers.getOffset() == null) {
            long count = fetchCount();
            if (count > 0L) {
                // Set the limit only if there's result
                metadata.setModifiers(new QueryModifiers(count, 0L));
            }
        }

        // Execute search
        SearchResponse searchResponse = executeSearch();
        List<K> results = new ArrayList<K>();
        for (SearchHit hit : searchResponse.getHits().getHits()) {
            results.add(transformer.apply(hit));
        }
        return results;
    }

    public K fetchFirst(Path<?>... paths) {
        queryMixin.setProjection(paths);
        return fetchFirst();
    }

    @Nullable
    @Override
    public K fetchFirst() {
        // Set the size of response
        queryMixin.getMetadata().setModifiers(new QueryModifiers(1L, 0L));

        SearchResponse searchResponse = executeSearch();
        SearchHits hits = searchResponse.getHits();
        if (hits.getTotalHits() > 0) {
            return transformer.apply(hits.getAt(0));
        } else {
            return null;
        }
    }

    public K fetchOne(Path<?>... paths) {
        queryMixin.setProjection(paths);
        return fetchOne();
    }

    @Nullable
    @Override
    public K fetchOne() {
        // Set the size of response
        // Set 2 as limit because it has to be ony one result which match the condition
        queryMixin.getMetadata().setModifiers(new QueryModifiers(2L, 0L));

        SearchResponse searchResponse = executeSearch();
        SearchHits hits = searchResponse.getHits();
        long totalHits = hits.getTotalHits();
        if (totalHits == 1L) {
            return transformer.apply(hits.getAt(0));
        } else if (totalHits > 1L) {
            throw new NonUniqueResultException();
        } else {
            return null;
        }
    }

    public QueryResults<K> fetchResults(Path<?>... paths) {
        queryMixin.setProjection(paths);
        return fetchResults();
    }

    @Override
    public QueryResults<K> fetchResults() {
        long total = fetchCount();
        if (total > 0L) {
            return new QueryResults<K>(fetch(), queryMixin.getMetadata().getModifiers(), total);
        } else {
            return QueryResults.emptyResults();
        }
    }

    @Override
    public long fetchCount() {
        Predicate filter = createFilter(queryMixin.getMetadata());
        return client.prepareCount().setQuery(createQuery(filter)).execute().actionGet().getCount();
    }

    @Override
    public ElasticsearchQuery<K> limit(@Nonnegative long limit) {
        return queryMixin.limit(limit);
    }

    @Override
    public ElasticsearchQuery<K> offset(@Nonnegative long offset) {
        return queryMixin.offset(offset);
    }

    @Override
    public ElasticsearchQuery<K> restrict(QueryModifiers modifiers) {
        return queryMixin.restrict(modifiers);
    }

    @Override
    public ElasticsearchQuery<K> orderBy(OrderSpecifier<?>... o) {
        return queryMixin.orderBy(o);
    }

    @Override
    public <T> ElasticsearchQuery<K> set(ParamExpression<T> param, T value) {
        return queryMixin.set(param, value);
    }

    @Override
    public ElasticsearchQuery<K> distinct() {
        return queryMixin.distinct();
    }

    @Override
    public ElasticsearchQuery<K> where(Predicate... o) {
        return queryMixin.where(o);
    }

    @Nullable
    protected Predicate createFilter(QueryMetadata metadata) {
        return metadata.getWhere();
    }

    private QueryBuilder createQuery(@Nullable Predicate predicate) {
        if (predicate != null) {
            return (QueryBuilder) serializer.handle(predicate);
        } else {
            return QueryBuilders.matchAllQuery();
        }
    }

    private SearchResponse executeSearch() {
        QueryMetadata metadata = queryMixin.getMetadata();
        Predicate filter = createFilter(metadata);
        return executeSearch(getIndex(), getType(), filter, metadata.getProjection(), metadata.getModifiers(),
                metadata.getOrderBy());
    }

    private SearchResponse executeSearch(String index, String type, Predicate filter,
            Expression<?> projection, QueryModifiers modifiers, List<OrderSpecifier<?>> orderBys) {
        SearchRequestBuilder requestBuilder = client.prepareSearch(index).setTypes(type);

        // Set query
        requestBuilder.setQuery(createQuery(filter));

        // Add order by
        for (OrderSpecifier<?> sort : orderBys) {
            requestBuilder.addSort(serializer.toSort(sort));
        }

        // Add projections
        if (projection != null) {
            List<String> sourceFields = new ArrayList<String>();
            if (projection instanceof FactoryExpression) {
                for (Expression<?> pr : ((FactoryExpression<?>) projection).getArgs()) {
                    sourceFields.add(pr.accept(serializer, null).toString());
                }
            } else {
                sourceFields.add(projection.accept(serializer, null).toString());
            }

            requestBuilder.setFetchSource(sourceFields.toArray(new String[sourceFields.size()]), null);
        }

        // Add limit and offset
        Integer limit = modifiers.getLimitAsInteger();
        Integer offset = modifiers.getOffsetAsInteger();
        if (limit != null) {
            requestBuilder.setSize(limit);
        }
        if (offset != null) {
            requestBuilder.setFrom(offset);
        }

        return requestBuilder.execute().actionGet();
    }

    public abstract String getIndex();

    public abstract String getType();

}
