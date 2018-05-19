package com.experoinc.javatest;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Static factory methods to create {@link DynamicProperty} instances.
 * 
 * @param <T>
 */
public class DynamicPropertyFactory<T> {

	private static final Logger logger = LoggerFactory.getLogger(DynamicProperty.class);

	private DynamicPropertyFactory() {

	}

	/**
	 * Creates an {@link DynamicProperty} instance with <code>initialValue</code>
	 *
	 * @param initialValue
	 *            The initial value of the property
	 * @param
	 * @param <T>
	 *            The data type
	 * @return
	 */
	public static <T> DynamicProperty<T> create(T initialValue) {
		logger.info("<<<<<<<<<-------- CALL CREATE DYN PROPERTY -> " + initialValue.toString());

		DynamicProperty<T> dynamicProperty = new DynamicPropertyWrapper<T>(initialValue);
		logger.debug("DYNAMIC PROPERTY CREATED -> " + dynamicProperty.toString());

		dynamicProperty.setValue(initialValue);

		logger.info("<<<<<<<<<-------- CALL CREATE DYN PROPERTY -> " + initialValue.toString());
		return dynamicProperty;
	}

	/**
	 * Creates a {@link DynamicProperty} instance whose <code>Value</code> property
	 * is determined by running a function. We call this a
	 * <code>CalculatedDynamicProperty</code>.
	 * <p>
	 * Attempts to set the <code>Value</code> property only result in the write
	 * function being called. The actual <code>Value</code> of the property will not
	 * change unless a re-evaluation of the read function is triggered.
	 *
	 * @param read
	 *            Called to calculate the value of the property. This method will be
	 *            called exactly 1 time during construction to determine the initial
	 *            value of this calculated dynamic property. If, during execution,
	 *            this method accesses any other {@link DynamicProperty} instances,
	 *            then this calculated dynamic property will subscribe to those
	 *            other instances. When any of those instances are changed, this
	 *            read function will be called again to determine the new value of
	 *            this calculated dynamic property. Under no other circumstances
	 *            will this method be called. **Specifically this method should not
	 *            automatically be called
	 * @param write
	 *            Called whenever the <code>DynamicProperty.Value</code> property
	 *            setter of this is invoked. This write action can do anything it
	 *            wants with the written value.
	 * @param <T>
	 *            The data type
	 * @return
	 */
	public static <T> DynamicProperty<T> create(Callable<T> read, Observer<T> write) {

		DynamicProperty<T> returnval = new DynamicPropertyWrapper<T>(write, read);
		returnval.subscribe(write);
		return returnval;
	}

	private static final class DynamicPropertyWrapper<T> implements DynamicProperty<T> {

		private final Observer<T> write;
		private final Callable<T> read;
		private volatile T property;
		
		private final Set<Closeable> callbacks = new CopyOnWriteArraySet<Closeable>();

		private DynamicPropertyWrapper(Observer<T> write, Callable<T> read) {
			this.write = write;
			this.read = read;
			try {
				this.property = read.call();
			} catch (Exception e) {
				logger.error("Error in DynamicPropertyWrapper Constructor " + e.getMessage());
				e.printStackTrace();
			}
		}

		public DynamicPropertyWrapper(T initialValue) {
			if(property!=null && !(property.equals(initialValue))) {
				property = initialValue;
			}
			write = new Observer<T>() {
			      @Override
			      public void observe(T value) {
			    
			      }
			    };
			subscribe(write);
			read = null;
			addCallback(new Closeable() {
				@Override
				public void close() throws IOException {
					// TODO Auto-generated method stub
					
				}
			});

		}

		@Override
		public synchronized T getValue() {
			return property;
		}

		@Override
		public synchronized void setValue(T value) {

			logger.debug("CREATE PROPERTY COMPLEX ----- SET VALUE -> " + value.toString());
			if (read != null) {
				try {
					if (!(read.call().equals(value))) {
						property = value;
					} else {
						property = read.call();
						write.observe(value);
						subscribe(write);
					}
				} catch (Exception e) {
					logger.error(e.getMessage());
				}
			} else {
				property = value;
			}
		}

		@Override
		public Closeable subscribe(Observer<T> callback) {
			Closeable cls = new Closeable() {
				@Override
				public void close() throws IOException {
					logger.debug("CREATE PROPERTY COMPLEX ----- SUBSCRIBE-> " + callback.toString());
					notify();
				}
			};
			addCallback(cls);
			return cls;
		}

		public void addCallback(Closeable callback) {
			if (callback != null) {
				callbacks.add(callback);
			}
		}

		public void removeAllCallbacks() {
			final Set<Closeable> callbacksToRemove = new HashSet<Closeable>(callbacks);
			callbacks.removeAll(callbacksToRemove);
		}
	}
}
