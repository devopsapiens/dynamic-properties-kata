package com.experoinc.javatest;

/**
 * Observes values
 */
public interface Observer<T> {
  void observe(T value);
}
