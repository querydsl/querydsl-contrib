package com.querydsl.elasticsearch;

import static java.util.Arrays.asList;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.elasticsearch.client.Requests.refreshRequest;
import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.querydsl.core.NonUniqueResultException;
import com.querydsl.core.QueryResults;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.elasticsearch.domain.QUser;
import com.querydsl.elasticsearch.domain.User;
import com.querydsl.elasticsearch.jackson.JacksonElasticsearchQueries;

public class ElasticsearchQueryTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static Client client;

    private final String indexUser = "index1";
    private final String typeUser = "user";

    private final QUser user = QUser.user;
    List<User> users = Lists.newArrayList();
    User u1, u2, u3, u4;

    public ElasticsearchQueryTest() {
    }

    @BeforeClass
    public static void beforeClass() {
        ImmutableSettings.Builder settings = ImmutableSettings.builder().put("path.data", ElasticsearchQueryTest.class.getResource("").getPath());
        Node node = NodeBuilder.nodeBuilder().local(true).settings(settings).node();
        client = node.client();

    }

    @Before
    public void before() {
        deleteType(indexUser);
        createIndex(indexUser);

        u1 = addUser("Jaakko", "Jantunen", 20);
        u2 = addUser("Jaakki", "Jantunen", 30);
        u3 = addUser("Jaana", "Aakkonen", 40);
        u4 = addUser("Jaana", "BeekkoNen", 50);

        refresh(indexUser, false);
    }

    @Test
    public void Count() {
        assertEquals(4, query().fetchCount());
    }

    @Test
    public void Count_Predicate() {
        assertEquals(2, where(user.lastName.eq("Jantunen")).fetchCount());
    }

    @Test
    public void SingleResult_Keys() {
        User u = where(user.firstName.eq("Jaakko")).fetchFirst(user.firstName);
        assertEquals("Jaakko", u.getFirstName());
        assertNull(u.getLastName());
        assertEquals(0, u.getAge());
    }

    @Test
    public void UniqueResult_Keys() {
        User u = where(user.firstName.eq("Jaakko")).fetchOne(user.firstName);
        assertEquals("Jaakko", u.getFirstName());
        assertNull(u.getLastName());
        assertEquals(0, u.getAge());
    }

    @Test(expected = NonUniqueResultException.class)
    public void UniqueResult_Keys_Non_Unique() {
        where(user.firstName.eq("Jaana")).fetchOne(user.firstName);
    }

    @Test
    public void Contains() {
        //assertQuery(user.friends.contains(u1), u3, u4, u2);
    }

    @Test
    public void Contains2() {
        //assertQuery(user.friends.contains(u4));
    }

    @Test
    public void NotContains() {
        //assertQuery(user.friends.contains(u1).not(), u1);
    }

    @Test
    public void Contains_Ignore_Case() {
        assertTrue(where(user.firstName.containsIgnoreCase("akk")).fetchCount() > 0);
    }

    @Test
    public void Contains_Ignore_Case_2() {
        assertFalse(where(user.firstName.containsIgnoreCase("xyzzz")).fetchCount() > 0);
    }

    @Test
    public void Equals_Ignore_Case() {
        assertTrue(where(user.firstName.equalsIgnoreCase("jAaKko")).fetchCount() > 0);
        assertTrue(where(user.firstName.equalsIgnoreCase("AaKk")).fetchCount() == 0);
    }

    @Test
    public void Starts_With_and_Between() {
        assertQuery(user.firstName.startsWith("Jaa").and(user.age.between(20, 30)), u2, u1);
        assertQuery(user.firstName.startsWith("Jaa").and(user.age.goe(20).and(user.age.loe(30))), u2, u1);
    }

    @Test
    public void Exists() {
        assertTrue(where(user.firstName.eq("Jaakko")).fetchCount() > 0);
        assertTrue(where(user.firstName.eq("JaakkoX")).fetchCount() == 0);
        assertTrue(where(user.id.eq(u1.getId())).fetchCount() > 0);
    }

    @Test
    public void Find_By_Id() {
        assertNotNull(where(user.id.eq(u1.getId())).fetchOne());
    }

    @Test
    public void Find_By_Ids() {
        assertQuery(user.id.in(u1.getId(), u2.getId()), u2, u1);
    }

    @Test
    public void Order() {
        List<User> users = query().orderBy(user.age.asc()).fetch();
        assertEquals(asList(u1, u2, u3, u4), users);

        users = query().orderBy(user.age.desc()).fetch();
        assertEquals(asList(u4, u3, u2, u1), users);
    }

    @Test
    public void ListResults() {
        QueryResults<User> results = query().limit(2).orderBy(user.age.asc()).fetchResults();
        assertEquals(4L, results.getTotal());
        assertEquals(2, results.getResults().size());

        results = query().offset(2).orderBy(user.age.asc()).fetchResults();
        assertEquals(4L, results.getTotal());
        assertEquals(2, results.getResults().size());
    }

    @Test
    public void EmptyResults() {
        QueryResults<User> results = query().where(user.firstName.eq("XXX")).fetchResults();
        assertEquals(0L, results.getTotal());
        assertEquals(Collections.<User>emptyList(), results.getResults());
    }

    @Test
    public void EqInAndOrderByQueries() {
        assertQuery(user.firstName.eq("Jaakko"), u1);
        assertQuery(user.firstName.equalsIgnoreCase("jaakko"), u1);
        assertQuery(user.lastName.eq("Aakkonen"), u3);

        assertQuery(user.firstName.in("Jaakko","Teppo"), u1);
        assertQuery(user.lastName.in("Aakkonen", "BeekkoNen"), u3, u4);

        assertQuery(user.firstName.eq("Jouko"));

        assertQuery(user.firstName.eq("Jaana"), user.lastName.asc(), u3, u4);
        assertQuery(user.firstName.eq("Jaana"), user.lastName.desc(), u4, u3);
        assertQuery(user.lastName.eq("Jantunen"), user.firstName.asc(), u2, u1);
        assertQuery(user.lastName.eq("Jantunen"), user.firstName.desc(), u1, u2);

        assertQuery(user.firstName.eq("Jaana").and(user.lastName.eq("Aakkonen")), u3);
        //This shoud produce 'and' also
        assertQuery(where(user.firstName.eq("Jaana"), user.lastName.eq("Aakkonen")), u3);

        assertQuery(user.firstName.ne("Jaana"), u2, u1);
        assertQuery(user.firstName.ne("Jaana").and(user.lastName.ne("Jantunen")));
        assertQuery(user.firstName.eq("Jaana").and(user.lastName.eq("Aakkonen")).not(), u4, u2, u1);

    }

    @Test
    public void Iterate() {
        User a = addUser("A", "A", 10);
        User b = addUser("A1", "B", 10);
        User c = addUser("A2", "C", 10);

        refresh(indexUser, false);

        Iterator<User> i = where(user.firstName.startsWith("A"))
                .orderBy(user.firstName.asc())
                .iterate();

        assertEquals(a, i.next());
        assertEquals(b, i.next());
        assertEquals(c, i.next());
        assertEquals(false, i.hasNext());
    }

    @Test
    public void Enum_Eq() {
        assertQuery(user.gender.eq(User.Gender.MALE), u3, u4, u2, u1);
    }

    @Test
    public void Enum_Ne() {
        assertQuery(user.gender.ne(User.Gender.MALE));
    }

    private ElasticsearchQuery<User> query() {
        return new JacksonElasticsearchQueries(client).query(User.class, indexUser, typeUser);
    }

    private ElasticsearchQuery<User> where(Predicate predicate) {
        return query().where(predicate);
    }

    private ElasticsearchQuery<User> where(Predicate ... e) {
        return query().where(e);
    }

    private void assertQuery(Predicate e, User ... expected) {
        assertQuery(where(e).orderBy(user.lastName.asc(), user.firstName.asc()), expected);
    }

    private void assertQuery(Predicate e, OrderSpecifier<?> orderBy, User ... expected) {
        assertQuery(where(e).orderBy(orderBy), expected);
    }

    private void assertQuery(ElasticsearchQuery<User> query, User ... expected) {
        List<User> results = query.fetch();

        assertNotNull(results);
        if (expected == null) {
            assertEquals("Should get empty result", 0, results.size());
            return;
        }
        assertEquals(expected.length, results.size());
        int i = 0;
        for (User u : expected) {
            assertEquals(u, results.get(i++));
        }
    }

    private User addUser(String first, String last, int age) {
        User user = new User(first, last, age, new Date());
        user.setGender(User.Gender.MALE);
        try {
            IndexResponse response = client.prepareIndex(indexUser, typeUser).setSource(mapper.writeValueAsString(user)).execute().actionGet();
            user.setId(response.getId());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        users.add(user);
        return user;
    }

    public void deleteType(String index) {
        if (indexExists(index)) {
            client.admin().indices().delete(new DeleteIndexRequest(index)).actionGet();
        }
    }

    public boolean indexExists(String index) {
        return client.admin().indices().exists(Requests.indicesExistsRequest(index)).actionGet().isExists();
    }

    public boolean createIndex(String index) {
        if (indexExists(index)) {
            return true;
        }

        CreateIndexRequestBuilder createIndexRequestBuilder = client.admin().indices().prepareCreate(index);
        return createIndexRequestBuilder.execute().actionGet().isAcknowledged();
    }

    public void refresh(String indexName, boolean waitForOperation) {
        client.admin().indices().refresh(refreshRequest(indexName).force(waitForOperation)).actionGet();
    }

}
