package org.copperforge.mog.conversion.dotout;

import java.nio.ByteBuffer;

import org.apache.commons.lang3.ArrayUtils;

public class DOShort extends DOValue<Short> {

    public DOShort(DOField field, byte[] bytes) {
        super(field, bytes);

        ArrayUtils.reverse(bytes);
        ByteBuffer bytebuff = ByteBuffer.wrap(bytes);
        value = (short) (bytebuff.getShort() & 0xffff);
    }

}
