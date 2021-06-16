package com.axibase.tsd.api.model.series.query;

import com.axibase.tsd.api.model.series.Sample;
import com.axibase.tsd.api.model.series.Series;
import com.axibase.tsd.api.model.series.SeriesType;
import com.axibase.tsd.api.model.series.query.transformation.Transformation;
import com.axibase.tsd.api.model.series.query.transformation.aggregate.Aggregate;
import com.axibase.tsd.api.model.series.query.transformation.downsample.Downsample;
import com.axibase.tsd.api.model.series.query.transformation.evaluate.Evaluate;
import com.axibase.tsd.api.model.series.query.transformation.forecast.Forecast;
import com.axibase.tsd.api.model.series.query.transformation.group.Group;
import com.axibase.tsd.api.model.series.query.transformation.interpolate.Interpolate;
import com.axibase.tsd.api.model.series.query.transformation.rate.Rate;
import com.axibase.tsd.api.model.series.query.transformation.smooth.Smooth;
import com.axibase.tsd.api.util.Util;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.Wither;

import java.util.*;
import java.util.stream.Collectors;

import static com.axibase.tsd.api.util.Util.MAX_QUERYABLE_DATE;
import static com.axibase.tsd.api.util.Util.MIN_QUERYABLE_DATE;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@Builder(toBuilder = true)
@Wither
public class SeriesQuery {
    private String entity;
    private String entityGroup;
    private String entityExpression;
    private String tagExpression;
    private List<String> entities;
    private String forecastName;
    private String metric;
    private String name;
    private List<SeriesSettings> series;
    private String startDate;
    private String endDate;
    private Interval interval;
    private Map<String, List<String>> tags;
    private Aggregate aggregate;
    private Interpolate interpolate;
    private Group group;
    private Rate rate;
    private Smooth smooth;
    private Downsample downsample;
    private Forecast forecast;
    private Evaluate evaluate;
    private String timeFormat;
    private Boolean exactMatch;
    private Integer limit;
    private Boolean cache;
    private String direction;
    private Integer seriesLimit;
    private Boolean versioned;
    private Boolean addMeta;
    private SeriesType type;
    private List<Transformation> transformationOrder;
    private String minInsertDate;
    private String maxInsertDate;
    private TimeZone timezone;

    public SeriesQuery() {
    }

    public SeriesQuery(Series series) {
        entity = escapeExpression(series.getEntity());
        metric = series.getMetric();
        tags = series.getTags().entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey, entry -> Util.wrapInMutableList(escapeExpression(entry.getValue()))));
        exactMatch = true;
        if (series.getData().isEmpty()) {
            startDate = MIN_QUERYABLE_DATE;
            endDate = MAX_QUERYABLE_DATE;
        } else {
            setIntervalBasedOnSeriesDate(series);
        }
        type = series.getType();
    }

    public SeriesQuery(String entity, String metric) {
        this.entity = entity;
        this.metric = metric;
    }

    public SeriesQuery(String entity, String metric, long startTime, long endTime) {
        this(entity, metric, Util.ISOFormat(startTime), Util.ISOFormat(endTime), new HashMap<>());
    }

    public SeriesQuery(String entity, String metric, String startDate, String endDate) {
        this(entity, metric, startDate, endDate, new HashMap<>());
    }

    public SeriesQuery(String entity, String metric, String startDate, String endDate, Map<String, String> tags) {
        this.entity = entity;
        this.metric = metric;
        this.startDate = startDate;
        this.endDate = endDate;
        this.tags = tags.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> Util.wrapInMutableList(entry.getValue())));
    }

    private String escapeExpression(String expression) {
        StringBuilder escapedName = new StringBuilder();
        for (char c : expression.toCharArray()) {
            if (c == '*' || c == '?' || c == '\\') {
                escapedName.append('\\');
            }
            escapedName.append(c);
        }
        return escapedName.toString();
    }

    public SeriesQuery addTag(String tag, String value) {
        tags.computeIfAbsent(tag, t -> new ArrayList<>(1)).add(value);
        return this;
    }

    public SeriesQuery addSeries(SeriesSettings series) {
        if (this.series == null) {
            this.series = new ArrayList<>();
        }
        this.series.add(series);
        return this;
    }

    private void setIntervalBasedOnSeriesDate(final Series series) {
        long minDate = Util.getUnixTime(MAX_QUERYABLE_DATE);
        long maxDate = Util.getUnixTime(MIN_QUERYABLE_DATE);

        Long curDate;
        for (Sample sample : series.getData()) {
            curDate = sample.getUnixTime();
            if (curDate == null) {
                curDate = Util.getUnixTime(sample.getRawDate());
            }
            minDate = Math.min(curDate, minDate);
            maxDate = Math.max(curDate, maxDate);
        }

        setStartDate(Util.ISOFormat(minDate));
        setEndDate(Util.ISOFormat(maxDate + 1));
    }

    @Override
    public String toString() {
        return Util.prettyPrint(this);
    }
}
