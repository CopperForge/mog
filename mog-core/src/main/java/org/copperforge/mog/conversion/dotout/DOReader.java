package org.copperforge.mog.conversion.dotout;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DOReader {

    private Logger log = LoggerFactory.getLogger(DOReader.class);

    private final RandomAccessFile file;
    private final String filename;
    private final DOFileFormat format;
    private final byte buf[];
    private long offset = 0;
    private final HashMap<String, DOValue<?>> record = new HashMap<>();

    public DOReader(String inFile, DOFileFormat format) throws DOException {
        this.filename = inFile;
        this.format = format;
        this.buf = new byte[format.length()];
        try {
            file = new RandomAccessFile(this.filename, "r");
        } catch (IOException e) {
            throw new DOException(e);
        }
    }

    public boolean read() throws DOException {
        boolean success = true;
        try {
            log.debug(
                    "offset = " + offset + ", file.length = " + file.length() + ", format.length = " + format.length());
            if (offset >= file.length())
                return false;

            file.seek(offset);
            file.read(buf);
            log.debug("buf = " + buf.length);
            parseBuffer();
            this.offset += format.length();
        } catch (IOException e) {
            throw new DOException(e);
        }
        return success;
    }

    public byte[] buffer() {
        return buf;
    }

    public long offset() {
        return offset;
    }

    public HashMap<String, DOValue<?>> record() {
        return record;
    }

    public long count() throws Exception {
        return file.length() / format.length();
    }

    private HashMap<String, DOValue<?>> parseBuffer() throws DOException {
        record.clear();
        byte[] bytes;
        char nullableInd;

        for (DOField field : format.getFields()) {
            nullableInd = 'N';
            log.debug("Parsing :: " + field);
            if (field.getNullable()) {
                nullableInd = (char) Arrays.copyOfRange(buf, 0, 1)[0];
                log.debug("Is value null? " + nullableInd);
                bytes = Arrays.copyOfRange(buf, field.getStartPos() + 1, field.getStartPos() + field.getLength() + 1);
            } else {
                bytes = Arrays.copyOfRange(buf, field.getStartPos(), field.getStartPos() + field.getLength());
            }

            DOValue<?> value = null;
            switch (field.getType()) {
                case DOFieldType.SMALLINT:
                case DOFieldType.TINYINT:
                    value = new DOShort(field, bytes);
                    break;

                case DOFieldType.BIGINT:
                    value = new DOLong(field, bytes);
                    break;

                case DOFieldType.INTEGER:
                    value = new DOInteger(field, bytes);
                    break;

                case DOFieldType.DOUBLE:
                case DOFieldType.DECIMAL:
                case DOFieldType.FLOAT:
                    value = new DODouble(field, bytes);
                    break;

                case DOFieldType.VARCHAR:
                case DOFieldType.NVARCHAR:
                    value = new DOVarchar(field, bytes);
                    break;

                case DOFieldType.CHAR:
                case DOFieldType.CLOB:
                case DOFieldType.NCHAR:
                case DOFieldType.RAW:
                case DOFieldType.WCHAR:
                    value = new DOString(field, bytes);
                    break;

                case DOFieldType.DATE:
                case DOFieldType.DATETIME:
                case DOFieldType.TIME:
                case DOFieldType.TIMESTAMP:
                case DOFieldType.BIT:
                    throw new DOException(field.getType() + " parser not yet implemented");

                case DOFieldType.UNKNOWN:
                default:
                    throw new DOException("Unknown field type");
            }

            log.debug("parsed value :: " + value);
            if (nullableInd == 'Y') value.setNullValue(true);
            if (value != null) record.put(field.getName(), value);

        }
        return record;
    }

}
