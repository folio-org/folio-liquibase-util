package org.folio.liquibase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.vertx.core.Vertx;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.sqlclient.Tuple;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.folio.postgres.testing.PostgresTesterContainer;
import org.folio.rest.persist.PostgresClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Integration tests for LiquibaseUtil using embedded Postgres.
 */
@ExtendWith(VertxExtension.class)
class LiquibaseUtilTest {

  private static final String MODULE_CONFIGURATION_SCHEMA = "test_config";

  private static final String TENANT_ID = "diku";
  private static final String TABLES_QUERY =
    "SELECT table_name FROM information_schema.tables WHERE table_schema = $1";
  private static final String COLUMNS_QUERY =
    "SELECT column_name FROM information_schema.columns WHERE table_schema = $1 and table_name = $2";
  private static Vertx vertx;

  @BeforeAll
  static void setUpClass(final VertxTestContext context) throws Exception {
    vertx = Vertx.vertx();

    Class.forName("org.postgresql.Driver");

    PostgresClient.setPostgresTester(new PostgresTesterContainer());

    PostgresClient postgresClient = PostgresClient.getInstance(vertx);

    // create a user for tenant
    String schemaName = PostgresClient.convertToPsqlStandard(TENANT_ID);
    String elevateTenantUserQueryTemplate = "CREATE ROLE %s WITH PASSWORD '%s' LOGIN";
    String elevateTenantUserQuery = String.format(elevateTenantUserQueryTemplate, schemaName, TENANT_ID);
    String createSchemaTemplate = "CREATE SCHEMA %s AUTHORIZATION %s";
    String createSchema = String.format(createSchemaTemplate, schemaName, schemaName);
    postgresClient.select("SELECT 1")
      .compose(x -> postgresClient.execute(elevateTenantUserQuery))
      .compose(x -> postgresClient.execute(createSchema))
      .onComplete(context.succeedingThenComplete());
  }

  @Test
  void testInitializeSchemaForModule(final VertxTestContext context) {
    LiquibaseUtil.initializeSchemaForModule(vertx, MODULE_CONFIGURATION_SCHEMA);
    String query = "SELECT count(*) as count FROM information_schema.schemata WHERE schema_name = $1";
    PostgresClient.getInstance(vertx).selectSingle(query, Tuple.of(MODULE_CONFIGURATION_SCHEMA))
      .onComplete(context.succeeding(row -> context.verify(() -> {
        assertThat(row.getInteger("count"), is(1));
        context.completeNow();
      })));
  }

  @Test
  void exceptionInInitializeSchemaForModule() {
    assertThrows(Exception.class, () -> LiquibaseUtil.initializeSchemaForModule(vertx, "invalid ' "));
  }

  @Test
  void testInitializeSchemaForTenant(final VertxTestContext context) {
    LiquibaseUtil.initializeSchemaForTenant(vertx, TENANT_ID);

    PostgresClient postgresClient = PostgresClient.getInstance(vertx);
    String schemaName = PostgresClient.convertToPsqlStandard(TENANT_ID);

    // check if all tenant schemata were created
    postgresClient.execute(TABLES_QUERY, Tuple.of(schemaName))
      .onComplete(context.succeeding(tableRes -> {
        List<String> actualTables = new ArrayList<>();
        tableRes.forEach(row -> actualTables.add(row.getString("table_name")));
        context.verify(() -> assertThat(actualTables, containsInAnyOrder(getExpectedTables().toArray())));

        Checkpoint checkpoint = context.checkpoint(actualTables.size());
        actualTables.forEach(tableName ->
          verifyColumns(postgresClient, schemaName, tableName, checkpoint, context));
      }));
  }

  @Test
  void exceptionInInitializeSchemaForTenant() {
    assertThrows(Exception.class, () -> LiquibaseUtil.initializeSchemaForTenant(vertx, "invalid ' "));
  }

  @AfterAll
  static void tearDownClass(final VertxTestContext context) {
    PostgresClient postgresClient = PostgresClient.getInstance(vertx);

    String schemaName = PostgresClient.convertToPsqlStandard(TENANT_ID);
    String dropSchemaQueryTemplate = "DROP SCHEMA IF EXISTS %s CASCADE";
    String dropSchemaQuery = String.format(dropSchemaQueryTemplate, schemaName);
    String dropUserQueryTemplate = "DROP ROLE IF EXISTS %s";
    String dropUserQuery = String.format(dropUserQueryTemplate, schemaName);
    postgresClient.execute(dropSchemaQuery)
      .compose(x -> postgresClient.execute(dropUserQuery))
      .onComplete(context.succeedingThenComplete());
  }

  // check if all schema columns were as expected
  private void verifyColumns(PostgresClient postgresClient, String schemaName, String tableName,
                             Checkpoint checkpoint, VertxTestContext context) {
    postgresClient.execute(COLUMNS_QUERY, Tuple.of(schemaName, tableName))
      .onComplete(context.succeeding(columnRes -> {
        List<String> actualColumns = new ArrayList<>();
        columnRes.forEach(row -> actualColumns.add(row.getString("column_name")));
        List<String> expectedColumns = getExpectedColumns(tableName);
        context.verify(() -> {
          assertThat(actualColumns, containsInAnyOrder(expectedColumns.toArray()));
          checkpoint.flag();
        });
      }));
  }

  private List<String> getExpectedTables() {
    return Arrays.asList("databasechangeloglock", "databasechangelog", "audit_message", "audit_message_payload",
      "user");
  }

  private List<String> getExpectedColumns(String tableName) {
    return switch (tableName) {
      case "databasechangeloglock" -> Arrays.asList("id", "locked", "lockgranted", "lockedby");
      case "databasechangelog" -> Arrays.asList("id", "author", "filename", "dateexecuted", "orderexecuted",
        "exectype", "md5sum", "description", "comments", "tag", "liquibase", "contexts", "labels", "deployment_id");
      case "audit_message" -> Arrays.asList("id", "event_id", "event_type", "correlation_id", "tenant_id",
        "created_by", "audit_date", "state", "published_by", "error_message");
      case "audit_message_payload" -> Arrays.asList("event_id", "content");
      case "user" -> Arrays.asList("username", "password", "token");
      default -> new ArrayList<>();
    };
  }
}
