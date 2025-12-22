package com.etendoerp.etendorx.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.interfaces.RSAPublicKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openbravo.base.exception.OBException;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;

class JwkRSAKeyProviderTest {

  private JwkProvider jwkProvider;
  private String keyId = "test-key-id";
  private JwkRSAKeyProvider provider;

  @BeforeEach
  void setUp() {
    jwkProvider = mock(JwkProvider.class);
    provider = new JwkRSAKeyProvider(jwkProvider, keyId);
  }

  @Test
  void testGetPublicKeyByIdSuccess() throws Exception {
    Jwk jwk = mock(Jwk.class);
    RSAPublicKey publicKey = mock(RSAPublicKey.class);
    when(jwkProvider.get(keyId)).thenReturn(jwk);
    when(jwk.getPublicKey()).thenReturn(publicKey);

    RSAPublicKey result = provider.getPublicKeyById(keyId);

    assertEquals(publicKey, result);
  }

  @Test
  void testGetPublicKeyByIdException() throws Exception {
    when(jwkProvider.get(keyId)).thenThrow(new RuntimeException("Error"));

    assertThrows(OBException.class, () -> provider.getPublicKeyById(keyId));
  }

  @Test
  void testGetPrivateKey() {
    assertNull(provider.getPrivateKey());
  }

  @Test
  void testGetPrivateKeyId() {
    assertNull(provider.getPrivateKeyId());
  }

  @Test
  void testGetKeyId() {
    assertEquals(keyId, provider.getKeyId());
  }
}
