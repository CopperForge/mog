package org.copperforge.mog.security;

import org.copperforge.mog.MogException;

public interface MogDecryptor {

    String decrypt(String value) throws MogException;

}
