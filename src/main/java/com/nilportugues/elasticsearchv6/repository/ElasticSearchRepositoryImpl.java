package com.nilportugues.elasticsearchv6.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nilportugues.elasticsearch.annotations.IdAnnotationProcessor;
import com.nilportugues.elasticsearch.repository.ElasticSearchRepository;
import com.nilportugues.oauth.shared.domain.*;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.ExistsQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.filter.Filters;
import org.elasticsearch.search.aggregations.bucket.filter.FiltersAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.filter.FiltersAggregator;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class ElasticSearchRepositoryImpl<T extends Serializable> implements ElasticSearchRepository<T> {

    private static final Logger LOG = LoggerFactory.getLogger(ElasticSearchRepositoryImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final BasicHeader JSON_HEADER = new BasicHeader("Content-type", "application/json");
    private static final ElasticSearchFilterAdapter FILTER_ADAPTER = new ElasticSearchFilterAdapter();
    private final IdAnnotationProcessor<T, String> ID_PROCESSOR = new IdAnnotationProcessor<>();

    private final RestHighLevelClient client;
    private final String indexName;
    private final String typeName;
    private Class<T> hydrationClass;

    public ElasticSearchRepositoryImpl(
        final RestHighLevelClient client,
        final Class<T> clazz,
        final String indexName,
        final String typeName) {

        this.client = client;
        this.indexName = indexName;
        this.typeName = typeName;
        this.hydrationClass = clazz;
    }

    @Override
    public T findById(String id) {
        try {
            final GetResponse response = client.get(new GetRequest(indexName, typeName, id), JSON_HEADER);
            final String jsonObject = response.getSourceAsString();
            return hydrateOne(hydrationClass, jsonObject);
        } catch (IOException ignored) {
            LOG.error(ignored.getMessage());
        }
        return null;
    }

    @Override
    public void deleteById(String id) {
        try {
            client.delete(new DeleteRequest(indexName, typeName, id), JSON_HEADER);
        } catch (IOException ignored) {
            LOG.error(ignored.getMessage());
        }
    }

    @Override
    public T save(T data) {
        try {
            final String id = String.valueOf(ID_PROCESSOR.findIdValue(data));

            final UpdateRequest request = new UpdateRequest();
            request.index(indexName);
            request.type(typeName);
            request.id(id);
            request.doc(MAPPER.writeValueAsString(data), XContentType.JSON);
            request.docAsUpsert(true);

            client.update(request, JSON_HEADER);
            return data;

        } catch (Exception ignored) {
            ignored.printStackTrace();
            LOG.error(ignored.getMessage());
            return null;
        }
    }

    @Override
    public List<T> find(final FilterOptions filterOptions, final SortOptions sortOptions) {
        try {
            final SearchRequest request = new SearchRequest(indexName);

            final SearchSourceBuilder searchSourceBuilder = request.source();
            buildSortOptions(sortOptions, searchSourceBuilder);
            buildFilters(filterOptions, new FacetOptions(), searchSourceBuilder);

            final SearchResponse searchResponse = client.search(request, JSON_HEADER);
            return hydrateList(hydrationClass, searchResponse);
        } catch (Exception ignored) {
            LOG.error(ignored.getMessage());
        }

        return null;
    }

    @Override
    public Paginated<T> findAll(final FilterOptions filterOptions,
                                final PageOptions pageOptions,
                                final SortOptions sortOptions) {

        try {
            final SearchRequest request = new SearchRequest(indexName);

            final SearchSourceBuilder searchSourceBuilder = request.source();
            buildPagination(pageOptions, searchSourceBuilder);
            buildSortOptions(sortOptions, searchSourceBuilder);
            buildFilters(filterOptions, new FacetOptions(), searchSourceBuilder);

            final SearchResponse searchResponse = client.search(request, JSON_HEADER);

            long totalElements = searchResponse.getHits().getTotalHits();
            int totalPages = Math.toIntExact(totalElements / pageOptions.getSize());
            if (0 == totalPages && totalElements > 0) {
                totalPages = 1;
            }

            return new Paginated<>(
                hydrateList(hydrationClass, searchResponse),
                totalPages,
                totalElements,
                pageOptions.getNumber(),
                pageOptions.getSize());

        } catch (Exception ignored) {
            LOG.error(ignored.getMessage());
        }
        return null;
    }

    @Override
    public Faceted<T> search(final FacetOptions facetOptions,
        final FilterOptions filterOptions,
        final PageOptions pageOptions,
        final SortOptions sortOptions) {

        try {
            final SearchRequest request = new SearchRequest(indexName);
            final SearchSourceBuilder searchSourceBuilder = request.source();

            buildPagination(pageOptions, searchSourceBuilder);
            buildSortOptions(sortOptions, searchSourceBuilder);
            buildFacets(facetOptions, searchSourceBuilder);
            buildFilters(filterOptions, facetOptions, searchSourceBuilder);

            final SearchResponse response = client.search(request, JSON_HEADER);

            final HashMap<String, HashMap<String, Object>> finalFacets = buildFacetTotals(response, facetOptions);
            long totalElements = response.getHits().getTotalHits();

            int totalPages = Math.toIntExact(totalElements / pageOptions.getSize());
            if (0 == totalPages && totalElements > 0) {
                totalPages = 1;
            }

            return new Faceted<>(
                hydrateList(hydrationClass, response),
                totalPages,
                totalElements,
                pageOptions.getNumber(),
                pageOptions.getSize(),
                finalFacets,
                sortOptions,
                filterOptions,
                facetOptions);

        } catch (Exception ignored) {
            ignored.printStackTrace();
            LOG.error(ignored.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<T> hydrateList(final Class hydrationClass, final SearchResponse searchResponse) {

        final ArrayList<SearchHit> list = new ArrayList<>();
        Collections.addAll(list, searchResponse.getHits().getHits());

        return list.stream()
            .map(SearchHit::getSourceAsString)
            .map(json -> (T) hydrateOne(hydrationClass, json))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private T hydrateOne(final Class<T> hydrationClass, final String json) {

        try {
            return MAPPER.readValue(json, hydrationClass);
        } catch (Exception e) {
            LOG.error(e.getMessage());
            return null;
        }
    }

    private HashMap<String, HashMap<String, Object>> buildFacetTotals(final SearchResponse searchResponse,
        final FacetOptions facetOptions) {

        final HashMap<String, HashMap<String, Object>> totalFacets = new HashMap<>();
        final Aggregations aggregations = searchResponse.getAggregations();

        if (null == aggregations) {
            return totalFacets;
        }

        facetOptions.getFacetKeys().forEach(facetName -> {

            final Filters buckets = aggregations.get(facetName);

            buckets.getBuckets().forEach(b -> {
                String key = String.valueOf(b.getKey());
                long value = b.getDocCount();

                totalFacets.putIfAbsent(key, new HashMap<>());
                totalFacets.get(key).put(facetName, value);
            });
        });

        return totalFacets;
    }

    private SearchSourceBuilder buildFacets(final FacetOptions facetOptions,
        final SearchSourceBuilder requestBuilder) {

        facetOptions.getFacetsFromField().forEach((facetName, fieldName) -> {

            final ExistsQueryBuilder queryBuilder = new ExistsQueryBuilder(fieldName);
            FiltersAggregator.KeyedFilter keyedFilter = new FiltersAggregator.KeyedFilter(String.valueOf(fieldName), queryBuilder);

            final FiltersAggregationBuilder filterAggregation = AggregationBuilders.filters(facetName, keyedFilter);
            requestBuilder.aggregation(filterAggregation);
        });


        facetOptions.getFacetsFromFilteredValues().forEach((facetName, value) -> {

            final List<FiltersAggregator.KeyedFilter> list = new ArrayList<>();
            value.forEach((fieldName, pair) -> {
                final FilterOptions filterOptions = pair.getValue();
                final BoolQueryBuilder queryBuilder = FILTER_ADAPTER.toQueryBuilder(filterOptions, null);

                list.add(new FiltersAggregator.KeyedFilter(String.valueOf(fieldName), queryBuilder));
            });

            final FiltersAggregator.KeyedFilter[] keyedFilters = list.toArray(new FiltersAggregator.KeyedFilter[list.size()]);
            final FiltersAggregationBuilder filterAggregation = AggregationBuilders.filters(facetName, keyedFilters);

            requestBuilder.aggregation(filterAggregation);
        });

        return requestBuilder;
    }

    private SearchSourceBuilder buildSortOptions(final SortOptions sortOptions,
        final SearchSourceBuilder requestBuilder) {

        if (Optional.ofNullable(sortOptions).isPresent()) {
            for (Map.Entry<String, String> name : sortOptions.getSortOrder().entrySet()) {
                requestBuilder.sort(
                    name.getKey(),
                    (name.getValue().equalsIgnoreCase(SortOptions.ASC)) ? SortOrder.ASC : SortOrder.DESC);
            }
        }
        return requestBuilder;
    }

    private SearchSourceBuilder buildPagination(final PageOptions pageOptions, final SearchSourceBuilder requestBuilder) {
        if (Optional.ofNullable(pageOptions).isPresent()) {
            requestBuilder.size(pageOptions.getSize());
            requestBuilder.from(pageOptions.getNumber() - 1); // ElasticSearch starts from 0
            if (pageOptions.getNumber() > 1) {
                requestBuilder.from((pageOptions.getNumber() * pageOptions.getSize()));
            }
        }

        return requestBuilder;
    }

    private SearchSourceBuilder buildFilters(final FilterOptions filterOptions,
        final FacetOptions facetOptions,
        final SearchSourceBuilder requestBuilder) {

        if (Optional.ofNullable(filterOptions).isPresent()) {
            final BoolQueryBuilder filterQuery = FILTER_ADAPTER.toQueryBuilder(filterOptions, facetOptions);
            requestBuilder.query(filterQuery);
        }

        return requestBuilder;
    }
}
