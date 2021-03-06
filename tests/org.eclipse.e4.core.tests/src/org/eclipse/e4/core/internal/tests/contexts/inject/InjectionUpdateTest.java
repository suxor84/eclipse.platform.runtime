/*******************************************************************************
 * Copyright (c) 2010, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.e4.core.internal.tests.contexts.inject;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import junit.framework.TestCase;

import org.eclipse.e4.core.contexts.ContextFunction;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;

/**
 * Tests updates of injected values and calls to runnables
 */
public class InjectionUpdateTest extends TestCase {

	private IEclipseContext c1; // common root
	private IEclipseContext c21; // dependents of root - path 1
	private IEclipseContext c22; // dependents of root - path 2

	static public class PropagationTest {

		public int called = 0;
		public String in; 

		@Inject
		public PropagationTest() {
			// placeholder
		}

		@Inject
		public void setCalculated(@Named("calculated") String string) {
			called++;
			in = string;
		}
	}

	protected void setUp() throws Exception {
		super.setUp();
		
		c1 = EclipseContextFactory.create("c1");
		c1.set("id", "c1");

		c21 = c1.createChild("c21");
		c21.set("id", "c21");
		c1.set("c21", c21);

		c22 = c1.createChild("c22");
		c22.set("id", "c22");
		c1.set("c22", c22);
	}
	
	public void testPropagation() {
		c1.set("base", "abc");

		c21.set("derived1", new ContextFunction() {
			public Object compute(IEclipseContext context, String contextKey) {
				String baseString = (String) context.get("base");
				return baseString.charAt(0) + "_";
			}});

		c22.set("derived2", new ContextFunction() {
			public Object compute(IEclipseContext context, String contextKey) {
				String baseString = (String) context.get("base");
				return "_" + baseString.charAt(baseString.length() - 1);
			}});

		c1.set("calculated", new ContextFunction() {
			public Object compute(IEclipseContext context, String contextKey) {
				IEclipseContext context21 = (IEclipseContext) context.get("c21");
				String derived1 = (String) context21.get("derived1");
				
				IEclipseContext context22 = (IEclipseContext) context.get("c22");
				String derived2 = (String) context22.get("derived2");
				return derived1 + derived2;
			}});

		PropagationTest testObject = (PropagationTest) ContextInjectionFactory.make(PropagationTest.class, c1);
		assertNotNull(testObject);
		assertEquals(1, testObject.called);
		assertEquals("a__c", testObject.in);

		c1.set("base", "123"); // this should result in only one injection call
		assertEquals(2, testObject.called);
		assertEquals("1__3", testObject.in);

		c1.set("base", "xyz");
		assertEquals(3, testObject.called);
		assertEquals("x__z", testObject.in);
	}
	
	public static class InjectTarget {
		private static final String KEY = "key";

		@Inject
		private IEclipseContext context;

		@PostConstruct
		void pc() {
			context.containsKey(KEY);
		}

		public void modify() {
			context.set(KEY, null);
		}
	}

	public void testNestedUpdatesPostConstruct() throws Exception {
		IEclipseContext appContext = EclipseContextFactory.create();
		appContext.set(InjectTarget.class.getName(), new ContextFunction() {
			public Object compute(IEclipseContext context, String contextKey) {
				return ContextInjectionFactory
						.make(InjectTarget.class, context);
			}
		});

		InjectTarget targetA = appContext.get(InjectTarget.class);
		targetA.modify();

		InjectTarget targetB = appContext.get(InjectTarget.class);
		assertEquals(targetA, targetB);
		assertSame(targetA, targetB);
	}

	public static class InjectTarget2 {
		private static final String KEY = "key"; //$NON-NLS-1$

		@Inject
		private IEclipseContext context;

		public Object key;

		@Inject
		public InjectTarget2(@Optional @Named("key") Object key) {
			this.key = key;
		}

		public void modify() {
			context.set(KEY, null);
		}
	}

	public void testNestedUpdatesConstructor() throws Exception {
		IEclipseContext appContext = EclipseContextFactory.create();
		appContext.set(InjectTarget2.class.getName(), new ContextFunction() {
			public Object compute(IEclipseContext context, String contextKey) {
				return ContextInjectionFactory.make(InjectTarget2.class,
						context);
			}
		});

		InjectTarget2 targetA = appContext.get(InjectTarget2.class);
		targetA.modify();

		InjectTarget2 targetB = appContext.get(InjectTarget2.class);
		assertEquals(targetA, targetB);
		assertSame(targetA, targetB);
	}
	
}
