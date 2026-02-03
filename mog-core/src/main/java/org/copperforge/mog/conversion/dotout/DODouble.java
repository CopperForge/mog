package org.copperforge.mog.conversion.dotout;

import java.nio.ByteBuffer;

import org.apache.commons.lang3.ArrayUtils;

public class DODouble extends DOValue<Double> {

    public DODouble(DOField field, byte[] bytes) {
        super(field, bytes);

        ArrayUtils.reverse(bytes);
        ByteBuffer bytebuff = ByteBuffer.wrap(bytes);
        value = bytebuff.getDouble();
    }

}
