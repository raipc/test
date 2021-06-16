package com.axibase.tsd.api.model.command;


import com.axibase.tsd.api.model.common.InterpolationMode;
import com.axibase.tsd.api.model.metric.Metric;
import com.axibase.tsd.api.model.series.DataType;

import java.util.Map;


public class MetricCommand extends AbstractCommand {
    private static final String METRIC_COMMAND = "metric";
    private String metricName;
    private String label;
    private String description;
    private String filterExpression;
    private Boolean versioning;
    private DataType dataType;
    private String timeZoneId;
    private Map<String, String> tags;
    private Boolean enabled;
    private InterpolationMode interpolate;


    public MetricCommand(String metricName) {
        super(METRIC_COMMAND);
        this.metricName = metricName;
    }

    public MetricCommand(Metric metric) {
        this(metric.getName());
        setInterpolate(metric.getInterpolate());
        setDataType(metric.getDataType());
        setDescription(metric.getDescription());
        setTags(metric.getTags());
        setFilterExpression(metric.getFilter());
        setVersioning(metric.getVersioned());
        setLabel(metric.getLabel());
        setTimeZoneId(metric.getTimeZoneID());
        setEnabled(metric.getEnabled());
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFilterExpression() {
        return filterExpression;
    }

    public void setFilterExpression(String filterExpression) {
        this.filterExpression = filterExpression;
    }

    public Boolean getVersioning() {
        return versioning;
    }

    public void setVersioning(Boolean versioning) {
        this.versioning = versioning;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public InterpolationMode getInterpolate() {
        return interpolate;
    }

    public void setInterpolate(InterpolationMode interpolate) {
        this.interpolate = interpolate;
    }

    public String getTimeZoneId() {
        return timeZoneId;
    }

    public void setTimeZoneId(String timeZoneId) {
        this.timeZoneId = timeZoneId;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String compose() {
        StringBuilder stringBuilder = commandBuilder();
        if (this.metricName != null) {
            stringBuilder.append(FieldFormat.quoted("m", metricName));
        }
        if (this.label != null) {
            stringBuilder.append(FieldFormat.quoted("l", label));
        }
        if (this.description != null) {
            stringBuilder.append(FieldFormat.quoted("d", description));
        }
        if (this.dataType != null) {
            stringBuilder.append(FieldFormat.quoted("p", dataType.toString()));
        }
        if (this.interpolate != null) {
            stringBuilder.append(FieldFormat.quoted("i", interpolate.toString()));
        }
        if (this.filterExpression != null) {
            stringBuilder.append(FieldFormat.quoted("f", filterExpression));
        }
        if (this.versioning != null) {
            stringBuilder.append(FieldFormat.quoted("v", versioning.toString()));
        }
        if (this.timeZoneId != null) {
            stringBuilder.append(FieldFormat.quoted("z", timeZoneId));
        }
        if (this.enabled != null) {
            stringBuilder.append(FieldFormat.quoted("b", enabled.toString()));
        }
        if (this.tags != null) {
            for (Map.Entry<String, String> entry : tags.entrySet()) {
                stringBuilder.append(FieldFormat.keyValue("t", entry.getKey(), entry.getValue()));
            }
        }
        return stringBuilder.toString();
    }
}
