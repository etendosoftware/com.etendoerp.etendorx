package com.etendoerp.etendorx.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption utility for OAuth tokens stored at rest.
 * Requires property {@code etrx.token.encryption.key} in Openbravo.properties:
 * a 64-character hex string (32 bytes).
 *
 * Generate with: openssl rand -hex 32
 */
public class TokenEncryptionUtil {

  private static final Logger LOG = LogManager.getLogger(TokenEncryptionUtil.class);
  private static final String ALGORITHM = "AES/GCM/NoPadding";
  private static final int IV_LENGTH = 12;
  private static final int TAG_LENGTH_BITS = 128;
  private static final String PROPERTY_KEY = "etrx.token.encryption.key";

  private TokenEncryptionUtil() {
    throw new IllegalStateException("Utility class");
  }

  private static byte[] getKey() {
    String hexKey = OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty(PROPERTY_KEY);
    if (hexKey == null || hexKey.trim().length() != 64) {
      throw new OBException("[TokenEncryptionUtil] " + PROPERTY_KEY + " must be a 64-char hex string in Openbravo.properties");
    }
    hexKey = hexKey.trim();
    byte[] key = new byte[32];
    for (int i = 0; i < 32; i++) {
      key[i] = (byte) Integer.parseInt(hexKey.substring(i * 2, i * 2 + 2), 16);
    }
    return key;
  }

  /**
   * Encrypts a plaintext string. Output format: base64(iv):base64(ciphertext+authTag)
   */
  public static String encrypt(String plaintext) {
    try {
      byte[] key = getKey();
      byte[] iv = new byte[IV_LENGTH];
      new SecureRandom().nextBytes(iv);
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
      byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(ciphertext);
    } catch (Exception e) {
      LOG.error("[TokenEncryptionUtil] Encryption failed", e);
      throw new OBException("Token encryption failed", e);
    }
  }

  /**
   * Decrypts a string produced by encrypt(). Returns null on failure.
   */
  public static String decrypt(String ciphertext) {
    try {
      String[] parts = ciphertext.split(":", 2);
      if (parts.length != 2) {
        return null;
      }
      byte[] key = getKey();
      byte[] iv = Base64.getDecoder().decode(parts[0]);
      byte[] ct = Base64.getDecoder().decode(parts[1]);
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
      return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
    } catch (Exception e) {
      LOG.warn("[TokenEncryptionUtil] Decryption failed: {}", e.getMessage());
      return null;
    }
  }
}
