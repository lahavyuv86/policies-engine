package org.hawkular.alerts.api.model.condition;

import org.hawkular.alerts.api.doc.DocModel;
import org.hawkular.alerts.api.doc.DocModelProperty;
import org.hawkular.alerts.api.model.trigger.Mode;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.util.Objects;

/**
 * A <code>MissingCondition</code> is used to evaluate when a data or an event has not been received on time interval.
 *
 * A MissingCondition will be evaluated to true when a data/event has not been received in the last interval time,
 * in milliseconds, starting to count from trigger was enabled or last received data/event.
 *
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
@DocModel(description = "A MissingCondition is used to evaluate when a data or an event has not been received " +
        "on time interval. + \n" +
        " + \n" +
        "A MissingCondition will be evaluated to true when a data/event has not been received in the " +
        "last interval time, in milliseconds, starting to count from trigger was enabled or last received data/event.")
public class MissingCondition extends Condition {

    private static final long serialVersionUID = 1L;

    @JsonInclude
    private String dataId;

    @DocModelProperty(description = "A time interval defined in milliseconds.",
            position = 0,
            required = true)
    @JsonInclude(Include.NON_NULL)
    private long interval;

    public MissingCondition() {
        this("", "", Mode.FIRING, 1, 1, null, 0L);
    }

    public MissingCondition(String tenantId, String triggerId, String dataId, long interval) {
        this(tenantId, triggerId, Mode.FIRING, 1, 1, dataId, interval);
    }

    public MissingCondition(String tenantId, String triggerId, Mode triggerMode, String dataId, long interval) {
        this(tenantId, triggerId, triggerMode, 1, 1, dataId, interval);
    }

    /**
     * This constructor requires the tenantId be assigned prior to persistence. It can be used when
     * creating triggers via Rest, as the tenant will be assigned automatically.
     */
    public MissingCondition(String triggerId, Mode triggerMode, String dataId, long interval) {
        this("", triggerId, triggerMode, 1, 1, dataId, interval);
    }

    public MissingCondition(String tenantId, String triggerId, Mode triggerMode, int conditionSetSize,
            int conditionSetIndex, String dataId, long interval) {
        super(tenantId, triggerId, (null == triggerMode ? Mode.FIRING : triggerMode), conditionSetSize,
                conditionSetIndex, Type.MISSING);
        this.dataId = dataId;
        this.interval = interval;
        updateDisplayString();
    }

    public MissingCondition(MissingCondition condition) {
        super(condition);

        this.dataId = condition.getDataId();
        this.interval = condition.getInterval();
    }

    @Override
    public String getDataId() {
        return dataId;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public boolean match(long previousTime, long time) {
        return (previousTime + interval) < time;
    }

    @Override
    public void updateDisplayString() {
        String s = String.format("%s missing GTE %dms", this.dataId, this.interval);
        setDisplayString(s);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        MissingCondition that = (MissingCondition) o;
        return interval == that.interval &&
                Objects.equals(dataId, that.dataId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), dataId, interval);
    }

    @Override
    public String toString() {
        return "MissingCondition{" +
                "dataId='" + dataId + '\'' +
                ", interval=" + interval +
                '}';
    }
}
