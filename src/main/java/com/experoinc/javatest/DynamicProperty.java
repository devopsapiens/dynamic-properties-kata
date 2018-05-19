package com.experoinc.javatest;

import java.io.Closeable;

/**
 * Represents a property that can be observed or updated.
 *
 * @param <T>
 */
public interface DynamicProperty<T> {

  /**
   * Gets or sets the value of the property.
   * Whenever the value is updated, all observers will be notified of the new value.
   */
  T getValue();

  void setValue(T value);

  /**
   * Subscribes a callback to this dynamic property.
   * Anytime this dynamic property value is modified, <code>callback</code> should be called with the new value.
   * Returns an object that can be disposed to end the subscription and stop notifications sent to <code>callback</code>.
   * <p>
   * Any number of subscriptions can be made to dynamic property.  All subscriptions should be notified when <code>Value</code> changes.
   *
   * @param callback Method to be called whenever <code>Value</code> is modified
   */
  Closeable subscribe(Observer<T> callback);

}
