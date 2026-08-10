package org.copperforge.mog.reporting.xlsx;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Workbook;
import org.copperforge.mog.MogException;

public class XLSXStyleCache {

    private final Workbook workbook;
    private final Map<String, XLSXStyle> stylesByName = new LinkedHashMap<>();
    private final Map<String, CellStyle> cellStylesByName = new HashMap<>();

    public XLSXStyleCache(Workbook workbook, List<XLSXStyle> styles) {
        this.workbook = workbook;
        if (styles == null) {
            return;
        }
        for (XLSXStyle style : styles) {
            if (style != null && style.getName() != null && !style.getName().isBlank()) {
                stylesByName.put(style.getName(), style);
            }
        }
    }

    public XLSXStyle style(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return stylesByName.get(name);
    }

    public CellStyle cellStyle(String name) throws MogException {
        if (name == null || name.isBlank()) {
            return null;
        }
        XLSXStyle style = style(name);
        if (style == null) {
            return null;
        }
        if (!(style instanceof XLSXCellStyle cellStyle)) {
            throw new MogException("XLSX style '" + name + "' is type '" + style.getType()
                    + "', not a reusable cell style");
        }
        CellStyle cached = cellStylesByName.get(name);
        if (cached != null) {
            return cached;
        }
        try {
            cached = cellStyle.cellStyle(workbook);
        } catch (IllegalArgumentException e) {
            throw new MogException("Invalid XLSX cell style '" + name + "': " + e.getMessage(), e);
        }
        cellStylesByName.put(name, cached);
        return cached;
    }
}
