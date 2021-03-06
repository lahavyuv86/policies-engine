package org.hawkular.alerts.api.model.condition;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.hawkular.alerts.api.doc.DocModel;
import org.hawkular.alerts.api.doc.DocModelProperty;
import org.hawkular.alerts.api.json.JacksonDeserializer;
import org.hawkular.alerts.api.model.trigger.Mode;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * A base class for condition definition.
 *
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
@DocModel(description = "A base class for condition definition. ",
        subTypes = { AvailabilityCondition.class, CompareCondition.class, EventCondition.class, ExternalCondition.class,
            MissingCondition.class, NelsonCondition.class, RateCondition.class, StringCondition.class,
            ThresholdCondition.class, ThresholdRangeCondition.class })
@JsonDeserialize(using = JacksonDeserializer.ConditionDeserializer.class)
public abstract class Condition implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Type {
        AVAILABILITY, COMPARE, STRING, THRESHOLD, RANGE, EXTERNAL, EVENT, RATE, MISSING, NELSON
    }

    @DocModelProperty(description = "Tenant id owner of this condition.",
            position = 0,
            required = true,
            allowableValues = "Tenant is overwritten from Hawkular-Tenant HTTP header parameter request")
    @JsonInclude
    protected String tenantId;

    /**
     * The owning trigger
     */
    @DocModelProperty(description = "The owning trigger.",
            position = 1,
            allowableValues = "triggerId is set up from REST request parameters")
    @JsonInclude
    protected String triggerId;

    /**
     * The owning trigger's mode when this condition is active
     */
    @DocModelProperty(description = "The owning trigger's mode when this condition is active.",
            position = 2,
            required = true)
    @JsonInclude
    protected Mode triggerMode;

    @DocModelProperty(description = "The type of the condition defined. Each type has its specific properties defined " +
            "on its subtype of condition.",
            position = 3,
            required = true)
    @JsonInclude
    protected Type type;

    /**
     * Number of conditions associated with a particular trigger.
     * i.e. 2 [ conditions ]
     */
    @DocModelProperty(description = "Number of conditions associated with a particular trigger. This is a read-only value " +
            "defined by the system.",
            position = 4)
    @JsonInclude
    protected int conditionSetSize;

    /**
     * Index of the current condition
     * i.e. 1 [ of 2 conditions ]
     */
    @DocModelProperty(description = "Index of the current condition. This is a read-only value defined by the system.",
            position = 5)
    @JsonInclude
    protected int conditionSetIndex;

    /**
     * A composed key for the condition
     */
    @DocModelProperty(description = "A composed key for the condition. This is a read-only value defined by the system.",
            position = 6)
    @JsonInclude
    protected String conditionId;

    @DocModelProperty(description = "Properties defined by the user for this condition.",
            position = 7)
    @JsonInclude(Include.NON_EMPTY)
    protected Map<String, String> context;

    @DocModelProperty(description = "A canonical display string for the condition expression. Can be null until the " +
            "condition is fully defined.",
            position = 7)
    @JsonInclude(Include.NON_EMPTY)
    public String displayString;

    @DocModelProperty(description = "Last time this trigger was evaluated.", position = 8)
    @JsonInclude
    private long lastEvaluation;

    public Condition() {
        // for json assembly
    }

    public Condition(String tenantId, String triggerId, Mode triggerMode, int conditionSetSize, int conditionSetIndex,
            Type type) {
        this.tenantId = tenantId;
        this.triggerId = triggerId;
        this.triggerMode = triggerMode;
        this.conditionSetSize = conditionSetSize;
        this.conditionSetIndex = conditionSetIndex;
        this.type = type;
        updateId();
    }

    public Condition(Condition condition) {
        if (condition == null) {
            throw new IllegalArgumentException("condition must be not null");
        }
        this.tenantId = condition.getTenantId();
        this.triggerId = condition.getTriggerId();
        this.triggerMode = condition.getTriggerMode();
        this.conditionSetSize = condition.getConditionSetSize();
        this.conditionSetIndex = condition.conditionSetIndex;
        this.type = condition.getType();
        this.context = new HashMap<>(condition.getContext());
        this.displayString = condition.displayString;
        this.lastEvaluation = condition.lastEvaluation;
        updateId();
    }

    public int getConditionSetIndex() {
        return conditionSetIndex;
    }

    public void setConditionSetIndex(int conditionSetIndex) {
        this.conditionSetIndex = conditionSetIndex;
        updateId();
    }

    public int getConditionSetSize() {
        return conditionSetSize;
    }

    public void setConditionSetSize(int conditionSetSize) {
        this.conditionSetSize = conditionSetSize;
        updateId();
    }

    public String getTriggerId() {
        return triggerId;
    }

    public void setTriggerId(String triggerId) {
        this.triggerId = triggerId;
        updateId();
    }

    public Mode getTriggerMode() {
        return triggerMode;
    }

    public void setTriggerMode(Mode triggerMode) {
        this.triggerMode = triggerMode;
        updateId();
    }

    public String getConditionId() {
        return conditionId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
        updateId();
    }

    public Map<String, String> getContext() {
        if (null == context) {
            context = new HashMap<>();
        }
        return context;
    }

    public void setContext(Map<String, String> context) {
        this.context = context;
    }

    public String getDisplayString() {
        if (null == displayString) {
            updateDisplayString();
        }
        return displayString;
    }

    public void setDisplayString(String displayString) {
        this.displayString = displayString;
    }

    public long getLastEvaluation() {
        return lastEvaluation;
    }

    public void setLastEvaluation(long lastEvaluation) {
        this.lastEvaluation = lastEvaluation;
    }

    private void updateId() {
        StringBuilder sb = new StringBuilder(tenantId);
        sb.append("-").append(triggerId);
        sb.append("-").append(triggerMode.name());
        sb.append("-").append(conditionSetSize);
        sb.append("-").append(conditionSetIndex);
        this.conditionId = sb.toString();
    }

    public Type getType() {
        return type;
    }

    /**
     * Validate throws IllegalArgumentExceptions if the condition is not valid
     */
    public void validate() {}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Condition condition = (Condition) o;
        return conditionSetSize == condition.conditionSetSize &&
                conditionSetIndex == condition.conditionSetIndex &&
                Objects.equals(tenantId, condition.tenantId) &&
                Objects.equals(triggerId, condition.triggerId) &&
                triggerMode == condition.triggerMode &&
                type == condition.type &&
                Objects.equals(conditionId, condition.conditionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, triggerId, triggerMode, type, conditionSetSize, conditionSetIndex, conditionId, context);
    }

    @Override
    public String toString() {
        return "Condition [tenantId=" + tenantId + "triggerId=" + triggerId + ", triggerMode=" + triggerMode
                + ", conditionSetSize=" + conditionSetSize + ", conditionSetIndex=" + conditionSetIndex + "]";
    }

    /**
     * @return The dataId, can be null if the Condition has no relevant dataId.
     */
    @DocModelProperty(description = "Data identifier used for condition evaluation. dataId is used in conjunction with " +
                "operators defined at subtype condition level.",
            position = 8,
            required = true,
            name = "dataId")
    public abstract String getDataId();

    /**
     * Build displayString given the current field settings, and call {@link #setDisplayString(String)}.
     * <p>
     * NOTE that if, after construction, a client updated a relevant Condition field, he will also need to
     * then call {@link #updateDisplayString()} to ensure the displayString is correct.</p>
     */
    @JsonIgnore
    public abstract void updateDisplayString();

    public void updateLastEvaluation() {
        this.lastEvaluation = System.currentTimeMillis();
    }

    /**
     * Used to determine whether two conditions have differences. The base implementation is equals(), subclassed
     * should override if this is not sufficient.
     *
     * @param c the other Condition
     * @return true if this Condition has the same persisted field values as the other condition.
     */
    public boolean isSame(Condition c) {
        return this.equals(c);
    }
}
