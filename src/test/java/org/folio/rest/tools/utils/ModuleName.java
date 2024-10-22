package org.folio.rest.tools.utils;

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
