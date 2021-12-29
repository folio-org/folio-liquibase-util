package org.folio.liquibase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.folio.postgres.testing.PostgresTesterContainer;
import org.folio.rest.persist.PostgresClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

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

    Async async = context.async();

    PostgresClient.setPostgresTester(new PostgresTesterContainer());

    PostgresClient postgresClient = PostgresClient.getInstance(vertx);

    postgresClient.select("SELECT 1", context.asyncAssertSuccess());

    // create a user for tenant
    String schemaName = PostgresClient.convertToPsqlStandard(TENANT_ID);
    String elevateTenantUserQueryTemplate = "CREATE USER %s WITH LOGIN";
    String elevateTenantUserQuery = String.format(elevateTenantUserQueryTemplate, schemaName);
    postgresClient.execute(elevateTenantUserQuery,  res -> {
      context.assertTrue(res.succeeded());
      async.complete();
    });
  }

  @Test
  public void testInitializeSchemaForModule(final TestContext context) {
    PostgresClient postgresClient = PostgresClient.getInstance(vertx);

    String schemaQueryTemplate = "SELECT schema_name FROM information_schema.schemata WHERE schema_name = '%s';";

    // initialize configuration schema for module
    vertx.executeBlocking(blockingFuture -> {
      LiquibaseUtil.initializeSchemaForModule(vertx, MODULE_CONFIGURATION_SCHEMA);
      blockingFuture.complete();
    }, result -> {
      // check if module configuration schema was created
      String schemaQuery = String.format(schemaQueryTemplate, MODULE_CONFIGURATION_SCHEMA);
      postgresClient.select(schemaQuery,  res -> {
        context.assertTrue(res.succeeded());
        context.assertEquals(1, res.result().rowCount());
      });
    });
  }

  @Test
  public void testInitializeSchemaForTenant(final TestContext context) {
    PostgresClient postgresClient = PostgresClient.getInstance(vertx);

    String schemaName = PostgresClient.convertToPsqlStandard(TENANT_ID);

    String schemaTablesQueryTemplate = "SELECT table_name FROM information_schema.tables WHERE table_schema = '%s';";
    String schemaColumnsQueryTemplate = "SELECT column_name FROM information_schema.columns WHERE table_schema = '%s' and table_name = '%s';";

    // initialize schema for tenant
    vertx.executeBlocking(blockingFuture -> {
      LiquibaseUtil.initializeSchemaForTenant(vertx, TENANT_ID);
      blockingFuture.complete();
    }, result -> {

      // check if all tenant schemata were created
      String schemaTablesQuery = String.format(schemaTablesQueryTemplate, schemaName);
      postgresClient.select(schemaTablesQuery, tableRes -> {
        context.assertTrue(tableRes.succeeded());
        List<String> actualTables = new ArrayList<>();
        tableRes.result().forEach(row -> actualTables.add(row.getString("table_name")));
        List<String> expectedTables = getExpectedTables();
        Collections.sort(actualTables);
        Collections.sort(expectedTables);
        context.assertEquals(expectedTables, actualTables);

        actualTables.forEach(tableName -> {

          // check if all schema columns were as expected
          String schemaColumnsQuery = String.format(schemaColumnsQueryTemplate, schemaName, tableName);
          postgresClient.select(schemaColumnsQuery, columnRes -> {
            context.assertTrue(columnRes.succeeded());
            List<String> actualColumns = new ArrayList<>();
            columnRes.result().forEach(row -> actualColumns.add(row.getString("column_name")));
            List<String> expectedColumns = getExpectedColumns(tableName);
            Collections.sort(actualColumns);
            Collections.sort(expectedColumns);
            context.assertEquals(expectedColumns, actualColumns);
          });
        });
      });
    });
  }

  @AfterClass
  public static void tearDownClass(final TestContext context) {
    Async async = context.async();
    PostgresClient postgresClient = PostgresClient.getInstance(vertx);

    // drop user for tenant
    String dropUserQueryTemplate = "DROP USER %s";
    String schemaName = PostgresClient.convertToPsqlStandard(TENANT_ID);
    String dropUserQuery = String.format(dropUserQueryTemplate, schemaName);
    postgresClient.execute(dropUserQuery, res -> {
      context.assertTrue(res.succeeded());

      // close vertx
      vertx.close(context.asyncAssertSuccess(closRes -> {
        PostgresClient.stopPostgresTester();
        async.complete();
      }));
    });
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
