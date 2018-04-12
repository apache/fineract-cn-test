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
package org.apache.fineract.cn.test.fixture.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.schemabuilder.SchemaBuilder;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import org.apache.fineract.cn.test.env.TestEnvironment;
import org.apache.fineract.cn.test.fixture.DataStoreTenantInitializer;
import org.apache.fineract.cn.cassandra.util.ContactPointUtils;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;

import java.util.concurrent.TimeUnit;

@SuppressWarnings({"WeakerAccess", "unused"})
public final class CassandraInitializer extends DataStoreTenantInitializer {

  private final boolean useExistingDB;
  private Cluster cluster;

  public CassandraInitializer() {
    this(false);
  }
  public CassandraInitializer(boolean useExistingDB) {
    super();this.useExistingDB = useExistingDB;
  }

  @Override
  public void initialize() throws Exception {
    final Cluster.Builder clusterBuilder = new Cluster.Builder()
        .withClusterName(System.getProperty(TestEnvironment.CASSANDRA_CLUSTER_NAME_PROPERTY));
    ContactPointUtils.process(clusterBuilder, System.getProperty(TestEnvironment.CASSANDRA_CONTACT_POINTS_PROPERTY));
    cluster = clusterBuilder.build();
    setup();
  }

  @Override
  public void initializeTenant(final String tenantName) {
    createKeyspaceTenant(tenantName);
  }

  @Override
  public void finish() {
    if (cluster != null)
      cluster.close();

    if (!useExistingDB) this.tearDown();
  }

  private void setup() throws Exception {
    if (!useExistingDB) {
      startEmbeddedCassandra();
      createKeyspaceSeshat();
    }
  }

  private void tearDown() {
    EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();
  }

  private void startEmbeddedCassandra() throws Exception {
    EmbeddedCassandraServerHelper.startEmbeddedCassandra(TimeUnit.SECONDS.toMillis(30L));
  }

  private void createKeyspaceSeshat() {
    try (final Session session = cluster.connect()) {
      // create meta keyspace seshat
      session.execute("CREATE KEYSPACE " +
          System.getProperty(TestEnvironment.CASSANDRA_META_KEYSPACE_PROPERTY) +
          " WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1}");
      // create needed tenant management table
      final String createTenantsTable = SchemaBuilder.createTable(
          System.getProperty(TestEnvironment.CASSANDRA_META_KEYSPACE_PROPERTY), "tenants")
          .addPartitionKey("identifier", DataType.text())
          .addColumn("cluster_name", DataType.text())
          .addColumn("contact_points", DataType.text())
          .addColumn("keyspace_name", DataType.text())
          .addColumn("replication_type", DataType.text())
          .addColumn("replicas", DataType.text())
          .addColumn("name", DataType.text())
          .addColumn("description", DataType.text())
          .buildInternal();
      session.execute(createTenantsTable);
    }
  }

  private void createKeyspaceTenant(final String identifier) {
    try (final Session session = cluster.connect()) {
      // create tenant keyspace
      session.execute("CREATE KEYSPACE " + identifier
          + " WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1}");
      // create needed command source table for the new tenant
      final String createCommandSourceTable = SchemaBuilder.createTable(identifier, "command_source")
          .addPartitionKey("source", DataType.text())
          .addPartitionKey("bucket", DataType.text())
          .addClusteringColumn("created_on", DataType.timestamp())
          .addColumn("command", DataType.text())
          .addColumn("processed", DataType.cboolean())
          .addColumn("failed", DataType.cboolean())
          .addColumn("failure_message", DataType.text())
          .buildInternal();
      session.execute(createCommandSourceTable);
      // insert tenant connection info in management table
      session.execute("USE " + System.getProperty(TestEnvironment.CASSANDRA_META_KEYSPACE_PROPERTY));
      final MappingManager mappingManager = new MappingManager(session);
      final CassandraTenant cassandraTenant = new CassandraTenant();
      cassandraTenant.setIdentifier(identifier);
      cassandraTenant.setClusterName(System.getProperty(TestEnvironment.CASSANDRA_CLUSTER_NAME_PROPERTY));
      cassandraTenant.setContactPoints(System.getProperty(TestEnvironment.CASSANDRA_CONTACT_POINTS_PROPERTY));
      cassandraTenant.setKeyspaceName(identifier);
      cassandraTenant.setReplicationType("Simple");
      cassandraTenant.setReplicas("1");
      cassandraTenant.setName(identifier);
      final Mapper<CassandraTenant> cassandraTenantMapper = mappingManager.mapper(CassandraTenant.class);
      cassandraTenantMapper.save(cassandraTenant);
    }
  }
}
