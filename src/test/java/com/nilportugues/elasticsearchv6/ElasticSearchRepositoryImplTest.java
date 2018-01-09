package com.nilportugues.elasticsearchv6;

import com.nilportugues.elasticsearchv6.repository.ElasticSearchRepositoryImpl;
import com.nilportugues.oauth.shared.domain.*;
import org.apache.http.entity.StringEntity;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

public class ElasticSearchRepositoryImplTest {

    private ElasticSearchRepositoryImpl<TestUser> repository;

    @BeforeEach
    private void createRepository() {
        repository = new ElasticSearchRepositoryImpl<>(ElasticConfig.CLIENT, TestUser.class, "test_index", "user");
    }

    @BeforeAll
    private static void createData() throws Exception {

        //CREATES AN INDEX
        try {
            HashMap<String, String> queryParams = new HashMap<>();

            //INDEX
            HashMap<String, Object> settings = new HashMap<>();
            settings.put("number_of_shards", "1");
            settings.put("number_of_replicas", "0");

            //Create the index with the mapping.
            HashMap<String, Object> jsonData = new HashMap<>();
            jsonData.put("settings", settings);

            String json = ElasticConfig.MAPPER.writeValueAsString(jsonData);

            ElasticConfig.CLIENT
                    .getLowLevelClient()
                    .performRequest("PUT", "/test_index", queryParams, new StringEntity(json), ElasticConfig.JSON_HEADER);

        } catch (Exception ignored) {

        }

        //CREATES A MAPPING
        try {
            HashMap<String, String> queryParams = new HashMap<>();

            //MAPPING
            //Have a list of mappings for each type to do a translation
            HashMap<String, String> stringType = new HashMap<>();
            stringType.put("type", "text");

            //Based on the object's properties, build the propertyList
            HashMap<String, Object> userPropertyList = new HashMap<>();
            userPropertyList.put("id", stringType);
            userPropertyList.put("username", stringType);

            //Envelope for the propertyList
            HashMap<String, Object> mappings = new HashMap<>();
            mappings.put("properties", userPropertyList);

            String json = ElasticConfig.MAPPER.writeValueAsString(mappings);

            ElasticConfig.CLIENT
                    .getLowLevelClient()
                    .performRequest("PUT", "/test_index/_mapping/user", queryParams, new StringEntity(json), ElasticConfig.JSON_HEADER);
        } catch (Exception ignored) {

        }
    }

    @Test
    public void testItCanFindOne() {
        final TestUser data = new TestUser();
        data.setId("1");
        data.setUsername("find-test");
        repository.save(data);

        Assert.assertNotNull(repository.findById("1"));
    }

    @Test
    public void testItCanSave() {
        final TestUser data = new TestUser();
        data.setId("2");
        data.setUsername("save-test");
        repository.save(data);

        Assert.assertNotNull(repository.findById("2"));
    }

    @Test
    public void testItCanDelete() {
        final TestUser data = new TestUser();
        data.setId("3");
        data.setUsername("delete-test");
        repository.save(data);
        repository.deleteById("3");

        Assert.assertNull(repository.findById("3"));
    }

    @Test
    public void testItCanFind() {
        final TestUser data = new TestUser();
        data.setId("4");
        data.setUsername("at-least-one-element");
        repository.save(data);

        List<TestUser> list = repository.find(new FilterOptions(), new SortOptions());
        Assert.assertNotNull(list);
        Assert.assertTrue(list.size() > 0);
    }

    @Test
    public void testItCanDoPaginatedResults() {
        final TestUser data = new TestUser();
        data.setId("5");
        data.setUsername("at-least-one-element");
        repository.save(data);

        final PageOptions pageOptions = new PageOptions();
        pageOptions.setSize(1);
        pageOptions.setNumber(1);

        final Paginated<TestUser> paginated = repository.findAll(new FilterOptions(), pageOptions, new SortOptions());

        Assert.assertNotNull(paginated);
        Assert.assertNotNull(paginated.getContent());
        Assert.assertTrue(paginated.getPageSize() == 1);
        Assert.assertTrue(paginated.getPageNumber() == 1);
    }

    @Test
    public void testItCanDoFacetedSearch() {
        final TestUser data = new TestUser();
        data.setId("5");
        data.setUsername("at-least-one-element");
        repository.save(data);

        final PageOptions pageOptions = new PageOptions();
        pageOptions.setSize(1);
        pageOptions.setNumber(1);

        final FacetOptions facets = new FacetOptions();
        facets.addFacetForField("Usernames", "username");
        facets.addFacetForField("User Ids", "id");

        final Faceted<TestUser> faceted = repository.search(facets, new FilterOptions(), pageOptions, new SortOptions());

        Assert.assertNotNull(faceted);
        Assert.assertNotNull(faceted.getContent());
        Assert.assertTrue(faceted.getPageSize() == 1);
        Assert.assertTrue(faceted.getPageNumber() == 1);
    }
}
