package org.copperforge.mog.reporting.element;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.copperforge.mog.reporting.element.table.PivotTable;
import org.copperforge.mog.reporting.element.chart.Chart;
import org.copperforge.mog.reporting.element.table.Table;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = SpannedText.class, name = "spannedText"),
    @JsonSubTypes.Type(value = Table.class, name = "table"),
    @JsonSubTypes.Type(value = PivotTable.class, name = "pivotTable"),
    @JsonSubTypes.Type(value = Chart.class, name = "chart")
})
public class ReportElement {

    private String type;
    private Integer height;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    @Override
    public String toString() {
        return "ReportElementDefinition [type=" + type + ", height=" + height + "]";
    }

}
