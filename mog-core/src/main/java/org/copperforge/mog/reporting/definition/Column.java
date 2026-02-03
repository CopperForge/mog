package org.copperforge.mog.reporting.definition;

import org.apache.poi.ss.usermodel.CellStyle;

public class Column {

    private String title;

    private String key;

    private int span = 1;

    private CellStyle style = null;

    private Integer width;

    public Column() {
        
    }

    public Column(String name) {
        this.title = name;
        this.key = nameToKey(name);
    }

    public Column(String name, int width) {
        this.title = name;
        this.key = nameToKey(name);
        this.width = width;
    }

    public Column(String name, String key) {
        this.title = name;
        this.key = key;
    }

    public Column(String name, String key, int width) {
        this.title = name;
        this.key = key;
        this.width = width;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String name) {
        this.title = name;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getSpan() {
        return span;
    }

    public void setSpan(int span) {
        this.span = span;
    }

    public CellStyle getStyle() {
        return style;
    }

    public void setStyle(CellStyle style) {
        this.style = style;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    @Override
    public String toString() {
        return "ReportTableColumn [title=" + title + ", key=" + key + ", span=" + span + ", style=" + style + ", width="
                + width + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((title == null) ? 0 : title.hashCode());
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        result = prime * result + span;
        result = prime * result + ((style == null) ? 0 : style.hashCode());
        result = prime * result + width;
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
        Column other = (Column) obj;
        if (title == null) {
            if (other.title != null)
                return false;
        } else if (!title.equals(other.title))
            return false;
        if (key == null) {
            if (other.key != null)
                return false;
        } else if (!key.equals(other.key))
            return false;
        if (span != other.span)
            return false;
        if (style == null) {
            if (other.style != null)
                return false;
        } else if (!style.equals(other.style))
            return false;
        if (width != other.width)
            return false;
        return true;
    }

    private String nameToKey(String name) {
        String[] words = name.split("[\\W_]+");

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (i == 0) {
                word = word.isEmpty() ? word : word.toLowerCase();
            } else {
                word = word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();      
            }
            builder.append(word);
        }
        return builder.toString();
    }
}
