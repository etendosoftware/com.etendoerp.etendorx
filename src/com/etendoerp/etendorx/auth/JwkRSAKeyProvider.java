package com.etendoerp.etendorx.auth;

import com.auth0.jwt.interfaces.RSAKeyProvider;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;

import java.security.interfaces.RSAPublicKey;
import java.security.interfaces.RSAPrivateKey;

/**
 * Implementation of the {@link RSAKeyProvider} interface that retrieves RSA public keys
 * from a {@link JwkProvider} using a specified key ID.
 *
 * <p>This class is intended to be used when validating JWTs signed with RSA algorithms (e.g., RS256),
 * where the public key is retrieved dynamically from a JWK endpoint.</p>
 *
 * <p>Note: This implementation does not provide a private key, as it's intended only for
 * signature verification, not for signing tokens.</p>
 */
public class JwkRSAKeyProvider implements RSAKeyProvider {

  private final JwkProvider jwkProvider;
  private final String keyId;

  /**
   * Constructs a new {@code JwkRSAKeyProvider}.
   *
   * @param jwkProvider the {@link JwkProvider} used to retrieve keys
   * @param keyId       the ID of the key to be used
   */
  public JwkRSAKeyProvider(JwkProvider jwkProvider, String keyId) {
    this.jwkProvider = jwkProvider;
    this.keyId = keyId;
  }

  /**
   * Retrieves the RSA public key corresponding to the given key ID from the {@link JwkProvider}.
   *
   * @param keyId the ID of the key to retrieve
   * @return the {@link RSAPublicKey} associated with the given key ID
   * @throws RuntimeException if the key cannot be retrieved or cast
   */
  @Override
  public RSAPublicKey getPublicKeyById(String keyId) {
    try {
      Jwk jwk = jwkProvider.get(keyId);
      return (RSAPublicKey) jwk.getPublicKey();
    } catch (Exception e) {
      throw new RuntimeException("Error getting JWK public key", e);
    }
  }

  /**
   * This implementation does not provide a private key.
   *
   * @return {@code null}
   */
  @Override
  public RSAPrivateKey getPrivateKey() {
    return null;
  }

  /**
   * This implementation does not provide a private key ID.
   *
   * @return {@code null}
   */
  @Override
  public String getPrivateKeyId() {
    return null;
  }

  /**
   * Returns the configured key ID associated with this provider.
   *
   * @return the key ID
   */
  public String getKeyId() {
    return keyId;
  }
}
