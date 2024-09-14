package org.copperforge.mog.conversion.dotout;

public class DOField {

    private String name;

    private DOFieldType type;

    private Boolean nullable = true;

    private Integer startPos;

    private Integer length;

    private Integer integral;

    private Integer precision;

    @Override
    public String toString() {
        String nullableInd = nullable ? "(*) " : "";
        String lengthDesc = "" + length;
        if (integral != null) {
            lengthDesc = "" + integral;
            if (precision != null) {
                lengthDesc += ", " + precision;
            }
        }
        return nullableInd + name + " [ " + type + "(" + lengthDesc + ")" + " ]";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DOFieldType getType() {
        return type;
    }

    public void setType(DOFieldType type) {
        this.type = type;
    }

    public Boolean getNullable() {
        return nullable;
    }

    public void setNullable(Boolean nullable) {
        this.nullable = nullable;
    }

    public Integer getStartPos() {
        return startPos;
    }

    public void setStartPos(Integer startPos) {
        this.startPos = startPos;
    }

    public Integer getLength() {
        if (nullable) return length + 1;
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public Integer getIntegral() {
        return integral;
    }

    public void setIntegral(Integer integral) {
        this.integral = integral;
    }

    public Integer getPrecision() {
        return precision;
    }

    public void setPrecision(Integer precision) {
        this.precision = precision;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((nullable == null) ? 0 : nullable.hashCode());
        result = prime * result + ((startPos == null) ? 0 : startPos.hashCode());
        result = prime * result + ((length == null) ? 0 : length.hashCode());
        result = prime * result + ((integral == null) ? 0 : integral.hashCode());
        result = prime * result + ((precision == null) ? 0 : precision.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DOField other = (DOField) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (type != other.type)
            return false;
        if (nullable == null) {
            if (other.nullable != null)
                return false;
        } else if (!nullable.equals(other.nullable))
            return false;
        if (startPos == null) {
            if (other.startPos != null)
                return false;
        } else if (!startPos.equals(other.startPos))
            return false;
        if (length == null) {
            if (other.length != null)
                return false;
        } else if (!length.equals(other.length))
            return false;
        if (integral == null) {
            if (other.integral != null)
                return false;
        } else if (!integral.equals(other.integral))
            return false;
        if (precision == null) {
            if (other.precision != null)
                return false;
        } else if (!precision.equals(other.precision))
            return false;
        return true;
    }

}
