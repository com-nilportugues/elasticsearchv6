package com.nilportugues.elasticsearchv6.repository;

import com.nilportugues.shared.domain.FacetOptions;
import com.nilportugues.shared.domain.FilterOptions;
import org.elasticsearch.index.query.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class ElasticSearchFilterAdapter {

    @SuppressWarnings("unchecked")
    public BoolQueryBuilder toQueryBuilder(final FilterOptions filter, FacetOptions facetOptions) {

        final HashMap<String, Long> boostFields = new HashMap<>();
        if (null != facetOptions) {
            facetOptions.getFieldBoost().entrySet().forEach(pair -> boostFields.put(pair.getKey(), pair.getValue()));
        }

        final BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        Set range = filter.getRanges().entrySet();
        if (1 == range.size()) {
            Entry entry = (Entry) range.toArray()[0];
            ArrayList<String> data = (ArrayList<String>) entry.getValue();

            String start = data.get(0);
            String end = data.get(1);

            if (start != null && end != null) {
                RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(entry.getKey().toString());
                rangeQuery.from(start).to(end);

                if (!boostFields.isEmpty()) {
                    final Long boostValue = boostFields.get(entry.getKey().toString());
                    rangeQuery.boost(boostValue);
                }

                boolQueryBuilder.must(rangeQuery);
            }
        }

        Set notRanges = filter.getNotRanges().entrySet();
        if (1 == notRanges.size()) {
            Entry entry = (Entry) notRanges.toArray()[0];
            ArrayList<String> data = (ArrayList<String>) entry.getValue();

            String start = data.get(0);
            String end = data.get(1);

            if (start != null && end != null) {
                RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(entry.getKey().toString());
                rangeQuery.lte(start);
                rangeQuery.gte(end);

                if (!boostFields.isEmpty()) {
                    final Long boostValue = boostFields.get(entry.getKey().toString());
                    rangeQuery.boost(boostValue);
                }

                boolQueryBuilder.must(rangeQuery);
            }
        }

        filter.getEquals().entrySet().forEach(entry -> {
            final Entry<String, List> castedEntry = (Entry) entry;
            castedEntry.getValue().forEach(value -> {
                final MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery(castedEntry.getKey(), castedEntry.getValue());

                if (!boostFields.isEmpty()) {
                    final Long boostValue = boostFields.get(castedEntry.getKey());
                    matchQueryBuilder.boost(boostValue);
                }

                boolQueryBuilder.must(matchQueryBuilder);
            });
        });

        filter.getNot().entrySet().forEach(entry -> {
            final Entry<String, List> castedEntry = (Entry) entry;
            castedEntry.getValue().forEach(value -> {
                final MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery(castedEntry.getKey(), castedEntry.getValue());

                if (!boostFields.isEmpty()) {
                    final Long boostValue = boostFields.get(castedEntry.getKey());
                    matchQueryBuilder.boost(boostValue);
                }

                boolQueryBuilder.mustNot(matchQueryBuilder);
            });
        });

        filter.getEnds().entrySet().forEach(entry -> {
            final Entry<String, List> castedEntry = (Entry) entry;
            castedEntry.getValue().forEach(value -> {
                final String endsRegex = "(.*)?(" + value + ")";
                final RegexpQueryBuilder regexpQueryBuilder = QueryBuilders.regexpQuery(castedEntry.getKey(), endsRegex);

                if (!boostFields.isEmpty()) {
                    final Long boostValue = boostFields.get(castedEntry.getKey());
                    regexpQueryBuilder.boost(boostValue);
                }

                boolQueryBuilder.must(regexpQueryBuilder);
            });
        });

        filter.getNotEnds().entrySet().forEach(entry -> {
            final Entry<String, List> castedEntry = (Entry) entry;
            castedEntry.getValue().forEach(value -> {
                final String endsRegex = "(.*)?(" + value + ")";
                final RegexpQueryBuilder regexpQueryBuilder = QueryBuilders.regexpQuery(castedEntry.getKey(), endsRegex);

                if (!boostFields.isEmpty()) {
                    final Long boostValue = boostFields.get(castedEntry.getKey());
                    regexpQueryBuilder.boost(boostValue);
                }

                boolQueryBuilder.mustNot(regexpQueryBuilder);
            });
        });

        filter.getStarts().entrySet().forEach(entry -> {
            final Entry<String, List> castedEntry = (Entry) entry;
            castedEntry.getValue().forEach(value -> {
                final String startsRegex = "(" + value + ")(.*)?";
                final RegexpQueryBuilder regexpQueryBuilder = QueryBuilders.regexpQuery(castedEntry.getKey(), startsRegex);

                if (!boostFields.isEmpty()) {
                    final Long boostValue = boostFields.get(castedEntry.getKey());
                    regexpQueryBuilder.boost(boostValue);
                }

                boolQueryBuilder.must(regexpQueryBuilder);
            });
        });

        filter.getNotStarts().entrySet().forEach(entry -> {
            final Entry<String, List> castedEntry = (Entry) entry;
            castedEntry.getValue().forEach(value -> {
                final String startsRegex = "(" + value + ")(.*)?";
                final RegexpQueryBuilder regexpQueryBuilder = QueryBuilders.regexpQuery(castedEntry.getKey(), startsRegex);

                if (!boostFields.isEmpty()) {
                    final Long boostValue = boostFields.get(castedEntry.getKey());
                    regexpQueryBuilder.boost(boostValue);
                }

                boolQueryBuilder.mustNot(regexpQueryBuilder);
            });
        });

        filter.getHas().entrySet().forEach(entry -> {
            final Entry<String, List> castedEntry = (Entry) entry;
            castedEntry.getValue().forEach(value -> {
                try {
                    // @todo: propagate this everywhere here.
                    List<String> values = (List<String>) value;
                    if (values.size() > 1) {
                        values.forEach(v -> {
                            final QueryStringQueryBuilder queryStringQuery = QueryBuilders.queryStringQuery("*" + v + "*");

                            if (!boostFields.isEmpty()) {
                                final Long boostValue = boostFields.get(castedEntry.getKey());
                                queryStringQuery.boost(boostValue);
                            }

                            boolQueryBuilder.must(queryStringQuery);
                        });

                    } else {
                        final QueryStringQueryBuilder queryStringQuery = QueryBuilders.queryStringQuery("*" + values.get(0) + "*");

                        if (!boostFields.isEmpty()) {
                            final Long boostValue = boostFields.get(castedEntry.getKey());
                            queryStringQuery.boost(boostValue);
                        }

                        boolQueryBuilder.must(queryStringQuery);
                    }
                } catch (ClassCastException e) {

                }
            });
        });

        filter.getNotHas().entrySet().forEach(entry -> {
            final Entry<String, List> castedEntry = (Entry) entry;
            castedEntry.getValue().forEach(value -> {
                final QueryStringQueryBuilder queryStringQuery = QueryBuilders.queryStringQuery("*" + value + "*");

                if (!boostFields.isEmpty()) {
                    final Long boostValue = boostFields.get(castedEntry.getKey());
                    queryStringQuery.boost(boostValue);
                }

                boolQueryBuilder.mustNot(queryStringQuery);
            });
        });

        filter.getGreaterThan().entrySet().forEach(entry -> {
            final Entry<String, List> castedEntry = (Entry) entry;
            castedEntry.getValue().forEach(value -> {
                final RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(castedEntry.getKey());
                rangeQuery.gt(castedEntry.getValue());

                if (!boostFields.isEmpty()) {
                    final Long boostValue = boostFields.get(castedEntry.getKey());
                    rangeQuery.boost(boostValue);
                }

                boolQueryBuilder.must(rangeQuery);
            });
        });

        filter.getGreaterThanOrEqual().entrySet().forEach(entry -> {
            final Entry<String, List> castedEntry = (Entry) entry;
            castedEntry.getValue().forEach(value -> {
                final RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(castedEntry.getKey());
                rangeQuery.gte(castedEntry.getValue());

                if (!boostFields.isEmpty()) {
                    final Long boostValue = boostFields.get(castedEntry.getKey());
                    rangeQuery.boost(boostValue);
                }

                boolQueryBuilder.must(rangeQuery);
            });
        });

        filter.getLessThan().entrySet().forEach(entry -> {
            final Entry<String, List> castedEntry = (Entry) entry;
            castedEntry.getValue().forEach(value -> {
                final RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(castedEntry.getKey());
                rangeQuery.lt(castedEntry.getValue());

                if (!boostFields.isEmpty()) {
                    final Long boostValue = boostFields.get(castedEntry.getKey());
                    rangeQuery.boost(boostValue);
                }

                boolQueryBuilder.must(rangeQuery);
            });
        });

        filter.getLessThanOrEqual().entrySet().forEach(entry -> {
            final Entry<String, List> castedEntry = (Entry) entry;
            castedEntry.getValue().forEach(value -> {
                final RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(castedEntry.getKey());
                rangeQuery.lte(castedEntry.getValue());

                if (!boostFields.isEmpty()) {
                    final Long boostValue = boostFields.get(castedEntry.getKey());
                    rangeQuery.boost(boostValue);
                }

                boolQueryBuilder.must(rangeQuery);
            });
        });

        filter.getIn().entrySet().forEach(entry -> {
            final Entry<String, List> castedEntry = (Entry) entry;
            castedEntry.getValue().forEach(value -> {
                final MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery(castedEntry.getKey(), castedEntry.getValue());

                if (!boostFields.isEmpty()) {
                    final Long boostValue = boostFields.get(castedEntry.getKey());
                    matchQueryBuilder.boost(boostValue);
                }

                boolQueryBuilder.must(matchQueryBuilder);
            });
        });

        filter.getNotIn().entrySet().forEach(entry -> {
            final Entry<String, List> castedEntry = (Entry) entry;
            castedEntry.getValue().forEach(value -> {
                final MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery(castedEntry.getKey(), castedEntry.getValue());

                if (!boostFields.isEmpty()) {
                    final Long boostValue = boostFields.get(castedEntry.getKey());
                    matchQueryBuilder.boost(boostValue);
                }

                boolQueryBuilder.mustNot(matchQueryBuilder);
            });
        });

        return boolQueryBuilder;
    }
}
