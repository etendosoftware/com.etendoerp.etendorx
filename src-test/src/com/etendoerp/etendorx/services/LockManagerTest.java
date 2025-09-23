package com.etendoerp.etendorx.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

/**
 * Unit tests for LockManager.
 */
public class LockManagerTest {

  /**
   * Verifies that acquiring a lease for a single id increases the active
   * lock wrapper count while held and that releasing the lease returns the
   * registry to the previous count.
   */
  @Test
  public void testAcquireAndReleaseSingleThread() {
    int before = LockManager.getActiveLockCount();
    try (LockManager.LockLease lease = LockManager.lock("single-id")) {
      // while lease is active, there must be at least one active wrapper
      assertTrue(LockManager.getActiveLockCount() >= before + 1);
    }
    // after closing, the active locks should return to previous count
    assertEquals(before, LockManager.getActiveLockCount());
  }

  /**
   * Ensures exclusive access when multiple threads attempt to lock the same
   * id concurrently. This test verifies no two threads enter the critical
   * section at the same time and that all tasks complete and the registry is
   * cleaned up afterwards.
   */
  @Test
  public void testConcurrentExclusiveAccess() throws InterruptedException {
    final String lockId = "concurrent-id";
    final int threads = 50;
    ExecutorService ex = Executors.newFixedThreadPool(16);
    AtomicInteger counter = new AtomicInteger(0);
    AtomicInteger active = new AtomicInteger(0);
    AtomicBoolean violation = new AtomicBoolean(false);

    for (int i = 0; i < threads; i++) {
      ex.submit(() -> {
        try (LockManager.LockLease lease = LockManager.lock(lockId)) {
          // only one thread should be active in the critical section
          if (!active.compareAndSet(0, 1)) {
            violation.set(true);
          }
          // simulate work
          try {
            Thread.sleep(5);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          counter.incrementAndGet();
          if (!active.compareAndSet(1, 0)) {
            violation.set(true);
          }
        }
      });
    }

    ex.shutdown();
    boolean finished = ex.awaitTermination(30, TimeUnit.SECONDS);
    assertTrue("Executor did not finish in time", finished);
    assertFalse("Detected concurrent access to critical section", violation.get());
    assertEquals("All tasks should have incremented the counter", threads, counter.get());
    // after all leases are closed, there should be no active wrappers for that id
    assertEquals(0, LockManager.getActiveLockCount());
  }

  /**
   * Acquires and releases locks for many distinct ids concurrently and
   * verifies that the internal lock registry is cleaned up (no active
   * wrappers remain) after all leases are released.
   */
  @Test
  public void testMultipleIdsCleanup() throws InterruptedException {
    final int ids = 200;
    ExecutorService ex = Executors.newFixedThreadPool(32);
    for (int i = 0; i < ids; i++) {
      final String id = "id-" + i;
      ex.submit(() -> {
        try (LockManager.LockLease lease = LockManager.lock(id)) {
          // quick hold
        }
      });
    }
    ex.shutdown();
    boolean finished = ex.awaitTermination(30, TimeUnit.SECONDS);
    assertTrue("Executor did not finish in time", finished);
    // all acquired and released; map should be cleaned up
    assertEquals(0, LockManager.getActiveLockCount());
  }
}
