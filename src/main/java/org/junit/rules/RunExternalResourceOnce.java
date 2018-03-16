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
package org.junit.rules;

/**
 * Use this to "decorate" a resource to ensure that it is
 * initialized exactly once and de-initialized exactly once
 * when used in a suite of multiple tests. This is mostly
 * useful when creating test suites.
 *
 * Example:
 *
 * <pre>
 * {@code
 * @literal @ClassRule
 *     public static TestRule orderClassRules = RuleChain
 *         .outerRule(new RunExternalResourceOnce(testEnvironment))
 *         .around(new RunExternalResourceOnce(cassandraInitializer))
 *         .around(new RunExternalResourceOnce(mariaDBInitializer));
 * }
 * </pre>
 *
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
public class RunExternalResourceOnce extends ExternalResource {
  private final ExternalResource decoratedResource;
  private int callCount = 0;

  public RunExternalResourceOnce(final ExternalResource decoratedResource) {
    //I love to decorate. Don't you?
    this.decoratedResource = decoratedResource;
  }

  @Override
  protected void before() throws Throwable {
    if (callCount == 0)
      decoratedResource.before();
    callCount++;
  }

  @Override
  protected void after() {
    callCount--;
    if (callCount == 0)
      decoratedResource.after();
  }
}