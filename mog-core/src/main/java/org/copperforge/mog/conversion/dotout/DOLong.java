package org.copperforge.mog.conversion.dotout;

import java.nio.ByteBuffer;

import org.apache.commons.lang3.ArrayUtils;

public class DOLong extends DOValue<Long> {

    public static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(x);
        return buffer.array();
    }
    
    public DOLong(DOField field, byte[] bytes) {
        super(field, bytes);
        ArrayUtils.reverse(bytes);
        ByteBuffer bytebuff = ByteBuffer.wrap(bytes);
        value = bytebuff.getLong() & 0xffffffffffffffffL;
    }

    public DOLong(Long value) {
        super(null, longToBytes(value));
        this.value = value;
    }

}
