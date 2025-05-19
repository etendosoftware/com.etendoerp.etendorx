package com.etendoerp.etendorx.auth;

import com.auth0.jwt.interfaces.RSAKeyProvider;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;

import java.security.interfaces.RSAPublicKey;
import java.security.interfaces.RSAPrivateKey;

public class JwkRSAKeyProvider implements RSAKeyProvider {

  private final JwkProvider jwkProvider;
  private final String keyId;

  public JwkRSAKeyProvider(JwkProvider jwkProvider, String keyId) {
    this.jwkProvider = jwkProvider;
    this.keyId = keyId;
  }

  @Override
  public RSAPublicKey getPublicKeyById(String keyId) {
    try {
      Jwk jwk = jwkProvider.get(keyId);
      return (RSAPublicKey) jwk.getPublicKey();
    } catch (Exception e) {
      throw new RuntimeException("Error getting JWK public key", e);
    }
  }

  @Override
  public RSAPrivateKey getPrivateKey() {
    return null; // Only use the public key to verify tokens
  }

  @Override
  public String getPrivateKeyId() {
    return null; // Don't use a private key (HS256)
  }

  public String getKeyId() {
    return keyId;
  }
}
