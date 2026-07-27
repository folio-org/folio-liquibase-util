package org.folio.rest.tools.utils;

/**
 * This class is required for tests to get the module name and version.
 * The logic of getting these values is in {@link org.folio.rest.persist.PostgresClient}.
 */
public class ModuleName {

  private static final String MODULE_NAME = "folio_liquibase_util";
  private static final String MODULE_VERSION = "1.0.0"; // or the appropriate version

  public static String getModuleName() {
    return MODULE_NAME;
  }

  public static String getModuleVersion() {
    return MODULE_VERSION;
  }
}
