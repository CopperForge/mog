package org.copperforge.mog.security;

import org.copperforge.mog.MogException;

public interface MogEncryptor {

    String encrypt(String value) throws MogException;
    
}
