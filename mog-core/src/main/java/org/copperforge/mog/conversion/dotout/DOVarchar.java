package org.copperforge.mog.conversion.dotout;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;

public class DOVarchar extends DOString {


    public DOVarchar(DOField field, byte[] bytes) {
        super(field, bytes);
        
        log.debug("Parsing varchar field " + field + ", bytes = " + Hex.encodeHexString(bytes));
        byte[] lengthBytes = Arrays.copyOf(bytes, 2);
        byte[] stringBytes = Arrays.copyOfRange(bytes, 2, bytes.length);
        log.debug("Length bytes = " + Hex.encodeHexString(lengthBytes) + ", data bytes = " + Hex.encodeHexString(stringBytes));

        ArrayUtils.reverse(lengthBytes);
        ByteBuffer wrapped = ByteBuffer.wrap(lengthBytes);
        length = wrapped.getShort();
        log.debug("Determined length as " + length);
        
        value = new String(stringBytes);
    }

    private int length;

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

}
