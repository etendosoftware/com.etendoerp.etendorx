package com.etendoerp.etendorx.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.JWTVerifier;
import org.openbravo.base.exception.OBException;

/**
 * Utility class for verifying and validating JWT tokens.
 */
public class TokenVerifier {

  /**
   * Private constructor to prevent instantiation of the utility class.
   * This ensures that the class cannot be instantiated and is used only in a static context.
   *
   * @throws IllegalStateException always thrown to indicate the class is a utility class
   */
  private TokenVerifier() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Validates the provided JWT token using the given secret.
   *
   * @param token  the JWT token to be validated
   * @param secret the secret key used to validate the token
   * @return the subject (sub) claim from the token if it is valid
   * @throws OBException if the token is invalid or an error occurs during validation
   */
  public static String isValid(String token, String secret) {
    try {
      Algorithm algorithm = Algorithm.HMAC256(secret);
      JWTVerifier verifier = JWT.require(algorithm).build();
      DecodedJWT jwt = verifier.verify(token);
      return jwt.getSubject();
    } catch (Exception e) {
      throw new OBException("Token is not valid");
    }
  }
}
