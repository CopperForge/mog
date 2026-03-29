package org.copperforge.mog.data.dotout;

import java.util.ArrayList;
import java.util.List;

import org.copperforge.mog.MogException;
import org.copperforge.mog.conversion.dotout.DOFileFormat;
import org.copperforge.mog.conversion.dotout.DOParser;
import org.copperforge.mog.conversion.dotout.DOReader;
import org.copperforge.mog.data.MogFetchable;
import org.copperforge.mog.data.MogFileDataSource;
import org.copperforge.mog.data.filter.MogDataFilter;
import org.copperforge.mog.data.filter.MogQueryFilter;
import org.copperforge.mog.runtime.MogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.jsqlparser.expression.Expression;

public class MogDotOutDataSource extends MogFileDataSource {

    private String format;

    private Logger log = LoggerFactory.getLogger(MogDotOutDataSource.class);

    @Override
    public List<? extends MogFetchable> fetch(MogDataFilter filter, MogContext context) throws MogException {
        List<MogDotOutFetchable> fetchables = new ArrayList<>();

        try {
            Expression where = null;
            if (filter != null) {
                MogQueryFilter queryFilter = requireFilter(filter, MogQueryFilter.class, "query");
                where = queryFilter.where();
            }

            MogDotOutFetchable fetchable;
            DOParser parser = new DOParser();
            DOFileFormat doFormat = parser.parse(format);
            log.debug("format = " + doFormat);
            DOReader reader = new DOReader(getFile(), doFormat);
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

        } catch (MogException e) {
            throw e;
        } catch (Exception e) {
            throw new MogException("Unable to fetch data for " + getName(), e);
        }

        log.debug("fetchables = " + fetchables.size());
        return fetchables;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    public String toString() {
        return "MogDotOutDataSource [format=" + format + ", log=" + log + ", getFile()=" + getFile() + ", getName()="
                + getName() + ", getType()=" + getType() + "]";
    }

}
