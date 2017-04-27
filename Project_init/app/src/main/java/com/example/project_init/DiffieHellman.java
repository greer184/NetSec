package com.example.project_init;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;

import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;

// Creates mechanism for D-H key exchange
public class DiffieHellman {

    private KeyPair keyPair;
    private KeyAgreement keyAgreement;

    private static final byte PRIME[] = {
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xC9, (byte)0x0F, (byte)0xDA, (byte)0xA2,
            (byte)0x21, (byte)0x68, (byte)0xC2, (byte)0x34,
            (byte)0xC4, (byte)0xC6, (byte)0x62, (byte)0x8B,
            (byte)0x80, (byte)0xDC, (byte)0x1C, (byte)0xD1,
            (byte)0x29, (byte)0x02, (byte)0x4E, (byte)0x08,
            (byte)0x8A, (byte)0x67, (byte)0xCC, (byte)0x74,
            (byte)0x02, (byte)0x0B, (byte)0xBE, (byte)0xA6,
            (byte)0x3B, (byte)0x13, (byte)0x9B, (byte)0x22,
            (byte)0x51, (byte)0x4A, (byte)0x08, (byte)0x79,
            (byte)0x8E, (byte)0x34, (byte)0x04, (byte)0xDD,
            (byte)0xEF, (byte)0x95, (byte)0x19, (byte)0xB3,
            (byte)0xCD, (byte)0x3A, (byte)0x43, (byte)0x1B,
            (byte)0x30, (byte)0x2B, (byte)0x0A, (byte)0x6D,
            (byte)0xF2, (byte)0x5F, (byte)0x14, (byte)0x37,
            (byte)0x4F, (byte)0xE1, (byte)0x35, (byte)0x6D,
            (byte)0x6D, (byte)0x51, (byte)0xC2, (byte)0x45,
            (byte)0xE4, (byte)0x85, (byte)0xB5, (byte)0x76,
            (byte)0x62, (byte)0x5E, (byte)0x7E, (byte)0xC6,
            (byte)0xF4, (byte)0x4C, (byte)0x42, (byte)0xE9,
            (byte)0xA6, (byte)0x37, (byte)0xED, (byte)0x6B,
            (byte)0x0B, (byte)0xFF, (byte)0x5C, (byte)0xB6,
            (byte)0xF4, (byte)0x06, (byte)0xB7, (byte)0xED,
            (byte)0xEE, (byte)0x38, (byte)0x6B, (byte)0xFB,
            (byte)0x5A, (byte)0x89, (byte)0x9F, (byte)0xA5,
            (byte)0xAE, (byte)0x9F, (byte)0x24, (byte)0x11,
            (byte)0x7C, (byte)0x4B, (byte)0x1F, (byte)0xE6,
            (byte)0x49, (byte)0x28, (byte)0x66, (byte)0x51,
            (byte)0xEC, (byte)0xE4, (byte)0x5B, (byte)0x3D,
            (byte)0xC2, (byte)0x00, (byte)0x7C, (byte)0xB8,
            (byte)0xA1, (byte)0x63, (byte)0xBF, (byte)0x05,
            (byte)0x98, (byte)0xDA, (byte)0x48, (byte)0x36,
            (byte)0x1C, (byte)0x55, (byte)0xD3, (byte)0x9A,
            (byte)0x69, (byte)0x16, (byte)0x3F, (byte)0xA8,
            (byte)0xFD, (byte)0x24, (byte)0xCF, (byte)0x5F,
            (byte)0x83, (byte)0x65, (byte)0x5D, (byte)0x23,
            (byte)0xDC, (byte)0xA3, (byte)0xAD, (byte)0x96,
            (byte)0x1C, (byte)0x62, (byte)0xF3, (byte)0x56,
            (byte)0x20, (byte)0x85, (byte)0x52, (byte)0xBB,
            (byte)0x9E, (byte)0xD5, (byte)0x29, (byte)0x07,
            (byte)0x70, (byte)0x96, (byte)0x96, (byte)0x6D,
            (byte)0x67, (byte)0x0C, (byte)0x35, (byte)0x4E,
            (byte)0x4A, (byte)0xBC, (byte)0x98, (byte)0x04,
            (byte)0xF1, (byte)0x74, (byte)0x6C, (byte)0x08,
            (byte)0xCA, (byte)0x23, (byte)0x73, (byte)0x27,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF
    };

    private static final BigInteger P = new BigInteger(1, PRIME);
    private static final BigInteger G = BigInteger.valueOf(2);

    public byte[] generatePublicKey() {
        DHParameterSpec dhParam;
        byte[] pubKey = null;

        try {
            // Generate key
            dhParam = new DHParameterSpec(P, G);
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DiffieHellman");
            keyGen.initialize(dhParam);

            // Generate key pair
            keyPair = keyGen.generateKeyPair();
            keyAgreement = KeyAgreement.getInstance("DiffieHellman");
            keyAgreement.init(keyPair.getPrivate());

            // Get public key
            BigInteger pBI = ((DHPublicKey) keyPair.getPublic()).getY();
            pubKey = pBI.toByteArray();

        } catch (Exception e) {
        }

        return pubKey;
    }

    public byte[] computeSharedKey(byte[] pubKey, int length){
        byte[] sharedKey = new byte[length / 8];

        try {
            // Convert back to big Integer
            BigInteger pBI = new BigInteger(1, pubKey);

            // Generate shared key
            KeyFactory keyFact = KeyFactory.getInstance("DiffieHellman");
            PublicKey pKey = keyFact.generatePublic(new DHPublicKeySpec(pBI, P, G));
            keyAgreement.doPhase(pKey, true);
            byte[] sharedKeyLong = keyAgreement.generateSecret();
            for (int i = 1; i < sharedKey.length; i++){
                sharedKey[i] = sharedKeyLong[i];
            }

        } catch (Exception e) {

        }
        return sharedKey;
    }
}
