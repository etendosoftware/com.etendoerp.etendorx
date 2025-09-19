package com.etendoerp.etendorx.services;

import java.time.Duration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Simple LockManager utility that provides a lock object per id.
 *
 * Only exposes a single responsibility: obtain a lock object by id.
 */
public final class LockManager {

  private static final Logger log = LogManager.getLogger();

  private static final Cache<String, Object> locks = Caffeine.newBuilder()
      .expireAfterAccess(Duration.ofMinutes(30))
      .maximumSize(10_000)
      .build();

  private LockManager() {
    // utility class
  }

  /**
   * Returns a lock object for the given id. The returned object is stable
   * for the id while present in the cache; cache eviction will remove unused locks.
   *
   * @param lockId the identifier for the lock
   * @return lock object (never null)
   */
  public static Object getLock(String lockId) {
    if (lockId == null) {
      // defensive: callers normally check for null before synchronizing
      return new Object();
    }
    log.debug("LockManager.getLock id={} currentSize={}", lockId, locks.estimatedSize());
    return locks.get(lockId, k -> new Object());
  }
}
