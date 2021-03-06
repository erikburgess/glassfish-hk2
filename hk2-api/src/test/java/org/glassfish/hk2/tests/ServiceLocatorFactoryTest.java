/*
 * Copyright (c) 2012, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.hk2.tests;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.api.ServiceLocatorListener;
import org.glassfish.hk2.tests.extension.ServiceLocatorImpl;
import org.junit.Test;

/**
 * @author jwells
 *
 */
public class ServiceLocatorFactoryTest {
  
  /**
   * Tests you can add a locator to the system
   */
  @Test
  public void testAddToServiceLocatorFactory() {
    ServiceLocatorFactory slf = ServiceLocatorFactory.getInstance();
    Assert.assertNotNull(slf);
    
    ServiceLocator sl = slf.create("AddtoServiceLocatorFactory");
    Assert.assertNotNull(sl);
  }
  
  private final static String FIND_FROM_SLF = "FindFromServiceLocatorFactory";
  /**
   * Tests that a locator is not removed after a find... uh, duh...
   */
  @Test
  public void testFindFromServiceLocatorFactory() {
    ServiceLocatorFactory slf = ServiceLocatorFactory.getInstance();
    slf.create(FIND_FROM_SLF);
    
    ServiceLocator sl = slf.find(FIND_FROM_SLF);
    Assert.assertNotNull(sl);
    
    // Do it a second time to make sure it didn't get removed
    sl = slf.find(FIND_FROM_SLF);
    Assert.assertNotNull(sl);
  }
  
  private final static String DELETE_FROM_SLF = "DeleteFromServiceLocatorFactory";
  /**
   * Tests that our dummy service locator is properly removed on shutdown
   */
  @Test
  public void testDeleteFromServiceLocatorFactory() {
    ServiceLocatorFactory slf = ServiceLocatorFactory.getInstance();
    ServiceLocator locator = slf.create(DELETE_FROM_SLF);
    if (!(locator instanceof ServiceLocatorImpl)) return;
    
    ServiceLocatorImpl sl = (ServiceLocatorImpl) locator;
    Assert.assertNotNull(sl);
    Assert.assertFalse(sl.isShutdown());
    
    slf.destroy(DELETE_FROM_SLF);
    Assert.assertTrue(sl.isShutdown());
    
    // Make sure it is really gone
    Assert.assertNull(slf.find(DELETE_FROM_SLF));
    
    // And that destroying it again does no damage
    slf.destroy(DELETE_FROM_SLF); 
  }
  
  private final static String SLF_LISTENER_1 = "ServiceLocatorFactoryListener1";
  private final static String SLF_LISTENER_2 = "ServiceLocatorFactoryListener2";
  /**
   * Tests that listeners can be established on the create/destroy process
   */
  @Test
  public void testListenerMethods() {
      ServiceLocatorFactory slf = ServiceLocatorFactory.getInstance();
      
      ServiceLocatorListenerImpl listener1 = new ServiceLocatorListenerImpl();
      
      slf.addListener(listener1);
      
      slf.create(SLF_LISTENER_1);
      slf.create(SLF_LISTENER_2);
      ServiceLocator tmpLocator = slf.create(null);
      
      // Ensures a second listener can be added and will be initialized properly
      ServiceLocatorListenerImpl listener2 = new ServiceLocatorListenerImpl();
      slf.addListener(listener2);
      
      Set<String> names1 = listener1.getLocatorNames();
      
      Assert.assertTrue(names1.contains(SLF_LISTENER_1));
      Assert.assertTrue(names1.contains(SLF_LISTENER_2));
      Assert.assertTrue(names1.contains(tmpLocator.getName()));
      
      Set<String> names2 = listener2.getLocatorNames();
      
      Assert.assertTrue(names2.contains(SLF_LISTENER_1));
      Assert.assertTrue(names2.contains(SLF_LISTENER_2));
      Assert.assertFalse(names2.contains(tmpLocator.getName()));
      
      slf.destroy(SLF_LISTENER_1);
      
      Assert.assertFalse(names1.contains(SLF_LISTENER_1));
      Assert.assertTrue(names1.contains(SLF_LISTENER_2));
      Assert.assertTrue(names1.contains(tmpLocator.getName()));
      
      Assert.assertFalse(names2.contains(SLF_LISTENER_1));
      Assert.assertTrue(names2.contains(SLF_LISTENER_2));
      Assert.assertFalse(names2.contains(tmpLocator.getName()));
      
      slf.destroy(tmpLocator);
      
      Assert.assertFalse(names1.contains(SLF_LISTENER_1));
      Assert.assertTrue(names1.contains(SLF_LISTENER_2));
      Assert.assertFalse(names1.contains(tmpLocator.getName()));
      
      Assert.assertFalse(names2.contains(SLF_LISTENER_1));
      Assert.assertTrue(names2.contains(SLF_LISTENER_2));
      Assert.assertFalse(names2.contains(tmpLocator.getName()));
      
      slf.removeListener(listener1);
      
      slf.destroy(SLF_LISTENER_2);
      
      Assert.assertFalse(names1.contains(SLF_LISTENER_1));
      Assert.assertTrue(names1.contains(SLF_LISTENER_2));
      
      Assert.assertFalse(names2.contains(SLF_LISTENER_1));
      Assert.assertFalse(names2.contains(SLF_LISTENER_2));
      
      slf.removeListener(listener2);
  }
  
