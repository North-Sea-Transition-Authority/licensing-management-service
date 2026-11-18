package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.storageoperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.CorrectionPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.OrganisationChangeFormValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.OrganisationChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.form.ChangeFormTarget;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.StorageOperatorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.storageoperator.StorageOperatorChangeContext;

@ExtendWith(MockitoExtension.class)
class StorageOperatorChangeFormHandlerTest {

  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withLicence(LicenceTestUtil.builder().build())
      .build();
  private static final LicencePosition POSITION = LicencePositionTestUtil.newBuilder()
      .withId(POSITION_ID)
      .build();
  private static final LicencePositionChange LIVE_CHANGE = LicencePositionChangeTestUtil.newBuilder()
      .withId(UUID.randomUUID())
      .withLicencePosition(POSITION)
      .build();

  @Mock
  private OrganisationChangeService organisationChangeService;

  @Mock
  private OrganisationChangeFormValidator organisationChangeFormValidator;

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @InjectMocks
  private StorageOperatorChangeFormHandler storageOperatorChangeFormHandler;

  @Test
  void formPage() {
    when(organisationChangeService.getOrganisationChangeContext(CORRECTION, POSITION_ID, StorageOperatorOperation.class))
        .thenReturn(new StorageOperatorChangeContext(123, 456, "Current operator", "Previous operator"));
    when(organisationUnitQueryService.getOrganisationUnitSelectOption("123")).thenReturn(Map.of("123", "Current operator"));

    var result = storageOperatorChangeFormHandler.formPage(
        new ChangeFormTarget.LiveChange(CORRECTION, POSITION, LIVE_CHANGE)
    );

    var expected = new ModelAndView("lms/licence/correction/change/addOrganisationChange")
        .addObject("pageTitle", "Change storage operator")
        .addObject("operatorType", "storage operator")
        .addObject("previousOperatorName", "Previous operator")
        .addObject("form", new StorageOperatorChangeForm("123"))
        .addObject("preselectedOperator", Map.of("123", "Current operator"))
        .addObject(
            "organisationUnitsUrl",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null))
        );
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void save_whenNewChange_thenAddsStorageOperatorChange() {
    var correctionPosition = new CorrectionPosition.Executed(CORRECTION, POSITION);

    storageOperatorChangeFormHandler.save(
        new ChangeFormTarget.NewChange(correctionPosition),
        new StorageOperatorChangeForm("789")
    );

    verify(organisationChangeService).addOrganisationChange(
        correctionPosition,
        StorageOperatorOperation.class,
        LicenceOperation.newStorageOperatorOperation().withOperator(789).build()
    );
  }

  @Test
  void save_whenLiveChange_thenCorrectsChange() {
    storageOperatorChangeFormHandler.save(
        new ChangeFormTarget.LiveChange(CORRECTION, POSITION, LIVE_CHANGE),
        new StorageOperatorChangeForm("789")
    );

    verify(organisationChangeService).correctOrganisationChange(
        POSITION,
        CORRECTION,
        StorageOperatorOperation.class,
        LIVE_CHANGE.getId().toString(),
        LicenceOperation.newStorageOperatorOperation().withOperator(789).build()
    );
  }
}
