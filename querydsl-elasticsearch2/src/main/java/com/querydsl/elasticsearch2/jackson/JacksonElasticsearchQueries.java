package com.querydsl.elasticsearch2.jackson;

import java.io.IOException;
import java.lang.reflect.Field;

import javax.annotation.Nullable;

import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.querydsl.elasticsearch2.ElasticsearchQuery;
import com.querydsl.elasticsearch2.ElasticsearchSerializer;

/**
 * JacksonElasticsearchQueries is a factory to provide ElasticsearchQuery basic implementation.
 *
 * @author Kevin Leturc
 */
public class JacksonElasticsearchQueries {

    private final Client client;

    /**
     * Default constructor.
     *
     * @param client The elasticsearch client.
     */
    public JacksonElasticsearchQueries(Client client) {
        this.client = client;
    }

    public <K> ElasticsearchQuery<K> query(Class<K> entityClass, String index, String type) {
        return query(entityClass, index, type, new ElasticsearchSerializer());
    }

    public <K> ElasticsearchQuery<K> query(Class<K> entityClass, String index, String type, ElasticsearchSerializer serializer) {
        return query(index, type, serializer, defaultTransformer(entityClass));
    }

    public <K> ElasticsearchQuery<K> query(String index, String type, Function<SearchHit, K> transformer) {
        return query(index, type, new ElasticsearchSerializer(), transformer);
    }

    public <K> ElasticsearchQuery<K> query(final String index, final String type, ElasticsearchSerializer serializer, Function<SearchHit, K> transformer) {
        return new ElasticsearchQuery<K>(client, transformer, serializer) {

            /**
             * {@inheritDoc}
             */
            @Override
            public String getIndex() {
                return index;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String getType() {
                return type;
            }

        };
    }

    /**
     * Returns the default transformer.
     *
     * @param entityClass The entity class.
     * @param <K> The entity type.
     * @return The default transformer.
     */
    private <K> Function<SearchHit, K> defaultTransformer(final Class<K> entityClass) {
        final ObjectMapper mapper = new ObjectMapper();
        return new Function<SearchHit, K>() {

            /**
             * {@inheritDoc}
             */
            @Nullable
            @Override
            public K apply(@Nullable SearchHit input) {
                try {
                    K bean = mapper.readValue(input.getSourceAsString(), entityClass);

                    Field idField = null;
                    Class<?> target = entityClass;
                    while (idField == null && target != Object.class) {
                        for (Field field : target.getDeclaredFields()) {
                            if ("id".equals(field.getName())) {
                                idField = field;
                            }
                        }
                        target = target.getSuperclass();
                    }
                    if (idField != null) {
                        idField.setAccessible(true);
                        idField.set(bean, input.getId());
                    }

                    return bean;
                } catch (SecurityException se) {
                    throw new MappingException("Unable to lookup id field, may be use a custom transformer ?", se);
                } catch (IllegalAccessException e) {
                    throw new MappingException("Unable to set id value in id field, may be use a custom transformer ?", e);
                } catch (IOException e) {
                    throw new MappingException("Unable to read the Elasticsearch response.", e);
                }
            }
        };
    }
}
