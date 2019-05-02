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
package org.apache.fineract.cn.test.env;

import org.apache.fineract.cn.lang.AutoTenantContext;
import org.apache.fineract.cn.lang.security.RsaKeyPairFactory;
import org.junit.rules.ExternalResource;
import org.springframework.util.Base64Utils;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

@SuppressWarnings({"WeakerAccess", "unused"})
public final class TestEnvironment extends ExternalResource {

  public static final String SPRING_APPLICATION_NAME_PROPERTY = "spring.application.name";

  public static final String SERVER_PORT_PROPERTY = "server.port";
  public static final String SERVER_PORT_DEFAULT = "9090";

  public static final String SERVER_CONTEXT_PATH_PROPERTY = "server.contextPath";

  public static final String SYSTEM_PUBLIC_KEY_TIMESTAMP_PROPERTY = "system.publicKey.timestamp";
  public static final String SYSTEM_PUBLIC_KEY_MODULUS_PROPERTY = "system.publicKey.modulus";
  public static final String SYSTEM_PUBLIC_KEY_EXPONENT_PROPERTY = "system.publicKey.exponent";
  public static final String SYSTEM_PRIVATE_KEY_MODULUS_PROPERTY = "system.privateKey.modulus";
  public static final String SYSTEM_PRIVATE_KEY_EXPONENT_PROPERTY = "system.privateKey.exponent";

  public static final String CASSANDRA_CLUSTER_NAME_PROPERTY = "cassandra.clusterName";
  public static final String CASSANDRA_CLUSTER_NAME_DEFAULT = "Test Cluster";

  public static final String CASSANDRA_CONTACT_POINTS_PROPERTY = "cassandra.contactPoints";
  public static final String CASSANDRA_CONTACT_POINTS_DEFAULT = "127.0.0.1:9142";

  public static final String CASSANDRA_META_KEYSPACE_PROPERTY = "cassandra.keyspace";
  public static final String CASSANDRA_META_KEYSPACE_DEFAULT = "seshat";

  public static final String CASSANDRA_CONSISTENCY_LEVEL_READ_PROPERTY = "cassandra.cl.read";
  public static final String CASSANDRA_CONSISTENCY_LEVEL_WRITE_PROPERTY = "cassandra.cl.write";
  public static final String CASSANDRA_CONSISTENCY_LEVEL_DELETE_PROPERTY = "cassandra.cl.delete";
  public static final String CASSANDRA_CONSISTENCY_LEVEL_DEFAULT = "ONE";

  public static final String POSTGRESQL_DRIVER_CLASS_PROPERTY = "postgresql.driverClass";
  public static final String POSTGRESQL_DRIVER_CLASS_DEFAULT = "org.postgresql.Driver";

  public static final String POSTGRESQL_DATABASE_NAME_PROPERTY = "postgresql.database";
  public static final String POSTGRESQL_DATABASE_NAME_DEFAULT = "seshat";

  public static final String POSTGRESQL_HOST_PROPERTY = "postgresql.host";
  public static final String POSTGRESQL_HOST_DEFAULT = "localhost";

  public static final String POSTGRESQL_PORT_PROPERTY = "postgresql.port";
  public static final String POSTGRESQL_PORT_DEFAULT = "5432";

  public static final String POSTGRESQL_USER_PROPERTY = "postgresql.user";
  public static final String POSTGRESQL_USER_DEFAULT = "postgres";

  public static final String POSTGRESQL_PASSWORD_PROPERTY = "postgresql.password";
  public static final String POSTGRESQL_PASSWORD_DEFAULT = "postgres";

  public static final String SPRING_CLOUD_DISCOVERY_ENABLED_PROPERTY = "spring.cloud.discovery.enabled";
  public static final String SPRING_CLOUD_DISCOVERY_ENABLED_DEFAULT = "false";

  public static final String SPRING_CLOUD_CONFIG_ENABLED_PROPERTY = "spring.cloud.config.enabled";
  public static final String SPRING_CLOUD_CONFIG_ENABLED_DEFAULT = "false";

  public static final String FLYWAY_ENABLED_PROPERTY = "flyway.enabled";
  public static final String FLYWAY_ENABLED_DEFAULT = "false";

