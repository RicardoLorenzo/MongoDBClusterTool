/*
 * Copyright 2014 Ricardo Lorenzo<unshakablespirit@gmail.com>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package utils.security;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

/**
 * Created by ricardolorenzo on 25/07/2014.
 */
public class SSHKey implements Serializable {
    private static final long serialVersionUID = 7810402010013708977L;
    public static final String PRIVATE_PKCS8_MARKER = "-----BEGIN PRIVATE KEY-----";
    private KeyPair keyPair;

    protected SSHKey(KeyPair keyPair) {
        if(KeyPair.class.isInstance(keyPair)) {
            this.keyPair = KeyPair.class.cast(keyPair);
        }
    }

    protected PublicKey getPublicKey() {
        return this.keyPair.getPublic();
    }

    protected PrivateKey getPrivateKey() {
        return this.keyPair.getPrivate();
    }

    public KeyPair getKeyPair() {
        return this.keyPair;
    }

    public String getSSHPublicKey(String user) {
        StringBuilder sb = new StringBuilder();
        if(user != null && !user.isEmpty() && user.contains("@")) {
            sb.append(user.substring(0, user.indexOf("@")));
        } else {
            sb.append(user==null?"root":user);
        }
        sb.append(":ssh-rsa ");
        sb.append(new String(Base64.getEncoder().encode(encodePublicKey(this.keyPair))));
        if(user != null && !user.isEmpty()) {
            sb.append(" ");
            sb.append(user);
        }
        return sb.toString();
    }

    public String getSSHPrivateKey() {
        StringBuilder sb = new StringBuilder();
        byte[] privateKeyBytes = new PKCS8EncodedKeySpec(encodePrivateKey(this.keyPair)).getEncoded();
        String privateKey = new String(Base64.getEncoder().encode(privateKeyBytes));
        String ls = System.getProperty("line.separator");
        sb.append(PRIVATE_PKCS8_MARKER).append(ls);
        sb.append(Joiner.on(ls).join(Splitter.fixedLength(64).split(privateKey))).append(ls);
        sb.append(PRIVATE_PKCS8_MARKER.replace("BEGIN", "END")).append(ls);
        return sb.toString();
    }

    private static byte[] encodePrivateKey(KeyPair keyPair) {
        return keyPair.getPrivate().getEncoded();
    }

    private static byte[] encodePublicKey(KeyPair keyPair) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            RSAPublicKeySpec publicKey = factory.getKeySpec(keyPair.getPublic(), RSAPublicKeySpec.class);
            BigInteger publicExponent = publicKey.getPublicExponent();
            BigInteger modulus = publicKey.getModulus();
            writeLengthFirst("ssh-rsa".getBytes(), out);
            writeLengthFirst(publicExponent.toByteArray(), out);
            writeLengthFirst(modulus.toByteArray(), out);
            return out.toByteArray();
        } catch(InvalidKeySpecException e) {
            return new byte[0];
        } catch(IOException e) {
            return new byte[0];
        } catch(NoSuchAlgorithmException e) {
            return new byte[0];
        }
    }

    private static void writeLengthFirst(byte[] array, ByteArrayOutputStream out) throws IOException {
        out.write((array.length >>> 24) & 0xFF);
        out.write((array.length >>> 16) & 0xFF);
        out.write((array.length >>> 8) & 0xFF);
        out.write((array.length >>> 0) & 0xFF);
        if(array.length == 1 && array[0] == (byte) 0x00) {
            out.write(new byte[0]);
        } else {
            out.write(array);
        }
    }
}
