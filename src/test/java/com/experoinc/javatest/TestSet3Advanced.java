package com.experoinc.javatest;

import java.util.concurrent.Callable;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class TestSet3Advanced {
  @Test
  public void IfDependencyIsUpdatedDuringEvaluationThenCalculatedIsEvaluatedAgainButNotConcurrently() {
    // Some of these final variables are wrapped in arrays to allow the
    // anonymous classes to access the values AND change them - sort of a
    // hack
    final DynamicProperty<Integer> o = DynamicPropertyFactory.create(100);
    final boolean[] doUpdate = new boolean[]{true};
    final boolean[] evaluating = new boolean[]{false};
    final int[] count = new int[]{0};
    DynamicProperty<Integer> c = DynamicPropertyFactory.create(new Callable<Integer>() {
      @Override
      public Integer call() throws Exception {
        Assert.assertFalse(evaluating[0]);
        evaluating[0] = true;
        ++count[0];

        int result = o.getValue();

        if (doUpdate[0]) {
          doUpdate[0] = false;
          o.setValue(o.getValue() + 1); // read AND update the value.
          // Should trigger another
          // evaluation when this one
          // finishes.
        }

        evaluating[0] = false;
        return result;
      }
    }, new Observer<Integer>() {
      @Override
      public void observe(Integer value) {
      }
    });

    Assert.assertTrue(count[0] == 2);
    Assert.assertTrue(c.getValue() == 101);

    o.setValue(0);
    Assert.assertTrue(count[0] == 3);
    Assert.assertTrue(c.getValue() == 0);

    doUpdate[0] = true;
    o.setValue(-100);
    Assert.assertTrue(count[0] == 5);
    Assert.assertTrue(c.getValue() == -99);
  }

  @Test
  public void IfDependencyIsUpdatedByOtherThreadWhileEvaluatingThenCalculatedIsEvaluatedAgainButNotConcurrently() {
    // Some of these final variables are wrapped in arrays to allow the
    // anonymous classes to access the values AND change them - sort of a
    // hack
    final DynamicProperty<Integer> o = DynamicPropertyFactory.create(100);
    final long waitTime = 2L;
    final Phaser barrier = new Phaser();
    final boolean[] waitForBarrier = new boolean[]{true};
    final boolean[] evaluating = new boolean[]{false};
    final int[] count = new int[]{0};
    final DynamicProperty<Integer>[] c = new DynamicProperty[]{null};

    // Code to be run in a Thread below
    Runnable r1 = new Runnable() {
      public void run() {
        barrier.register();
        c[0] = DynamicPropertyFactory.create(new Callable<Integer>() {
          @Override
          public Integer call() throws Exception {
            Assert.assertFalse(evaluating[0]);
            evaluating[0] = true;
            ++count[0];
            int result = o.getValue();

            if (waitForBarrier[0]) {
              waitForBarrier[0] = false;
              try {
                barrier.arrive();
                barrier.awaitAdvanceInterruptibly(0, waitTime, TimeUnit.SECONDS);
              } catch (TimeoutException te) {
                Assert.fail();
                throw te;
              }
              Thread.sleep(200); // give the other thread time to
              // do its work
            }

            evaluating[0] = false;
            return result;
          }
        }, new Observer<Integer>() {
          @Override
          public void observe(Integer value) {
          }
        });
      }
    };

    // Code to be run in a second Thread below
    Runnable changeValue = new Runnable() {
      public void run() {
        barrier.register();
        // wait for the computed to start evaluating
        try {
          barrier.arrive();
          barrier.awaitAdvanceInterruptibly(0, waitTime, TimeUnit.SECONDS);
        } catch (Exception te) {
          Assert.fail();
        }

        // Now make our change
        o.setValue(o.getValue() + 1);
      }
    };

    try {
      // Create and run both threads simultaneously
      Thread t1 = new Thread(r1);
      Thread t2 = new Thread(changeValue);
      t1.start();
      t2.start();

      // Wait for each thread to complete
      t1.join();
      t2.join();
    } catch (InterruptedException ie) {
      // We were unable to wait for the completion of the threads
      Assert.fail();
    }

    Assert.assertTrue(count[0] == 2);
    Assert.assertTrue(o.getValue() == 101);
    Assert.assertTrue(c[0].getValue() == 101);

    o.setValue(0);
    Assert.assertTrue(count[0] == 3);
    Assert.assertTrue(c[0].getValue() == 0);

    waitForBarrier[0] = true;
    try {
      // Run the changeValue task in another thread.
      Thread t3 = new Thread(changeValue);
      t3.start();
      o.setValue(-100);

      // Wait for changeValue thread to complete
      t3.join();
    } catch (InterruptedException ie) {
      Assert.fail();
    }
    Assert.assertTrue(count[0] == 5);
    Assert.assertTrue(c[0].getValue() == -99);
  }
}