  //Remove circuit breaker.  We don't want a fallback when a call fails in a component test.
  public static final String HYSTRIX_ENABLED_PROPERTY = "feign.hystrix.enabled";
  public static final String HYSTRIX_ENABLED_DEFAULT = "false";

  public static final String RIBBON_USES_EUREKA_PROPERTY = "ribbon.eureka.enabled";
  public static final String RIBBON_USES_EUREKA_DEFAULT = "false";

  public static final String RIBBON_LIST_OF_SERVERS_PROPERTY = "ribbon.listOfServers";
  public static final String RIBBON_SERVER_DEFAULT = "localhost";

  public static AutoTenantContext createRandomTenantContext()
  {
    final String randomTenantName = getRandomTenantName();
    return new AutoTenantContext(randomTenantName);
  }

  public static String getRandomTenantName() {
    return "cleopatra" + Math.abs(new Random().nextInt());
  }

  public static String encodePassword(final String password) {
    return Base64Utils.encodeToString(password.getBytes());
  }

  Properties properties;
  private RsaKeyPairFactory.KeyPairHolder keyPairHolder;
  private int uniquenessSuffix = 0;

  public TestEnvironment(final String springApplicationName) {
    super();
    this.initialize(springApplicationName);
  }

  @Override
  protected void before() {
    // initialize test environment and populate default properties
    populate();
  }

  public TestEnvironment addProperties(final ExtraProperties properties) {
    properties.entrySet().forEach(x -> setProperty(x.getKey(), x.getValue()));

    return this;
  }

  public String generateUniqueIdentifier(final String prefix) {
    return generateUniqueIdentifier(prefix, 1);
  }

  //prefix followed by a positive number.
  public String generateUniqueIdentifier(final String prefix, final int minimumDigitCount) {
    uniquenessSuffix++;
    final String format = String.format("%%0%dd", minimumDigitCount);
    return prefix + String.format(format, uniquenessSuffix);
  }

  public void setContextPath(final String contextPath) {
    this.properties.setProperty(SERVER_CONTEXT_PATH_PROPERTY, contextPath);
  }

  public String serverURI() {
    return "http://localhost:" +
        this.properties.getProperty(TestEnvironment.SERVER_PORT_PROPERTY) +
        this.properties.getProperty(TestEnvironment.SERVER_CONTEXT_PATH_PROPERTY);
  }

  public String getSystemKeyTimestamp() {
    return keyPairHolder.getTimestamp();
  }

  public RSAPublicKey getSystemPublicKey() {
    return keyPairHolder.publicKey();
  }

  public RSAPrivateKey getSystemPrivateKey() {
    return keyPairHolder.privateKey();
  }

  public void setKeyPair(final String timestamp, final RSAPublicKey publicKey, final RSAPrivateKey privateKey)
  {
    this.keyPairHolder = new RsaKeyPairFactory.KeyPairHolder(timestamp, publicKey, privateKey);

    this.properties.setProperty(SYSTEM_PUBLIC_KEY_TIMESTAMP_PROPERTY, getSystemKeyTimestamp());
    this.properties.setProperty(SYSTEM_PUBLIC_KEY_MODULUS_PROPERTY, publicKey.getModulus().toString());
    this.properties.setProperty(SYSTEM_PUBLIC_KEY_EXPONENT_PROPERTY, publicKey.getPublicExponent().toString());
  }

  public void addSystemPrivateKeyToProperties()
  {
    setProperty(SYSTEM_PUBLIC_KEY_TIMESTAMP_PROPERTY, getSystemKeyTimestamp());
    setProperty(SYSTEM_PRIVATE_KEY_MODULUS_PROPERTY, getSystemPrivateKey().getModulus().toString());
    setProperty(SYSTEM_PRIVATE_KEY_EXPONENT_PROPERTY, getSystemPrivateKey().getPrivateExponent().toString());
  }

  public void setProperty(final String key, final String value) {
    this.properties.setProperty(key, value);
  }

  public String getProperty(final String key) {
    return this.properties.getProperty(key);
  }

  public void populate() {
    System.getProperties().putAll(this.properties);
  }

  public void populateProcessEnvironment(final ProcessBuilder processBuilder) {
    properties.entrySet().forEach(entry -> populateVariable(processBuilder, entry));
  }

