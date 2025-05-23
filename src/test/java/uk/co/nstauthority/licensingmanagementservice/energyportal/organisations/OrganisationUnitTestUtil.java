package uk.co.nstauthority.licensingmanagementservice.energyportal.organisations;

import java.util.List;
import uk.co.fivium.energyportalapi.generated.types.Address;
import uk.co.fivium.energyportalapi.generated.types.OrganisationUnit;

public class OrganisationUnitTestUtil {

  public static final Integer ORG_UNIT_ID_1 = 1;
  public static final String ORG_UNIT_NAME_1 = "Org unit 1";
  public static final Integer ORG_UNIT_ID_2 = 2;
  public static final String ORG_UNIT_NAME_2 = "Org unit 2";
  public static final Integer ORG_UNIT_ID_3 = 3;
  public static final String ORG_UNIT_NAME_3 = "Org unit 3";

  public OrganisationUnitTestUtil() {
    throw new IllegalStateException("Cannot instantiate static util class");
  }

  public static final OrganisationUnit ORG_UNIT_1 =
      OrganisationUnit.newBuilder()
          .organisationUnitId(ORG_UNIT_ID_1)
          .name(ORG_UNIT_NAME_1)
          .registeredNumber("registered number 1")
          .foreignRegisteredNumber("foreign registered number 1")
          .isDuplicate(false)
          .isActive(true)
          .registeredAddress(new Address("registered address"))
          .build();

  public static final OrganisationUnitJson ORG_UNIT_1_JSON =
      new OrganisationUnitJson(
          ORG_UNIT_1.getOrganisationUnitId(),
          ORG_UNIT_1.getName()
      );

  public static final OrganisationUnit ORG_UNIT_2 =
      OrganisationUnit.newBuilder()
          .organisationUnitId(ORG_UNIT_ID_2)
          .name(ORG_UNIT_NAME_2)
          .registeredNumber("registered number 2")
          .foreignRegisteredNumber("foreign registered number 2")
          .isDuplicate(false)
          .isActive(true)
          .registeredAddress(new Address("Registered Address 2"))
          .build();

  public static final OrganisationUnitJson ORG_UNIT_2_JSON =
      new OrganisationUnitJson(
          ORG_UNIT_2.getOrganisationUnitId(),
          ORG_UNIT_2.getName()
      );

  public static final OrganisationUnit ORG_UNIT_3 =
      OrganisationUnit.newBuilder()
          .organisationUnitId(ORG_UNIT_ID_3)
          .name(ORG_UNIT_NAME_3)
          .registeredNumber(null)
          .foreignRegisteredNumber("foreign registered number 3")
          .isDuplicate(false)
          .isActive(true)
          .build();

  public static final OrganisationUnitJson ORG_UNIT_3_JSON =
      new OrganisationUnitJson(
          ORG_UNIT_3.getOrganisationUnitId(),
          ORG_UNIT_3.getName()
      );

  public static List<OrganisationUnit> organisationUnits = List.of(ORG_UNIT_1, ORG_UNIT_2);
}
