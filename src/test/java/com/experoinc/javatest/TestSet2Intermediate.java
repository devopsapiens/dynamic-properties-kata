package com.experoinc.javatest;

import java.util.concurrent.Callable;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class TestSet2Intermediate {
  @Test
  public void ReleasesDependenciesNotUsedInSubsequentReads() {
    final DynamicProperty<Integer>[] values = new DynamicProperty[]{
      DynamicPropertyFactory.create(42),
      DynamicPropertyFactory.create(99),
      DynamicPropertyFactory.create(2012)};
    final int[] readCounts = new int[]{0, 0, 0};
    final DynamicProperty<Integer> which = DynamicPropertyFactory.create(0);
    DynamicProperty<Integer> p = DynamicPropertyFactory.create(new Callable<Integer>() {
      @Override
      public Integer call() throws Exception {
        readCounts[which.getValue()]++;
        return values[which.getValue()].getValue();
      }
    }, new Observer<Integer>() {
      @Override
      public void observe(Integer value) {
      }
    });

    Assert.assertArrayEquals(new int[]{1, 0, 0}, readCounts);

    values[1].setValue(100);
    Assert.assertArrayEquals(new int[]{1, 0, 0}, readCounts);

    which.setValue(2);
    Assert.assertArrayEquals(new int[]{1, 0, 1}, readCounts);

    values[0].setValue(10);
    Assert.assertArrayEquals(new int[]{1, 0, 1}, readCounts);
  }

  @Test
  public void CalculatedsEvaluatedOnDifferentThreadsShouldNotCaptureEachOthersDependencies() {
    final DynamicProperty<Integer> o1 = DynamicPropertyFactory.create(100);
    final DynamicProperty<Integer> o2 = DynamicPropertyFactory.create(1000);
    final Phaser barrier = new Phaser();

    final long waitTime = 2L;
    final Boolean[] waitForBarrier = new Boolean[]{true};

    final DynamicProperty<Integer>[] dps = new DynamicProperty[]{null, null};

    Runnable r1 = new Runnable() {
      @Override
      public void run() {
        barrier.register();
        dps[0] = DynamicPropertyFactory.create(new Callable<Integer>() {
          @Override
          public Integer call() throws Exception {
            if (waitForBarrier[0]) {
              try {
                barrier.arrive();
                barrier.awaitAdvanceInterruptibly(0, waitTime, TimeUnit.SECONDS);
              } catch (TimeoutException te) {
                Assert.fail("deadlock occurred");
                throw te;
              }
            }

            Integer result = o1.getValue();

            if (waitForBarrier[0]) {
              try {
                barrier.arrive();
                barrier.awaitAdvanceInterruptibly(1, waitTime, TimeUnit.SECONDS);
              } catch (TimeoutException te) {
                Assert.fail("deadlock occurred");
                throw te;
              }
            }

            return result;
          }
        }, new Observer<Integer>() {
          @Override
          public void observe(Integer value) {
          }
        });
      }
    };

    Runnable r2 = new Runnable() {
      @Override
      public void run() {
        barrier.register();
        dps[1] = DynamicPropertyFactory.create(new Callable<Integer>() {
          @Override
          public Integer call() throws Exception {
            Assert.assertTrue(
              "this calculated should not be evaluated after initialization",
              waitForBarrier[0]);
            try {
              barrier.arrive();
              barrier.awaitAdvanceInterruptibly(0, waitTime, TimeUnit.SECONDS);
            } catch (TimeoutException te) {
              Assert.fail("deadlock occurred");
              throw te;
            }
            Integer result = o2.getValue();
            try {
              barrier.arrive();
              barrier.awaitAdvanceInterruptibly(1, waitTime, TimeUnit.SECONDS);
            } catch (TimeoutException te) {
              Assert.fail("deadlock occurred");
              throw te;
            }
            return result;
          }
        }, new Observer<Integer>() {
          @Override
          public void observe(Integer value) {
          }
        });
      }
    };

    try {
      // Create threads and start them simultaneously
      Thread t1 = new Thread(r1);
      Thread t2 = new Thread(r2);
      t1.start();
      t2.start();

      // Wait for each thread to complete
      t1.join();
      t2.join();
    } catch (InterruptedException ie) {
      Assert.fail();
    }

    Assert.assertTrue(dps[0].getValue() == 100);
    Assert.assertTrue(dps[1].getValue() == 1000);

    waitForBarrier[0] = false;
    o1.setValue(-100);
    Assert.assertTrue(dps[0].getValue() == -100);
    Assert.assertTrue(dps[1].getValue() == 1000);
  }
}
