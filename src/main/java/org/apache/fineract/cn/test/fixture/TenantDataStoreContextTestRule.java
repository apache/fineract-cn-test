/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.cn.test.fixture;

import org.apache.fineract.cn.test.env.TestEnvironment;
import org.junit.rules.ExternalResource;
import org.springframework.util.Assert;

import javax.annotation.Nullable;

/**
 * Use an instance of class as a @Classrule in component tests which require a tenant
 * initialized in the data store(s) used by the service under test. It will generate a
 * tenant name, create any necessary data structures, and set the tenant context for
 * REST calls into the service.
 *
 * Example:
 * <pre>
 * {@code
 * @literal @ClassRule
 *     public final static TenantDataStoreContextTestRule tenantDataStoreContext
 *         = TenantDataStoreContextTestRule.forRandomTenantName(cassandraInitializer, mariaDBInitializer);
 * }
 * </pre>
 *
 *
 * @author Myrle Krantz
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class TenantDataStoreContextTestRule extends ExternalResource {
  @Nullable
  private String tenantName;
  @Nullable
  private TenantDataStoreTestContext tenantDataStoreTestContext;

  private final boolean generateTenantName;
  private final DataStoreTenantInitializer[] dataStoreTenantInitializers;

  private TenantDataStoreContextTestRule(
      final String tenantName,
      final DataStoreTenantInitializer... dataStoreTenantInitializers) {
    this.generateTenantName = false;
    this.tenantName = tenantName;
    this.dataStoreTenantInitializers = dataStoreTenantInitializers;
  }

  private TenantDataStoreContextTestRule(
      final DataStoreTenantInitializer... dataStoreTenantInitializers) {
    this.generateTenantName = true;
    this.tenantName = null;
    this.dataStoreTenantInitializers = dataStoreTenantInitializers;
  }

  public static TenantDataStoreContextTestRule forRandomTenantName(
          final DataStoreTenantInitializer... dataStoreTenantInitializers)
  {
    return new TenantDataStoreContextTestRule(dataStoreTenantInitializers);
  }

  public static TenantDataStoreContextTestRule forDefinedTenantName(
          final String tenantName,
          final DataStoreTenantInitializer... dataStoreTenantInitializers)
  {
    return new TenantDataStoreContextTestRule(tenantName, dataStoreTenantInitializers);
  }

  @Override
  public void before() {
    if (generateTenantName) {
      //Generate the tenantName in the before method rather than in the constructor.
      //If this rule is used as a static variable in a test parent class, in the context of a
      //test suite consisting of multiple test classes, the tenantName should
      //be regenerated for each test class run.  This has two advantages:
      //1.) The database initialization isn't re-executed for the tenant. Re-executing database
      //    initialization fails, causing all tests after the first one to fail.
      //2.) Each test class is executed in the context of a new tenant, thus mostly isolating
      //    each test class from any side-effects produced by the others.
      tenantName = TestEnvironment.getRandomTenantName();
    }
    tenantDataStoreTestContext = TenantDataStoreTestContext.forDefinedTenantName(tenantName, dataStoreTenantInitializers);
  }

  @Override
  public void after() {
    Assert.notNull(tenantDataStoreTestContext);
    tenantDataStoreTestContext.close();
  }

  public String getTenantName() {
    return tenantName;
  }
}