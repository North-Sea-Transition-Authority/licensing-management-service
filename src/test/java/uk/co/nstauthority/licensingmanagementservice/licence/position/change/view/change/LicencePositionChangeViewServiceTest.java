package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.operations.LicencePositionChangeOperation;

@ExtendWith(MockitoExtension.class)
class LicencePositionChangeViewServiceTest {

  private static final int JOINING_ID = 100;
  private static final int WITHDRAWING_ID = 200;
  private static final String JOINING_NAME = "Joining Org Ltd";
  private static final String WITHDRAWING_NAME = "Withdrawing Org Ltd";

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @InjectMocks
  private LicencePositionChangeViewService licencePositionChangeViewService;

  @Test
  void getChangeViews_filtersChangesNotOnCurrentPosition() {
    var currentLicencePosition = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();
    var otherLicencePosition = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();

    var otherChange = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(otherLicencePosition)
        .withOperations(List.of(LicencePositionChangeOperation.newAdministratorChange().withOperator(JOINING_ID).build()))
        .build();

    var result = licencePositionChangeViewService.getChangeViews(
        currentLicencePosition, List.of(otherLicencePosition, currentLicencePosition), List.of(otherChange));

    assertThat(result).isEmpty();
  }

  @Test
  void buildAdministratorChange() {
    var currentLicencePosition = LicencePositionTestUtil.newBuilder().build();
    var previousLicencePosition = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();

    var currentChange = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(currentLicencePosition)
        .withOperations(List.of(LicencePositionChangeOperation.newAdministratorChange().withOperator(JOINING_ID).build()))
        .build();

    var previousChange = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(previousLicencePosition)
        .withOperations(List.of(LicencePositionChangeOperation.newAdministratorChange().withOperator(WITHDRAWING_ID).build()))
        .build();

    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(JOINING_ID, WITHDRAWING_ID)))
        .thenReturn(Map.of(JOINING_ID, JOINING_NAME, WITHDRAWING_ID, WITHDRAWING_NAME));

    var result = licencePositionChangeViewService.getChangeViews(
        currentLicencePosition,
        List.of(previousLicencePosition, currentLicencePosition),
        List.of(previousChange, currentChange)
    );

    assertThat(result)
        .hasSize(1)
        .extractingByKey(LicencePositionChangeOperation.LICENCE_ADMINISTRATOR)
        .isInstanceOf(AdministratorChangeView.class)
        .extracting(
            licencePositionChangeView -> ((AdministratorChangeView) licencePositionChangeView).withdrawingOrganisationName(),
            licencePositionChangeView -> ((AdministratorChangeView) licencePositionChangeView).joiningOrganisationName()
        )
        .containsExactly(WITHDRAWING_NAME, JOINING_NAME);
  }

  @Test
  void buildAdministratorChange_noPriorChange() {
    var currentLicencePosition = LicencePositionTestUtil.newBuilder().build();

    var currentChange = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(currentLicencePosition)
        .withOperations(List.of(LicencePositionChangeOperation.newAdministratorChange().withOperator(JOINING_ID).build()))
        .build();

    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(JOINING_ID)))
        .thenReturn(Map.of(JOINING_ID, JOINING_NAME));

    var result = licencePositionChangeViewService.getChangeViews(currentLicencePosition, List.of(currentLicencePosition), List.of(currentChange));

    assertThat(result)
        .hasSize(1)
        .extractingByKey(LicencePositionChangeOperation.LICENCE_ADMINISTRATOR)
        .isInstanceOf(AdministratorChangeView.class)
        .extracting(
            licencePositionChangeView -> ((AdministratorChangeView) licencePositionChangeView).withdrawingOrganisationName(),
            licencePositionChangeView -> ((AdministratorChangeView) licencePositionChangeView).joiningOrganisationName()
        )
        .containsExactly(null, JOINING_NAME);
  }
}