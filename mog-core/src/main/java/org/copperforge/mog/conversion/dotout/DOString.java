package org.copperforge.mog.conversion.dotout;

public class DOString extends DOValue<String> {

    public DOString(DOField field, byte[] bytes) {
        super(field, bytes);
        value = new String(bytes);
    }

}
