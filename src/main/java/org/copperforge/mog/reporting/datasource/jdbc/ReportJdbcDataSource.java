package org.copperforge.mog.reporting.datasource.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.copperforge.mog.reporting.core.Reportable;
import org.copperforge.mog.reporting.datasource.ReportDataSource;

public class ReportJdbcDataSource extends ReportDataSource {

    private String url;

    private String user;

    private String password;

    private String query;

    private String jdbcClass;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getJdbcClass() {
        return jdbcClass;
    }

    public void setJdbcClass(String jdbcClass) {
        this.jdbcClass = jdbcClass;
    }

    @Override
    public String toString() {
        return "ReportJdbcDataSource [url=" + url + ", user=" + user + ", password=*, query=" + query
                + ", jdbcClass=" + jdbcClass + "]";
    }

    @Override
    public List<Reportable> data() {
        List<Reportable> data = new ArrayList<>();
        try {
            String url = getUrl(); // table details
            String username = getUser(); // MySQL credentials
            String password = getPassword();
            String query = getQuery(); // query to be run
            Class.forName(getJdbcClass()); // Driver name
            Connection con = DriverManager.getConnection(
                    url, username, password);
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(query); // Execute query
            ResultSetMetaData meta = rs.getMetaData();
            
            String columnName;
            Object value;
            while (rs.next()) {
                Reportable reportable = new Reportable();

                for (int colidx = 1 ; colidx <= meta.getColumnCount(); colidx++) {
                    columnName = meta.getColumnName(colidx);
                    value = rs.getObject(colidx);
                    reportable.set(columnName.toLowerCase(), value);
                }
                data.add(reportable);
            }

            st.close(); // close statement
            con.close(); // close connection
        } catch (Exception e) {
            e.printStackTrace();
        }

        return data;
    }

}
