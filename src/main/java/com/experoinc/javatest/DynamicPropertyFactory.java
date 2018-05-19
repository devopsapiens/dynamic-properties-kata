package com.experoinc.javatest;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Static factory methods to create {@link DynamicProperty} instances.
 */
public class DynamicPropertyFactory {

  private static final Logger logger = LoggerFactory.getLogger(DynamicProperty.class);


  private DynamicPropertyFactory() {

  }


  /**
   * Creates an {@link DynamicProperty} instance with <code>initialValue</code>
   *
   * @param initialValue The initial value of the property
   * @param
   * @param <T>          The data type
   * @return
   */
  public static <T> DynamicProperty<T> create(T initialValue ) {

        DynamicProperty<T> returnval = new DynamicProperty<T>() {
        private T property;
        
        @Override
        public T getValue() {
        	
        	return property;
        	}

        @Override
        public void setValue(T value) {
            property = value;
        }
        @Override
        public Closeable subscribe(Observer<T> callback) {
          System.out.println("CREATE PROPERTY SIMPLE ----- SUBSCRIBE METHOD callback is -> " + callback.toString());
          Closeable close = new Closeable() {
            @Override
            public void close() throws IOException {
              System.out.println("CREATE PROPERTY COMPLEX ----- SUBSCRIBE-> " + callback.toString());
              notify();
            }
          };
          return close;
        }
      };
      returnval.setValue(initialValue);
      System.out.println("CREATE PROPERTY SIMPLE ----- RETURN VALUE -> " + returnval.toString());
      return returnval;
  }

  /**
   * Creates a {@link DynamicProperty} instance whose <code>Value</code> property is determined by running a function.
   * We call this a <code>CalculatedDynamicProperty</code>.
   * <p>
   * Attempts to set the <code>Value</code> property only result in the write function being called. The actual
   * <code>Value</code> of the property will not change unless a re-evaluation of the read function is triggered.
   *
   * @param read  Called to calculate the value of the property.
   *              This method will be called exactly 1 time during construction to determine the initial value
   *              of this calculated dynamic property.
   *              If, during execution, this method accesses any other {@link DynamicProperty} instances, then
   *              this calculated dynamic property will subscribe to those other instances.
   *              When any of those instances are changed, this read function will be called again to
   *              determine the new value of this calculated dynamic property.
   *              Under no other circumstances will this method be called.  **Specifically this method should
   *              not automatically be called
   * @param write Called whenever the <code>DynamicProperty.Value</code> property setter of this is invoked.
   *              This write action can do anything it wants with the written value.
   * @param <T>   The data type
   * @return
   */
  public static <T> DynamicProperty<T> create(Callable<T> read, Observer<T> write) {

      DynamicProperty<T> returnval = new DynamicProperty<T>() {
        private T property;

        @Override
        public T getValue() {
          try {
			return read.call();
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return property;
        }

        @Override
        public void setValue(T value) {
            System.out.println("CREATE PROPERTY COMPLEX ----- SET VALUE -> " + value.toString());
            try {
				property = read.call();
				write.notify();
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
         
        }

        @Override
        public Closeable subscribe(Observer<T> callback) {
          Closeable close = new Closeable() {
            @Override
            public void close() throws IOException {
              System.out.println("CREATE PROPERTY COMPLEX ----- SUBSCRIBE-> " + callback.toString());
              notify();
            }
          };
          return close;
        }
      };
      returnval.subscribe(write);
      return returnval;
  }
}
