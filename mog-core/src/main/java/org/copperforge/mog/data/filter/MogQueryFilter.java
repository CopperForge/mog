package org.copperforge.mog.data.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.copperforge.mog.MogException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MogQueryFilter extends MogDataFilter {

    private String query;

    private PlainSelect statement;

    private Expression where;

    private List<String> columns = new ArrayList<>(Arrays.asList("*"));

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((query == null) ? 0 : query.hashCode());
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
        MogQueryFilter other = (MogQueryFilter) obj;
        if (query == null) {
            if (other.query != null)
                return false;
        } else if (!query.equals(other.query))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "MogQueryFilter [query=" + query + "]";
    }

    public MogQueryFilter() {
    }

    public MogQueryFilter(String query) throws MogException {
        setQuery(query);
    }

    public String getQuery() {
        return query;
    }

    public Expression where() {
        return where;
    }

    public Statement statement() {
        return statement;
    }

    public List<String> columns() {
        return columns;
    }

    public void setQuery(String query) throws MogException {
        try {
            this.query = query;
            try {
                where = CCJSqlParserUtil.parseExpression(query);
            } catch (JSQLParserException e) {
                // not a where, check for select
                statement = (PlainSelect) CCJSqlParserUtil.parse(query);
                where = statement.getWhere();
                columns = statement.getSelectItems().stream().map(s -> s.toString())
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            throw new MogException("Unable to parse query", e);
        }

    }
}