  private String populateVariable(final ProcessBuilder processBuilder, final Map.Entry<Object, Object> entry) {
    return processBuilder.environment().put(entry.getKey().toString(), entry.getValue().toString());
  }

  private void initialize(final String springApplicationName) {
    this.properties = new Properties();
    this.properties.setProperty(SPRING_APPLICATION_NAME_PROPERTY, springApplicationName);
    this.properties.setProperty(SERVER_PORT_PROPERTY, SERVER_PORT_DEFAULT);
    this.properties.setProperty(SERVER_CONTEXT_PATH_PROPERTY, "/" + springApplicationName.replace("-", "/"));
    this.properties.setProperty(CASSANDRA_CLUSTER_NAME_PROPERTY, CASSANDRA_CLUSTER_NAME_DEFAULT);
    this.properties.setProperty(CASSANDRA_CONTACT_POINTS_PROPERTY, CASSANDRA_CONTACT_POINTS_DEFAULT);
    this.properties.setProperty(CASSANDRA_META_KEYSPACE_PROPERTY, CASSANDRA_META_KEYSPACE_DEFAULT);
    this.properties.setProperty(CASSANDRA_CONSISTENCY_LEVEL_READ_PROPERTY, CASSANDRA_CONSISTENCY_LEVEL_DEFAULT);
    this.properties.setProperty(CASSANDRA_CONSISTENCY_LEVEL_WRITE_PROPERTY, CASSANDRA_CONSISTENCY_LEVEL_DEFAULT);
    this.properties.setProperty(CASSANDRA_CONSISTENCY_LEVEL_DELETE_PROPERTY, CASSANDRA_CONSISTENCY_LEVEL_DEFAULT);
    this.properties.setProperty(POSTGRESQL_DRIVER_CLASS_PROPERTY, POSTGRESQL_DRIVER_CLASS_DEFAULT);
    this.properties.setProperty(POSTGRESQL_DATABASE_NAME_PROPERTY, POSTGRESQL_DATABASE_NAME_DEFAULT);
    this.properties.setProperty(POSTGRESQL_HOST_PROPERTY, POSTGRESQL_HOST_DEFAULT);
    this.properties.setProperty(POSTGRESQL_PORT_PROPERTY, POSTGRESQL_PORT_DEFAULT);
    this.properties.setProperty(POSTGRESQL_USER_PROPERTY, POSTGRESQL_USER_DEFAULT);
    this.properties.setProperty(POSTGRESQL_PASSWORD_PROPERTY, POSTGRESQL_PASSWORD_DEFAULT);
    this.properties.setProperty(SPRING_CLOUD_DISCOVERY_ENABLED_PROPERTY, SPRING_CLOUD_DISCOVERY_ENABLED_DEFAULT);
    this.properties.setProperty(SPRING_CLOUD_CONFIG_ENABLED_PROPERTY, SPRING_CLOUD_CONFIG_ENABLED_DEFAULT);
    this.properties.setProperty(FLYWAY_ENABLED_PROPERTY, FLYWAY_ENABLED_DEFAULT);
    this.properties.setProperty(HYSTRIX_ENABLED_PROPERTY, HYSTRIX_ENABLED_DEFAULT);
    this.properties.setProperty(RIBBON_USES_EUREKA_PROPERTY, RIBBON_USES_EUREKA_DEFAULT);
    this.properties.setProperty(RIBBON_LIST_OF_SERVERS_PROPERTY, RIBBON_SERVER_DEFAULT + ":" + SERVER_PORT_DEFAULT);

    this.keyPairHolder = RsaKeyPairFactory.createKeyPair();

    this.properties.setProperty(SYSTEM_PUBLIC_KEY_TIMESTAMP_PROPERTY, this.keyPairHolder.getTimestamp());
    this.properties.setProperty(SYSTEM_PUBLIC_KEY_MODULUS_PROPERTY, this.keyPairHolder.publicKey().getModulus().toString());
    this.properties.setProperty(SYSTEM_PUBLIC_KEY_EXPONENT_PROPERTY, this.keyPairHolder.publicKey().getPublicExponent().toString());
  }
}
