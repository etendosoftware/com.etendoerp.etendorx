package com.etendoerp.etendorx.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.JWTVerifier;
import org.openbravo.base.exception.OBException;

public class TokenVerifier {
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
