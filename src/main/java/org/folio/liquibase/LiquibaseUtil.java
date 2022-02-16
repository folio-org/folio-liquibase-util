package org.folio.liquibase;

import io.vertx.core.Vertx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.folio.rest.persist.PostgresClient;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static java.lang.String.format;
import static org.folio.liquibase.SingleConnectionProvider.getConnection;

/**
 * Util class to manage liquibase scripting
 */
public class LiquibaseUtil {
  private static final Logger LOGGER = LogManager.getLogger();
  private static final String CHANGELOG_MODULE_PATH = "liquibase/module/changelog.xml";
  private static final String CHANGELOG_TENANT_PATH = "liquibase/tenant/changelog.xml";

  private LiquibaseUtil() {}

  /**
   * Performs initialization for module configuration schema: - creates schema for
   * module configuration - runs scripts to fill module schema
   *
   * @param vertx vertx instance
   * @param moduleConfigSchema module configuration schema name
   */
  public static void initializeSchemaForModule(Vertx vertx, String moduleConfigSchema) {
    LOGGER.info(format("Initializing schema %s for the module", moduleConfigSchema));
    try (Connection connection = getConnection(vertx)) {
      createModuleConfigurationSchema(connection, moduleConfigSchema);
      runScripts(moduleConfigSchema, connection, CHANGELOG_MODULE_PATH);
      LOGGER.info("Schema is initialized for the module");
    } catch (SQLException | LiquibaseException e) {
      // convert checked exceptions into unchecked exceptions
      throw new RuntimeException(e);
    }
  }

  /**
   * Performs initialization for tenant schema: - runs scripts to fill tenant
   * schema in
   *
   * @param vertx  vertx instance
   * @param tenant given tenant for which database schema has to be initialized
   */
  public static void initializeSchemaForTenant(Vertx vertx, String tenant) {
    String schemaName = PostgresClient.convertToPsqlStandard(tenant);
    LOGGER.info(format("Initializing schema %s for tenant %s", schemaName, tenant));
    try (Connection connection = getConnection(vertx, tenant)) {
      runScripts(schemaName, connection, CHANGELOG_TENANT_PATH);
      LOGGER.info("Schema is initialized for tenant " + tenant);
    } catch (SQLException | LiquibaseException e) {
      // convert checked exceptions into unchecked exceptions
      throw new RuntimeException(e);
    }
  }

  /**
   * Runs scripts for given change log in a scope of given schema
   *
   * @param schemaName    schema name
   * @param connection    connection to the underlying database
   * @param changelogPath path to changelog file in the module classpath
   * @throws LiquibaseException if database access error occurs
   */
  private static void runScripts(String schemaName, Connection connection, String changelogPath) throws LiquibaseException {
    Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
    database.setDefaultSchemaName(schemaName);
    try (Liquibase liquibase = new Liquibase(changelogPath, new ClassLoaderResourceAccessor(), database)) {
      liquibase.update(new Contexts());
    }
    // liquibase.close automatically closes database
  }

  /**
   * Creates module configuration schema
   *
   * @param connection connection to the underlying database
   * @param moduleConfigSchema module configuration schema name
   * @throws SQLException if query execution error occurs
   */
  private static void createModuleConfigurationSchema(Connection connection, String moduleConfigSchema) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      String sql = format("create schema if not exists %s", moduleConfigSchema);
      statement.execute(sql);
    }
  }
}
