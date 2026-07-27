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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;

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
    var previousLicencePosition = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();

    var previousChronologicalPosition = ChronologicalPositionTestUtil.live(
        previousLicencePosition,
        LicenceOperation.newAdministratorChange().withOperator(JOINING_ID).build()
    );
    var currentChronologicalPosition = ChronologicalPositionTestUtil.live(currentLicencePosition);


    var result = licencePositionChangeViewService.getChangeViews(
        currentLicencePosition.getId(), List.of(previousChronologicalPosition, currentChronologicalPosition)
    );

    assertThat(result).isEmpty();
  }

  @Test
  void buildAdministratorChange() {
    var currentLicencePosition = LicencePositionTestUtil.newBuilder().build();
    var previousLicencePosition = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();

    var previousChronologicalPosition = ChronologicalPositionTestUtil.live(
        previousLicencePosition,
        LicenceOperation.newAdministratorChange().withOperator(WITHDRAWING_ID).build()
    );
    var currentChronologicalPosition = ChronologicalPositionTestUtil.live(
        currentLicencePosition,
        LicenceOperation.newAdministratorChange().withOperator(JOINING_ID).build()
    );

    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(JOINING_ID, WITHDRAWING_ID)))
        .thenReturn(Map.of(JOINING_ID, JOINING_NAME, WITHDRAWING_ID, WITHDRAWING_NAME));

    var result = licencePositionChangeViewService.getChangeViews(
        currentLicencePosition.getId(),
        List.of(previousChronologicalPosition, currentChronologicalPosition)
    );

    assertThat(result)
        .hasSize(1)
        .extractingByKey(LicenceOperation.LICENCE_ADMINISTRATOR)
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

    var currentChronologicalPosition = ChronologicalPositionTestUtil.live(
        currentLicencePosition,
        LicenceOperation.newAdministratorChange().withOperator(JOINING_ID).build()
    );

    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(JOINING_ID)))
        .thenReturn(Map.of(JOINING_ID, JOINING_NAME));

    var result = licencePositionChangeViewService.getChangeViews(currentLicencePosition.getId(), List.of(currentChronologicalPosition));

    assertThat(result)
        .hasSize(1)
        .extractingByKey(LicenceOperation.LICENCE_ADMINISTRATOR)
        .isInstanceOf(AdministratorChangeView.class)
        .extracting(
            licencePositionChangeView -> ((AdministratorChangeView) licencePositionChangeView).withdrawingOrganisationName(),
            licencePositionChangeView -> ((AdministratorChangeView) licencePositionChangeView).joiningOrganisationName()
        )
        .containsExactly(null, JOINING_NAME);
  }

  @Test
  void buildAdministratorChange_carriesChangeTypeFromCorrectionChange() {
    var currentLicencePosition = LicencePositionTestUtil.newBuilder().build();

    var correctionChange = new PositionChange(
        UUID.randomUUID().toString(),
        1,
        LicencePositionChangeType.ADD_CHANGE,
        List.of(LicenceOperation.newAdministratorChange().withOperator(JOINING_ID).build())
    );
    var currentChronologicalPosition = ChronologicalPosition.fromLicencePosition(
        currentLicencePosition, List.of(correctionChange));

    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(JOINING_ID)))
        .thenReturn(Map.of(JOINING_ID, JOINING_NAME));

    var result = licencePositionChangeViewService.getChangeViews(
        currentLicencePosition.getId(), List.of(currentChronologicalPosition));

    assertThat(result)
        .extractingByKey(LicenceOperation.LICENCE_ADMINISTRATOR)
        .isInstanceOf(AdministratorChangeView.class)
        .extracting(LicencePositionChangeView::changeType)
        .isEqualTo(LicencePositionChangeType.ADD_CHANGE);
  }
}