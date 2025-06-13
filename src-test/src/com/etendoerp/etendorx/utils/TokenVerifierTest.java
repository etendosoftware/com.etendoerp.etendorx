package com.etendoerp.etendorx.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.exception.OBException;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.JWTVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TokenVerifier.
 */
@ExtendWith(MockitoExtension.class)
class TokenVerifierTest {

  private MockedStatic<Algorithm> algorithmStatic;
  private MockedStatic<JWT> jwtStatic;

  @AfterEach
  void tearDown() {
    if (algorithmStatic != null) {
      algorithmStatic.close();
    }
    if (jwtStatic != null) {
      jwtStatic.close();
    }
  }

  /**
   * Scenario: Valid JWT token and matching secret → returns subject.
   */
  @Test
  void testIsValid_withValidToken_returnsSubject() {
    String dummyToken = "header.payload.signature";
    String secret = "superSecret";
    String expectedSubject = "user123";

    // 1) Mock Algorithm.HMAC256(secret) → return a mock Algorithm
    Algorithm algorithmMock = mock(Algorithm.class);
    algorithmStatic = mockStatic(Algorithm.class);
    algorithmStatic.when(() -> Algorithm.HMAC256(secret)).thenReturn(algorithmMock);

    // 2) Mock JWT.require(algorithmMock) → return a mock Verification-like builder
    JWTVerifier.BaseVerification verificationMock = mock(JWTVerifier.BaseVerification.class);
    jwtStatic = mockStatic(JWT.class);
    jwtStatic.when(() -> JWT.require(algorithmMock)).thenReturn(verificationMock);

    // 3) Mock verificationMock.build() → return a mock JWTVerifier
    JWTVerifier verifierMock = mock(JWTVerifier.class);
    when(verificationMock.build()).thenReturn(verifierMock);

    // 4) Mock verifierMock.verify(dummyToken) → return a mock DecodedJWT
    DecodedJWT decodedJwtMock = mock(DecodedJWT.class);
    when(verifierMock.verify(dummyToken)).thenReturn(decodedJwtMock);
    when(decodedJwtMock.getSubject()).thenReturn(expectedSubject);

    // 5) Invoke isValid()
    String actualSubject = TokenVerifier.isValid(dummyToken, secret);

    // 6) Assert subject is returned
    assertEquals(expectedSubject, actualSubject);

    // 7) Verify static calls
    algorithmStatic.verify(() -> Algorithm.HMAC256(secret));
    jwtStatic.verify(() -> JWT.require(algorithmMock));
    verify(verificationMock).build();
    verify(verifierMock).verify(dummyToken);
    verify(decodedJwtMock).getSubject();
  }

  /**
   * Scenario: JWT verification throws an exception → isValid throws OBException.
   */
  @Test
  void testIsValid_withInvalidToken_throwsOBException() {
    String dummyToken = "invalid.token";
    String secret = "badSecret";

    // 1) Mock Algorithm.HMAC256(secret)
    Algorithm algorithmMock = mock(Algorithm.class);
    algorithmStatic = mockStatic(Algorithm.class);
    algorithmStatic.when(() -> Algorithm.HMAC256(secret)).thenReturn(algorithmMock);

    // 2) Mock JWT.require(algorithmMock)
    JWTVerifier.BaseVerification verificationMock = mock(JWTVerifier.BaseVerification.class);
    jwtStatic = mockStatic(JWT.class);
    jwtStatic.when(() -> JWT.require(algorithmMock)).thenReturn(verificationMock);

    // 3) Mock verificationMock.build() → return a mock JWTVerifier
    JWTVerifier verifierMock = mock(JWTVerifier.class);
    when(verificationMock.build()).thenReturn(verifierMock);

    // 4) Mock verifierMock.verify(dummyToken) → throw a RuntimeException
    when(verifierMock.verify(dummyToken)).thenThrow(new RuntimeException("signature mismatch"));

    // 5) Invoke isValid() and expect OBException
    OBException ex = assertThrows(OBException.class, () -> {
      TokenVerifier.isValid(dummyToken, secret);
    });
    assertEquals("Token is not valid", ex.getMessage());

    // 6) Verify static calls
    algorithmStatic.verify(() -> Algorithm.HMAC256(secret));
    jwtStatic.verify(() -> JWT.require(algorithmMock));
    verify(verificationMock).build();
    verify(verifierMock).verify(dummyToken);
  }

  /**
   * Scenario: Attempting to instantiate TokenVerifier via reflection → IllegalStateException.
   */
  @Test
  void testConstructor_throwsIllegalStateException() throws Exception {
    Constructor<TokenVerifier> constructor = TokenVerifier.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    InvocationTargetException thrown = assertThrows(InvocationTargetException.class, () -> {
      constructor.newInstance();
    });
    // The cause of the InvocationTargetException should be IllegalStateException with message "Utility class"
    assertTrue(thrown.getCause() instanceof IllegalStateException);
    assertEquals("Utility class", thrown.getCause().getMessage());
  }
}
