package org.copperforge.mog.data.catalog;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.copperforge.mog.MogException;
import org.copperforge.mog.reporting.definition.Column;
import org.copperforge.mog.reporting.xlsx.XLSXReport;
import org.copperforge.mog.reporting.xlsx.XLSXTableWriter;
import org.copperforge.mog.reporting.element.table.Table;
import org.copperforge.mog.reporting.ReportDataSource;
import org.copperforge.mog.data.filter.MogJsonFilter;
import org.copperforge.mog.runtime.MogContext;
import org.junit.jupiter.api.Test;

public class DataSourcesCatalogIntegrationTest {

    @Test
    void table_resolvesDataSource_fromCatalog() throws Exception {
        resetCatalog();
        // temp dir with datasources.mog and sales.json
        Path tmp = Files.createTempDirectory("mog-ds-test");
        Path sales = tmp.resolve("sales.json");
        String json = "{ \"data\": [ { \"region\": \"North\", \"revenue\": 100 }, { \"region\": \"South\", \"revenue\": 120 } ] }";
        Files.writeString(sales, json, StandardCharsets.UTF_8);

        Path catalog = tmp.resolve("datasources.mog");
        String ds = "{\n  \"datasources\": [ { \"name\": \"inline\", \"type\": \"json\", \"file\": \"" + sales.toString().replace("\\", "\\\\") + "\" } ]\n}";
        Files.writeString(catalog, ds, StandardCharsets.UTF_8);

        MogContext context = MogContext.builder().datasourcesPath(tmp.toString()).build();

        // Build a table that references the catalog datasource by name
        Table t = new Table();
        t.setName("t_sales");
        t.setTitle("Sales");
        var ul = new org.copperforge.mog.reporting.definition.CellReference(); ul.setRow(1); ul.setCol(1);
        t.setUpperLeft(ul);
        Column c1 = new Column(); c1.setTitle("Region");
        Column c2 = new Column(); c2.setTitle("Revenue");
        t.setColumns(java.util.List.of(c1, c2));
        ReportDataSource rds = new ReportDataSource();
        rds.setName("inline");
        MogJsonFilter filter = new MogJsonFilter();
        filter.setJsonPath("$.data[*]");
        rds.setFilter(filter);
        t.setDataSource(rds);

        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet sheet = wb.createSheet("S");
        XLSXReport report = new XLSXReport();
        report.setContext(context);
        new XLSXTableWriter().workbook(wb).sheet(sheet).write(report, t);

        // Assert data wrote: header + 2 data rows
        assertEquals(3, sheet.getLastRowNum() + 1);
        assertEquals("Region", sheet.getRow(0).getCell(0).getStringCellValue());
        assertEquals("Sales", sheet.getTables().get(0).getCTTable().getDisplayName());
    }

    private static void resetCatalog() throws Exception {
        Field f = DataSourcesCatalog.class.getDeclaredField("INSTANCE");
        f.setAccessible(true);
        f.set(null, null);
    }

}
