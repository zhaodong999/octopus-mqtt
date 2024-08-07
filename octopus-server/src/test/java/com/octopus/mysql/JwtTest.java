package com.octopus.mysql;

import com.auth0.jwt.HeaderParams;
import com.auth0.jwt.JWT;
import com.auth0.jwt.RegisteredClaims;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.ECDSAKeyProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import sun.security.ec.ECPrivateKeyImpl;
import sun.security.ec.ECPublicKeyImpl;

import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;


class JwtTest {

    @Test
    void verify() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        Map<String,Object> map = new HashMap<>();
        map.put(HeaderParams.ALGORITHM, "ES256");
        map.put(HeaderParams.TYPE, "JWT");

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1");
        keyGen.initialize(ecSpec);
        KeyPair keyPair = keyGen.generateKeyPair();
        Algorithm algorithm = Algorithm.ECDSA256(new ECDSAKeyProvider() {
            @Override
            public ECPublicKey getPublicKeyById(String keyId) {
                try {
                    Base64.Encoder encoder = Base64.getEncoder();
                    System.out.println(encoder.encodeToString(keyPair.getPublic().getEncoded()));
                    return new ECPublicKeyImpl(keyPair.getPublic().getEncoded());
                } catch (InvalidKeyException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public ECPrivateKey getPrivateKey() {
                try {
                    return new ECPrivateKeyImpl(keyPair.getPrivate().getEncoded());
                } catch (InvalidKeyException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public String getPrivateKeyId() {
                return null;
            }
        });

        String token = JWT.create().withHeader(map)
                .withClaim("name", "test")
                .withClaim(RegisteredClaims.ISSUER, "appKey")
                .withClaim(RegisteredClaims.EXPIRES_AT, System.currentTimeMillis() + 1000 * 60 * 60 * 24)
                .sign(algorithm);

        System.out.println(token);
        DecodedJWT verify = JWT.require(algorithm).build().verify(token);
        Assertions.assertEquals("test", verify.getClaim("name").asString());
    }
}