  private final static String AUTO_DESTROY_LOCATOR = "AutoDestroyLocator";
  /**
   * Tests that the create policy DESTROY works
   * 
   * @author jwells
   */
  @Test
  public void testDestroyPolicyWorks() {
      DestructionListener dListener = new DestructionListener();
      
      ServiceLocatorFactory slf = ServiceLocatorFactory.getInstance();
      
      slf.addListener(dListener);
      
      slf.create(AUTO_DESTROY_LOCATOR);
      
      Assert.assertFalse(dListener.wasDestroyed(AUTO_DESTROY_LOCATOR));
      
      slf.create(AUTO_DESTROY_LOCATOR, null, null, ServiceLocatorFactory.CreatePolicy.DESTROY);
      
      Assert.assertTrue(dListener.wasDestroyed(AUTO_DESTROY_LOCATOR));
      
      slf.removeListener(dListener);
      
  }
  
  private final static String ERROR_LOCATOR = "ErrorLocator";
  
  /**
   * Tests that the create policy ERROR works
   * 
   * @author jwells
   */
  @Test(expected=java.lang.IllegalStateException.class)
  public void testErrorPolicyWorks() {
      ServiceLocatorFactory slf = ServiceLocatorFactory.getInstance();
      slf.create(ERROR_LOCATOR);
      
      try {
          slf.create(ERROR_LOCATOR, null, null, ServiceLocatorFactory.CreatePolicy.ERROR);
      }
      finally {
          slf.destroy(ERROR_LOCATOR);
      }
  }
  
  private static class ServiceLocatorListenerImpl implements ServiceLocatorListener {
      private final HashSet<String> locators = new HashSet<String>();

      @Override
      public void initialize(Set<ServiceLocator> initialLocators) {
        for (ServiceLocator locator : initialLocators) {
            locators.add(locator.getName());
        }
        
      }

      @Override
      public void locatorAdded(ServiceLocator added) {
          locators.add(added.getName());
      }

      @Override
      public void locatorDestroyed(ServiceLocator destroyed) {
          locators.remove(destroyed.getName());
      }
      
      public Set<String> getLocatorNames() {
          return locators;
      }
  }
  
  private static class DestructionListener implements ServiceLocatorListener {
      private final Set<String> destroyedLocators = new HashSet<String>();

      @Override
      public void initialize(Set<ServiceLocator> initialLocators) {
      }

      @Override
      public void locatorAdded(ServiceLocator added) {
      }

      @Override
      public void locatorDestroyed(ServiceLocator destroyed) {
          destroyedLocators.add(destroyed.getName()); 
      }
      
      private boolean wasDestroyed(String name) {
          return destroyedLocators.contains(name);
      }
      
  }
}
