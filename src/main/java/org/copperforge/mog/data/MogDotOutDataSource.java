package org.copperforge.mog.data;

import java.util.ArrayList;
import java.util.List;

import org.copperforge.mog.MogException;
import org.copperforge.mog.data.filter.MogDataFilter;
import org.copperforge.mog.data.filter.MogQueryFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gwt.conversion.dotout.DOFileFormat;
import com.gwt.conversion.dotout.DOParser;
import com.gwt.conversion.dotout.DOReader;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;

public class MogDotOutDataSource extends MogDataSource {

    private String file;

    private String format;

    private Logger log = LoggerFactory.getLogger(MogDotOutDataSource.class);

    @Override
    public List<? extends MogFetchable> fetch(MogDataFilter filter) throws MogException {
        List<MogDotOutFetchable> fetchables = new ArrayList<>();

        try {
            Expression where = null;
            if (filter != null && filter instanceof MogQueryFilter) {
                MogQueryFilter queryFilter = (MogQueryFilter) filter;
                where = (Expression) CCJSqlParserUtil.parseExpression(queryFilter.getQuery(), true);
            }

            MogDotOutFetchable fetchable;
            DOParser parser = new DOParser();
            DOFileFormat doFormat = parser.parse(format);
            log.debug("format = " + doFormat);
            DOReader reader = new DOReader(file, doFormat);
            log.debug("Reader count = " + reader.count());
            DotOutExpressionVisitorAdaptor dotOutVisitor = new DotOutExpressionVisitorAdaptor();
            while (reader.read()) {
                fetchable = new MogDotOutFetchable(reader.record());
                log.debug("Fetched = " + fetchable + " from " + reader.record());
                if (where != null) {
                    where.accept(dotOutVisitor, fetchable);
                    if (dotOutVisitor.accepted())
                        fetchables.add(fetchable);
                } else {
                    fetchables.add(fetchable);
                }
            }

        } catch (Exception e) {
            throw new MogException("Unable to fetch data for " + getName(), e);
        }

        log.debug("fetchables = " + fetchables.size());
        return fetchables;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

}
