package uk.co.nstauthority.licensingmanagementservice.migration.pears.history;

/**
 * The COMPANY_LIST_TYPE values a consortium list operation carries. In PEARS the licence
 * administrator is one of these lists rather than an operation type of its own, and it is the only
 * one carried across today.
 */
public final class PearsCompanyListType {

  public static final String LICENCE_ADMINISTRATOR = "LICENCE_ADMINISTRATOR";

  public static final String BENEFICIAL_INTEREST = "BENEFICIAL_INTEREST";

  public static final String LICENSEE = "LICENSEE";

  public static final String SUBAREA_OPERATOR = "SUBAREA_OPERATOR";

  private PearsCompanyListType() {
    throw new IllegalStateException("Utility class should not be instantiated.");
  }
}
