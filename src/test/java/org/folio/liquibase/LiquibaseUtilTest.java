package org.folio.liquibase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.folio.postgres.testing.PostgresTesterContainer;
import org.folio.rest.persist.PostgresClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.sqlclient.Tuple;

/**
 * Integration tests for LiquibaseUtil using embedded Postgres.
 */
@RunWith(VertxUnitRunner.class)
public class LiquibaseUtilTest {

  private static final String MODULE_CONFIGURATION_SCHEMA = "test_config";

  private static final String TENANT_ID = "diku";

  private static Vertx vertx;

  @BeforeClass
  public static void setUpClass(final TestContext context) throws Exception {
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
    .onComplete(context.asyncAssertSuccess());
  }

  @Test
  public void testInitializeSchemaForModule(final TestContext context) {
    LiquibaseUtil.initializeSchemaForModule(vertx, MODULE_CONFIGURATION_SCHEMA);
    String query = "SELECT count(*) as count FROM information_schema.schemata WHERE schema_name = $1";
    PostgresClient.getInstance(vertx).selectSingle(query, Tuple.of(MODULE_CONFIGURATION_SCHEMA))
    .onComplete(context.asyncAssertSuccess(row -> {
      assertThat(row.getInteger("count"), is(1));
    }));
  }

  @Test(expected = Exception.class)
  public void exceptionInInitializeSchemaForModule(final TestContext context) {
    LiquibaseUtil.initializeSchemaForModule(vertx, "invalid ' ");
  }

  @Test
  public void testInitializeSchemaForTenant(final TestContext context) {
    LiquibaseUtil.initializeSchemaForTenant(vertx, TENANT_ID);

    PostgresClient postgresClient = PostgresClient.getInstance(vertx);

    String schemaName = PostgresClient.convertToPsqlStandard(TENANT_ID);

    // check if all tenant schemata were created
    String tablesQuery = "SELECT table_name FROM information_schema.tables WHERE table_schema = $1";
    String columnsQuery = "SELECT column_name FROM information_schema.columns WHERE table_schema = $1 and table_name = $2";

    postgresClient.execute(tablesQuery, Tuple.of(schemaName))
    .onComplete(context.asyncAssertSuccess(tableRes -> {
      List<String> actualTables = new ArrayList<>();
      tableRes.forEach(row -> actualTables.add(row.getString("table_name")));
      List<String> expectedTables = getExpectedTables();
      assertThat(actualTables, containsInAnyOrder(expectedTables.toArray()));

      actualTables.forEach(tableName -> {

        // check if all schema columns were as expected
        postgresClient.execute(columnsQuery, Tuple.of(schemaName, tableName))
        .onComplete(context.asyncAssertSuccess(columnRes -> {
          List<String> actualColumns = new ArrayList<>();
          columnRes.forEach(row -> actualColumns.add(row.getString("column_name")));
          List<String> expectedColumns = getExpectedColumns(tableName);
          assertThat(actualColumns, containsInAnyOrder(expectedColumns.toArray()));
        }));
      });
    }));
  }

  @Test(expected = Exception.class)
  public void exceptionInInitializeSchemaForTenant(final TestContext context) {
    LiquibaseUtil.initializeSchemaForTenant(vertx, "invalid ' ");
  }

  @AfterClass
  public static void tearDownClass(final TestContext context) {
    PostgresClient postgresClient = PostgresClient.getInstance(vertx);

    String schemaName = PostgresClient.convertToPsqlStandard(TENANT_ID);
    String dropSchemaQueryTemplate = "DROP SCHEMA IF EXISTS %s CASCADE";
    String dropSchemaQuery = String.format(dropSchemaQueryTemplate, schemaName);
    String dropUserQueryTemplate = "DROP ROLE IF EXISTS %s";
    String dropUserQuery = String.format(dropUserQueryTemplate, schemaName);
    postgresClient.execute(dropSchemaQuery)
    .compose(x -> postgresClient.execute(dropUserQuery))
    .onComplete(context.asyncAssertSuccess());
  }

  private List<String> getExpectedTables() {
    return Arrays.asList("databasechangeloglock",
      "databasechangelog",
      "audit_message",
      "audit_message_payload",
      "user");
  }

  private List<String> getExpectedColumns(String tableName) {
    List<String> expectedColumns;
    switch(tableName) {
      case "databasechangeloglock":
        expectedColumns = Arrays.asList("id",
          "locked",
          "lockgranted",
          "lockedby");
      break;
      case "databasechangelog":
        expectedColumns = Arrays.asList("id",
          "author",
          "filename",
          "dateexecuted",
          "orderexecuted",
          "exectype",
          "md5sum",
          "description",
          "comments",
          "tag",
          "liquibase",
          "contexts",
          "labels",
          "deployment_id");
      break;
      case "audit_message":
        expectedColumns = Arrays.asList("id",
          "event_id",
          "event_type",
          "correlation_id",
          "tenant_id",
          "created_by",
          "audit_date",
          "state",
          "published_by",
          "error_message");
      break;
      case "audit_message_payload":
        expectedColumns = Arrays.asList("event_id",
          "content");
      break;
      case "user":
        expectedColumns = Arrays.asList("username",
          "password",
          "token");
      break;
      default:
        expectedColumns = new ArrayList<>();
      break;
    }
    return expectedColumns;
  }

}
