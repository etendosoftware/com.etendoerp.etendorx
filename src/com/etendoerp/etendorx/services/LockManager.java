package com.etendoerp.etendorx.services;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * LockManager standalone implementation that uses a ConcurrentHashMap to
 * keep per-id locks with reference counting to allow automatic cleanup.
 *
 * <p>Usage:</p>
 * <pre>
 * Lock lock = LockManager.acquireLock(id);
 * try {
 * lock.lock();
 * // critical section
 * } finally {
 * LockManager.releaseLock(id, lock);
 * }
 * </pre>
 * <p>Or, for a more convenient and safer approach, use {@link #lock(String)}
 * with a try-with-resources block:</p>
 * <pre>
 * try (LockManager.LockLease lease = LockManager.lock(id)) {
 * // critical section
 * }
 * </pre>
 */
public final class LockManager {

  private static final Logger log = LogManager.getLogger();

  private static final ConcurrentMap<String, LockWrapper> locks = new ConcurrentHashMap<>();

  private LockManager() {
    // utility class
  }

  private static class LockWrapper {
    private final Lock lock = new ReentrantLock(true);
    private final AtomicInteger referenceCount = new AtomicInteger(0);

    Lock getLock() {
      return lock;
    }

    void increment() {
      referenceCount.incrementAndGet();
    }

    int decrementAndGet() {
      return referenceCount.decrementAndGet();
    }
  }

  /**
   * Acquires (or creates) and registers a lock wrapper for the provided id.
   *
   * <p>This method will create a new {@link LockWrapper} if none exists for
   * {@code lockId} and increment its internal reference counter. Callers must
   * pair this call with {@link #releaseLock(String, Lock)} (or use
   * {@link #lock(String)} / {@link LockLease}) to ensure the wrapper is
   * eventually removed from the internal registry.</p>
   *
   * @param lockId the identifier for the resource to lock (non-null, non-empty)
   * @return the {@link Lock} instance associated with the given id
   * @throws IllegalArgumentException when {@code lockId} is null or empty
   */
  public static Lock acquireLock(String lockId) {
    if (lockId == null || lockId.trim().isEmpty()) {
      throw new IllegalArgumentException("lockId cannot be null or empty");
    }
    LockWrapper wrapper = locks.computeIfAbsent(lockId, k -> {
      log.trace("Creating LockWrapper for id={}", k);
      return new LockWrapper();
    });
    wrapper.increment();
    return wrapper.getLock();
  }

  /**
   * Releases the provided {@link Lock} previously returned from
   * {@link #acquireLock(String)} and decrements the internal reference counter.
   * If the counter reaches zero the wrapper is removed from the internal map.
   *
   * <p>Note: this method will attempt to {@code unlock()} the provided lock.
   * If the current thread does not hold the lock, an error is logged and the
   * wrapper is left in the registry to avoid breaking mutual exclusion.</p>
   *
   * @param lockId the identifier used to acquire the lock
   * @param lock the {@link Lock} instance previously obtained via
   * {@link #acquireLock(String)}
   */
  public static void releaseLock(String lockId, Lock lock) {
    if (lockId == null || lock == null) {
      return;
    }
    // Sonar rule S2235: do not catch IllegalMonitorStateException. The
    // LockManager always supplies ReentrantLock instances from the internal
    // LockWrapper; therefore prefer checking ownership via
    // ReentrantLock.isHeldByCurrentThread() before unlocking. If an external
    ///non-ReentrantLock is passed, log and return (do not attempt to catch
    // IllegalMonitorStateException).
    if (!(lock instanceof ReentrantLock)) {
      log.error("Unsupported Lock implementation for id={}: {}", lockId, lock.getClass().getName());
      return;
    }

    ReentrantLock rl = (ReentrantLock) lock;
    if (!rl.isHeldByCurrentThread()) {
      log.error("Attempted to unlock a ReentrantLock not held by current thread for id={}", lockId);
      return;
    }

    rl.unlock();
    LockWrapper wrapper = locks.get(lockId);
    if (wrapper != null && wrapper.decrementAndGet() == 0) {
      locks.remove(lockId, wrapper);
      log.trace("Removed LockWrapper for id={}", lockId);
    }
  }

  /**
   * Returns the number of active lock wrappers currently tracked by this
   * LockManager. Useful for monitoring and tests.
   *
   * @return number of lock wrappers present in the internal registry (>= 0)
   */
  public static int getActiveLockCount() {
    return locks.size();
  }

  /**
   * A lease that holds the lock and releases it when closed.
   * Use with try-with-resources to guarantee release.
   */
  public static final class LockLease implements AutoCloseable {
    private final String id;
    private final Lock lock;
    private boolean closed = false;

    private LockLease(String id, Lock lock) {
      this.id = id;
      this.lock = lock;
    }

    @Override
    public void close() {
      if (!closed) {
        try {
          releaseLock(id, lock);
        } finally {
          closed = true;
          log.trace("LockLease closed for id={}", id);
        }
      }
    }
  }

  /**
   * Convenience helper that acquires and locks the specified id, returning a
   * {@link LockLease} which will release and decrement the reference count on
   * {@link LockLease#close()}.
   *
   * <p>This method is intended to be used with try-with-resources to guarantee
   * release, for example:</p>
   * <pre>
   * try (LockManager.LockLease lease = LockManager.lock(id)) {
   * // critical section
   * }
   * </pre>
   *
   * <p>Note for callers and static analysis tools:</p>
   * <ul>
   * <li>This method acquires the LockWrapper for the given id, increments its
   * reference count and then calls {@code lock()} on the underlying
   * {@link java.util.concurrent.locks.Lock}. The returned {@link LockLease}
   * will release the lock and decrement counts when its {@code close()}
   * method is executed. Therefore, callers must use try-with-resources or
   * otherwise ensure {@code close()} is executed to avoid leaks.</li>
   * <li>Some static analyzers may warn that this method locks without unlocking
   * inside the same method. This is intentional: unlocking happens in
   * {@link LockLease#close()} and is enforced by using try-with-resources.</li>
   * <li>This implementation removes the need for external cache libraries like
   * Caffeine; the lock registry here is JVM-local and uses a
   * {@link java.util.concurrent.ConcurrentHashMap} with reference counting.</li>
   * </ul>
   *
   * @param lockId the identifier for the resource to lock
   * @return a {@link LockLease} that holds the lock and will release it when closed
   */
  public static LockLease lock(String lockId) {
    Lock l = acquireLock(lockId);
    l.lock();
    return new LockLease(lockId, l);
  }
}
