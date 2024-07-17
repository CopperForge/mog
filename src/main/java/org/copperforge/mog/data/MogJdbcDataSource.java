package org.copperforge.mog.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.copperforge.mog.MogException;
import org.copperforge.mog.MogServiceManager;
import org.copperforge.mog.var.MogVariableService;
import org.copperforge.mog.var.VariableService;

public class MogJdbcDataSource extends MogDataSource {

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
        return "MogJdbcDataSource [url=" + url + ", user=" + user + ", password=*, query=" + query
                + ", jdbcClass=" + jdbcClass + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        result = prime * result + ((user == null) ? 0 : user.hashCode());
        result = prime * result + ((password == null) ? 0 : password.hashCode());
        result = prime * result + ((query == null) ? 0 : query.hashCode());
        result = prime * result + ((jdbcClass == null) ? 0 : jdbcClass.hashCode());
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
        MogJdbcDataSource other = (MogJdbcDataSource) obj;
        if (url == null) {
            if (other.url != null)
                return false;
        } else if (!url.equals(other.url))
            return false;
        if (user == null) {
            if (other.user != null)
                return false;
        } else if (!user.equals(other.user))
            return false;
        if (password == null) {
            if (other.password != null)
                return false;
        } else if (!password.equals(other.password))
            return false;
        if (query == null) {
            if (other.query != null)
                return false;
        } else if (!query.equals(other.query))
            return false;
        if (jdbcClass == null) {
            if (other.jdbcClass != null)
                return false;
        } else if (!jdbcClass.equals(other.jdbcClass))
            return false;
        return true;
    }

    @Override
    public List<MogFetchable> fetch(int offset, int limit) throws MogException {
        List<MogFetchable> data = new ArrayList<>();
        MogServiceManager manager = MogServiceManager.instance();
        VariableService environmentService = (VariableService) manager.get(MogVariableService.class);
        try {
            String url = environmentService.envsubst(getUrl()); // url
            String username = getUser(); // credentials
            String password = getPassword(); // TODO encryption
            String query = getQuery(); // query to be run
            Class.forName(getJdbcClass()); // Driver name
            Connection con = DriverManager.getConnection(
                    url, username, password);
            Statement st = con.createStatement();
            
            if (offset > 0) {
                query = addOffset(query, offset);
            }

            if (limit > 0) {
                query = addLimit(query, limit);
            }

            ResultSet rs = st.executeQuery(query); // Execute query
            ResultSetMetaData meta = rs.getMetaData();
            
            String columnName;
            Object value;
            while (rs.next()) {
                MogFetchable reportable = new MogFetchable();

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

    private String addLimit(String query, int limit) {
        return query + " FETCH NEXT " + limit + " ROWS ONLY";
    }

    private String addOffset(String query, int offset) {
        return query + " OFFSET " + offset + " ROWS";
    }

  
}
