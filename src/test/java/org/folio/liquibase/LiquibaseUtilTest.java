package org.folio.liquibase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

    PostgresClient.setIsEmbedded(true);

    PostgresClient postgresClient = PostgresClient.getInstance(vertx);

    postgresClient.startEmbeddedPostgres();

    postgresClient.select("SELECT 1", context.asyncAssertSuccess());
  }

  @Test
  public void testInitializeSchemaForModule(final TestContext context) {
    Async async = context.async();
    PostgresClient postgresClient = PostgresClient.getInstance(vertx);

    String schemaQueryTemplate = "SELECT schema_name FROM information_schema.schemata WHERE schema_name = '%s';";

    vertx.executeBlocking(blockingFuture -> {
      LiquibaseUtil.initializeSchemaForModule(vertx, MODULE_CONFIGURATION_SCHEMA);
      blockingFuture.complete();
    }, result -> {
      String schemaQuery = String.format(schemaQueryTemplate, MODULE_CONFIGURATION_SCHEMA);
      postgresClient.select(schemaQuery,  res -> {
        context.assertTrue(res.succeeded());
        context.assertEquals(1, res.result().getNumRows());
        async.complete();
      });
    });
  }

  @Test
  public void testInitializeSchemaForTenant(final TestContext context) {
    Async async = context.async();
    PostgresClient postgresClient = PostgresClient.getInstance(vertx);

    String schemaName = PostgresClient.convertToPsqlStandard(TENANT_ID);

    String schemaTablesQueryTemplate = "SELECT table_name FROM information_schema.tables WHERE table_schema = '%s';";
    String schemaColumnsQueryTemplate = "SELECT * FROM information_schema.columns WHERE table_schema = '%s' and table_name = '%s';";

    vertx.executeBlocking(blockingFuture -> {
      LiquibaseUtil.initializeSchemaForTenant(vertx, TENANT_ID);
      blockingFuture.complete();
    }, result -> {

      String schemaTablesQuery = String.format(schemaTablesQueryTemplate, schemaName);
      postgresClient.select(schemaTablesQuery, tableRes -> {
        context.assertTrue(tableRes.succeeded());
        List<String> actualTables = tableRes.result().getRows().stream().map(row -> row.getString("table_name")).collect(Collectors.toList());
        List<String> expectedTables = getExpectedTables();
        Collections.sort(actualTables);
        Collections.sort(expectedTables);
        context.assertEquals(expectedTables, actualTables);

        actualTables.forEach(tableName -> {

          String schemaColumnsQuery = String.format(schemaColumnsQueryTemplate, schemaName, tableName);
          postgresClient.select(schemaColumnsQuery, columnRes -> {
            context.assertTrue(columnRes.succeeded());
            List<String> actualColumns = columnRes.result().getRows().stream().map(row -> row.getString("column_name")).collect(Collectors.toList());
            List<String> expectedColumns = getExpectedColumns(tableName);
            Collections.sort(actualColumns);
            Collections.sort(expectedColumns);
            context.assertEquals(expectedColumns, actualColumns);
            async.complete();
          });

        });
      });
    });
  }

  @AfterClass
  public static void tearDownClass(final TestContext context) {
    Async async = context.async();
    vertx.close(context.asyncAssertSuccess(res -> {
      PostgresClient.stopEmbeddedPostgres();
      async.complete();
    }));
  }

  private List<String> getExpectedTables() {
    return Arrays.asList(new String[] { 
      "databasechangeloglock",
      "databasechangelog",
      "audit_message",
      "audit_message_payload",
      "user"
     });
  }

  private List<String> getExpectedColumns(String tableName) {
    List<String> expectedColumns;
    switch(tableName) {
      case "databasechangeloglock":
        expectedColumns = Arrays.asList(new String[] {
          "id",
          "locked",
          "lockgranted",
          "lockedby"
        });
      break;
      case "databasechangelog":
        expectedColumns = Arrays.asList(new String[] {
          "id",
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
          "deployment_id"
        });
      break;
      case "audit_message":
        expectedColumns = Arrays.asList(new String[] {
          "id",
          "event_id",
          "event_type",
          "correlation_id",
          "tenant_id",
          "created_by",
          "audit_date",
          "state",
          "published_by",
          "error_message"
        });
      break;
      case "audit_message_payload":
        expectedColumns = Arrays.asList(new String[] {
          "event_id",
          "content"
        });
      break;
      case "user":
        expectedColumns = Arrays.asList(new String[] {
          "username",
          "password",
          "token"
        });
      break;
      default: 
        expectedColumns = new ArrayList<>();
      break;
    }
    return expectedColumns;
  }

}