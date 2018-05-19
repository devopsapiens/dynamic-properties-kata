package com.experoinc.javatest;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;


/// Tests the minimum required features for this coding quiz
/// Tests that your dynamic properties function correctly in a single-threaded application.
public class TestSet1Basic_DynamicProperties {

  @Test
  public void canBeConstructedWithInitialValue() {
    DynamicProperty<Integer> p = DynamicPropertyFactory.create(42);
    Assert.assertTrue(p.getValue() == 42);
  }

  @Test
  public void valueCanBeChanged() {
    DynamicProperty<Integer> p = DynamicPropertyFactory.create(42);
    p.setValue(100);
    Assert.assertTrue(p.getValue() == 100);
  }

  @Test
  public void nothingImmediatelyHappensWhenSubscribed() {
    DynamicProperty<String> p = DynamicPropertyFactory.create("Forty Two");
    p.subscribe(new Observer<String>() {
      @Override
      public void observe(String value) {
        Assert.fail();
      }
    });
  }

  @Test
  public void subscriptionReceivesNotificationsWhenValueChanges() {
    final DynamicProperty<Integer> p = DynamicPropertyFactory.create(42);
    final Collection<Integer> notifications = new ArrayList<Integer>();

    // add each notification to our notifications list.
    p.subscribe(new Observer<Integer>() {
      @Override
      public void observe(Integer value) {
        notifications.add(value);
      }
    });

    p.setValue(100);
    Assert.assertArrayEquals(new Integer[]{100}, notifications.toArray());

    p.setValue(200);
    Assert.assertArrayEquals(new Integer[]{100, 200}, notifications.toArray());

    p.setValue(0);
    Assert.assertArrayEquals(new Integer[]{100, 200, 0}, notifications.toArray());
  }

  @Test
  public void subscriptionNotNotifiedAfterItIsDisposed() throws IOException {
    final DynamicProperty<Integer> p = DynamicPropertyFactory.create(42);
    final Collection<Integer> notifications = new ArrayList<Integer>();

    // add each notification to our notifications list.
    Closeable sub = p.subscribe(new Observer<Integer>() {
      @Override
      public void observe(Integer value) {
        notifications.add(value);
      }
    });

    p.setValue(100);
    Assert.assertArrayEquals(new Integer[]{100}, notifications.toArray());

    p.setValue(200);
    Assert.assertArrayEquals(new Integer[]{100, 200}, notifications.toArray());

    p.setValue(0);
    Assert.assertArrayEquals(new Integer[]{100, 200, 0}, notifications.toArray());

    sub.close();

    // No more notifications
    p.setValue(1);
    p.setValue(2);
    p.setValue(3);
    Assert.assertArrayEquals(new Integer[]{100, 200, 0}, notifications.toArray());
  }

  @Test
  public void multipleSubscribersAreNotified() throws IOException {
    final DynamicProperty<Integer> p = DynamicPropertyFactory.create(0);
    final ArrayList<ArrayList<Integer>> notifications = new ArrayList<ArrayList<Integer>>();
    notifications.add(new ArrayList<Integer>());
    notifications.add(new ArrayList<Integer>());
    notifications.add(new ArrayList<Integer>());
    Closeable[] subscriptions = new Closeable[3];
    ArrayList<ArrayList<Integer>> expected = new ArrayList<ArrayList<Integer>>();
    expected.add(new ArrayList<Integer>());
    expected.add(new ArrayList<Integer>());
    expected.add(new ArrayList<Integer>());

    subscriptions[0] = p.subscribe(new Observer<Integer>() {
      @Override
      public void observe(Integer value) {
        notifications.get(0).add(value);
      }
    });
    p.setValue(1);
    expected.get(0).add(1);

    subscriptions[1] = p.subscribe(new Observer<Integer>() {
      @Override
      public void observe(Integer value) {
        notifications.get(1).add(value);
      }
    });
    p.setValue(2);
    expected.get(0).add(2);
    expected.get(1).add(2);

    subscriptions[2] = p.subscribe(new Observer<Integer>() {
      @Override
      public void observe(Integer value) {
        notifications.get(2).add(value);
      }
    });
    p.setValue(3);
    expected.get(0).add(3);
    expected.get(1).add(3);
    expected.get(2).add(3);

    subscriptions[1].close();
    p.setValue(4);
    expected.get(0).add(4);
    expected.get(2).add(4);

    subscriptions[2].close();
    p.setValue(5);
    expected.get(0).add(5);

    subscriptions[0].close();
    p.setValue(6);

    for (int i = 0; i < 3; ++i) {
      Assert.assertArrayEquals(expected.get(i).toArray(), notifications.get(i).toArray());
    }
  }

}
