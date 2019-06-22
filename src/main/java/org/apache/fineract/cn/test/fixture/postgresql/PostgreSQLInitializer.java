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
package org.apache.fineract.cn.test.fixture.postgresql;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.apache.fineract.cn.test.env.TestEnvironment;
import org.apache.fineract.cn.test.fixture.DataStoreTenantInitializer;

import java.sql.*;
import org.apache.fineract.cn.postgresql.util.JdbcUrlBuilder;

@SuppressWarnings({"WeakerAccess", "unused", "SqlNoDataSourceInspection", "SqlDialectInspection"})
public final class PostgreSQLInitializer extends DataStoreTenantInitializer {

  private final boolean useExistingDB;
  private static EmbeddedPostgres pg;

  public PostgreSQLInitializer() {
    this(false);
  }

  public PostgreSQLInitializer(final boolean useExistingDB) {
    super();
    this.useExistingDB = useExistingDB;
  }

  @Override
  public void initialize() throws Exception  {
    PostgreSQLInitializer.setup(useExistingDB);
  }

  @Override
  public void initializeTenant(final String tenantName) {
    PostgreSQLInitializer.createDatabaseTenant(tenantName);

  }

  @Override
  public void finish() {
    if (!useExistingDB) {
      try {
        PostgreSQLInitializer.tearDown();
      } catch (final Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static void setup() throws Exception {
    setup(false);
  }
  public static void setup(final boolean useExistingDB) throws Exception {
    if (!useExistingDB) {
      PostgreSQLInitializer.startEmbeddedPostgreSQL();
      PostgreSQLInitializer.createDatabaseSeshat();
    }
  }

  public static void tearDown() throws Exception {
    if (PostgreSQLInitializer.pg != null) {
      PostgreSQLInitializer.pg.close();
      PostgreSQLInitializer.pg = null;
    }
  }

  private static void startEmbeddedPostgreSQL() throws Exception {
    PostgreSQLInitializer.pg = EmbeddedPostgres.start();
    System.setProperty(TestEnvironment.POSTGRESQL_HOST_PROPERTY, TestEnvironment.POSTGRESQL_HOST_DEFAULT);
    System.setProperty(TestEnvironment.POSTGRESQL_PORT_PROPERTY, TestEnvironment.POSTGRESQL_PORT_DEFAULT);
  }

  private static void createDatabaseSeshat() {
    try {
      Class.forName(System.getProperty(TestEnvironment.POSTGRESQL_DRIVER_CLASS_PROPERTY));
    } catch (ClassNotFoundException ex) {
      throw new IllegalArgumentException(ex.getMessage(), ex);
    }

    final String jdbcUrl = JdbcUrlBuilder
            .create(JdbcUrlBuilder.DatabaseType.POSTGRESQL)
            .host(System.getProperty(TestEnvironment.POSTGRESQL_HOST_PROPERTY))
            .port(System.getProperty(TestEnvironment.POSTGRESQL_PORT_PROPERTY))
            .instanceName(System.getProperty(TestEnvironment.POSTGRESQL_DATABASE_NAME_DEFAULT))
            .build();
    try (final Connection pgConnection = DriverManager.getConnection(jdbcUrl,
            System.getProperty(TestEnvironment.POSTGRESQL_USER_PROPERTY),
            System.getProperty(TestEnvironment.POSTGRESQL_PASSWORD_PROPERTY));
         final Statement createDbStatement = pgConnection.createStatement()) {
      pgConnection.setAutoCommit(true);
      // create meta database seshat
      createDbStatement.execute("CREATE DATABASE " + System.getProperty(TestEnvironment.POSTGRESQL_DATABASE_NAME_PROPERTY));
    } catch (final SQLException ex) {
      ex.printStackTrace();
    }

    final String tenantJdbcUrl = JdbcUrlBuilder
            .create(JdbcUrlBuilder.DatabaseType.POSTGRESQL)
            .host(System.getProperty(TestEnvironment.POSTGRESQL_HOST_PROPERTY))
            .port(System.getProperty(TestEnvironment.POSTGRESQL_PORT_PROPERTY))
            .instanceName(System.getProperty(TestEnvironment.POSTGRESQL_DATABASE_NAME_DEFAULT))
            .build();

    try (
            final Connection metaDbConnection = DriverManager.getConnection(tenantJdbcUrl,
                    System.getProperty(TestEnvironment.POSTGRESQL_USER_PROPERTY),
                    System.getProperty(TestEnvironment.POSTGRESQL_PASSWORD_PROPERTY));
            final Statement metaStatement = metaDbConnection.createStatement()
    ) {
      metaDbConnection.setAutoCommit(true);
      // create needed tenant management table
      metaStatement.execute("CREATE TABLE IF NOT EXISTS tenants (" +
              "  identifier    VARCHAR(32) NOT NULL," +
              "  driver_class  VARCHAR(255) NOT NULL," +
              "  database_name VARCHAR(32) NOT NULL," +
              "  host          VARCHAR(32) NOT NULL," +
              "  port          VARCHAR(5)  NOT NULL," +
              "  a_user        VARCHAR(32) NOT NULL," +
              "  pwd           VARCHAR(32) NOT NULL," +
              "  PRIMARY KEY (identifier)" +
              ")");
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public static void createDatabaseTenant(final String identifier) {
    try {
      Class.forName(System.getProperty(TestEnvironment.POSTGRESQL_DRIVER_CLASS_PROPERTY));
    } catch (ClassNotFoundException ex) {
      throw new IllegalArgumentException(ex.getMessage(), ex);
    }
    final String jdbcUrl = JdbcUrlBuilder
        .create(JdbcUrlBuilder.DatabaseType.POSTGRESQL)
        .host(System.getProperty(TestEnvironment.POSTGRESQL_HOST_PROPERTY))
        .port(System.getProperty(TestEnvironment.POSTGRESQL_PORT_PROPERTY))
        .instanceName(TestEnvironment.POSTGRESQL_DATABASE_NAME_DEFAULT)
        .build();
    try (final Connection connection = DriverManager.getConnection(jdbcUrl,
        System.getProperty(TestEnvironment.POSTGRESQL_USER_PROPERTY),
        System.getProperty(TestEnvironment.POSTGRESQL_PASSWORD_PROPERTY))) {
      try (final Statement statement = connection.createStatement()) {
        connection.setAutoCommit(true);
        // create tenant database
        statement.execute("CREATE DATABASE " + identifier);
        // insert tenant connection info in management table
        try (final ResultSet resultSet = statement.executeQuery("SELECT * FROM tenants WHERE identifier = '" + identifier + "'")) {
          if (resultSet.next()
              && resultSet.getInt(1) == 0) {
            final PostgreSQLTenant postgreSQLTenant = new PostgreSQLTenant();
            postgreSQLTenant.setIdentifier(identifier);
            postgreSQLTenant.setDriverClass(System.getProperty(TestEnvironment.POSTGRESQL_DRIVER_CLASS_PROPERTY));
            postgreSQLTenant.setDatabaseName(identifier);
            postgreSQLTenant.setHost(System.getProperty(TestEnvironment.POSTGRESQL_HOST_PROPERTY));
            postgreSQLTenant.setPort(System.getProperty(TestEnvironment.POSTGRESQL_PORT_PROPERTY));
            postgreSQLTenant.setUser(System.getProperty(TestEnvironment.POSTGRESQL_USER_PROPERTY));
            postgreSQLTenant.setPassword(System.getProperty(TestEnvironment.POSTGRESQL_PASSWORD_PROPERTY));
            postgreSQLTenant.insert(connection);
          }
        }
      }
    } catch (final SQLException ex) {
      ex.printStackTrace();
    }
  }
}