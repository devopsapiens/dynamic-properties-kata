package com.experoinc.javatest;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
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

    @SuppressWarnings("unused")
	private static final ConcurrentHashMap<String, DynamicProperty> CREATED_PROPERTIES = new ConcurrentHashMap<String, DynamicProperty>();
    
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

		DynamicProperty<T> dynamicProperty = new DynamicPropertyWrapper<T>(initialValue);
		
		CREATED_PROPERTIES.put(dynamicProperty.toString(), dynamicProperty);
		
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

		DynamicProperty<T> dynamicProperty = new DynamicPropertyWrapper<T>(write, read);
		
		CREATED_PROPERTIES.put(dynamicProperty.toString(), dynamicProperty);
		return dynamicProperty;
	}

	/**
	 * Nested Class to wrap the DynamicProperty Implementation.
	 * @author erasmodominguezjimenez
	 * @param <T>
	 */
private static final class DynamicPropertyWrapper<T> implements DynamicProperty<T> {
		
		private volatile T property;
		
		private final Set<Observer> callbacks = new CopyOnWriteArraySet<Observer>();

		//Constructor
		protected DynamicPropertyWrapper(Observer<T> write, Callable<T> read) {
			try {
				setProperty(write,read.call(),true);
			} catch (Exception e) {
				logger.error("Error in DynamicPropertyWrapper Constructor " + e.getMessage());
				e.printStackTrace();
			}
		}
        
		//Constructor
		protected DynamicPropertyWrapper(T initialValue) {
			
			setProperty(null ,initialValue, true);
		}
		/**
		 * 
		 * @param write
		 * @param value
		 * @param InitializeProperty Used to identify if the set Value was call from Create Method or if is an update - set property call.
		 */
		private  void setProperty(Observer<T> write, T value, boolean InitializeProperty) {

			if(InitializeProperty) {
				property = value;
				return;
			}
			
			//If this method was not invoked from Create method, is not a new Dynamic Property so , we can consider that is "an update"
			property = value;
			if (write !=null) {
				subscribe(write);
			    callbacks.forEach(call -> call.notify());	
			}
			
		}


		@Override
		public synchronized T getValue() {
			return property;
		}

		@Override
		public synchronized void setValue(T value) {
             
			setProperty(null, value,false);
		}

		@Override
		public Closeable subscribe(Observer<T> callback) {
			Closeable cls = new Closeable() {
				
				@Override
				public void close() throws IOException {
					callbacks.add(callback);
				}
			};
			return cls;
		}
	}
}
