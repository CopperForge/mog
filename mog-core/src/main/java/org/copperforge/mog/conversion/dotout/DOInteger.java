package org.copperforge.mog.conversion.dotout;

import java.nio.ByteBuffer;

import org.apache.commons.lang3.ArrayUtils;

public class DOInteger extends DOValue<Integer> {

    public DOInteger(DOField field, byte[] bytes) {
        super(field, bytes);

        ArrayUtils.reverse(bytes);
        ByteBuffer bytebuff = ByteBuffer.wrap(bytes);
        value = bytebuff.getInt() & 0xffffffff;
    }

}
