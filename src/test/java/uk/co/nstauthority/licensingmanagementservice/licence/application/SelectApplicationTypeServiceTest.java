package uk.co.nstauthority.licensingmanagementservice.licence.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.startjourney.StartContinuationApplicationController;
import uk.co.nstauthority.licensingmanagementservice.licence.rules.LicenceTypeRulesResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.startjourney.StartScheduleWorkProgrammeApplicationJourneyController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.FeatureFlagService;

@ExtendWith(MockitoExtension.class)
class SelectApplicationTypeServiceTest {

  private static final List<ApplicationType> ALL_APPLICATION_TYPES = Arrays.asList(ApplicationType.values());

  @Mock
  private FeatureFlagService featureFlagService;

  @Mock
  private LicenceTypeRulesResolver licenceTypeRulesResolver;

  @InjectMocks
  private SelectApplicationTypeService selectApplicationTypeService;

  @Test
  void getAvailableApplicationTypes_whenProductionLicenceType_thenScheduleAndContinuationReturned() {
    givenAllApplicationTypesEnabled();
    givenScheduleLicenceTypes();
    givenContinuationLicenceTypes();

    var result = selectApplicationTypeService.getAvailableApplicationTypes(LicenceType.SEAWARD_PRODUCTION);

    assertThat(result).containsExactly(
        ApplicationType.SCHEDULE_AMENDMENT_APPLICATION,
        ApplicationType.CONTINUATION_APPLICATION
    );
  }

  @Test
  void getAvailableApplicationTypes_whenCarbonStorageLicenceType_thenOnlyScheduleReturned() {
    givenAllApplicationTypesEnabled();
    givenScheduleLicenceTypes();
    givenContinuationLicenceTypes();

    var result = selectApplicationTypeService.getAvailableApplicationTypes(LicenceType.CARBON_STORAGE);

    assertThat(result).containsExactly(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION);
  }

  @Test
  void getAvailableApplicationTypes_whenApplicationTypeNotEnabled_thenNotReturned() {
    when(featureFlagService.filterEnabled(ALL_APPLICATION_TYPES))
        .thenReturn(List.of(ApplicationType.CONTINUATION_APPLICATION));
    givenContinuationLicenceTypes();

    var result = selectApplicationTypeService.getAvailableApplicationTypes(LicenceType.SEAWARD_PRODUCTION);

    assertThat(result).containsExactly(ApplicationType.CONTINUATION_APPLICATION);
  }

  @Test
  void getAvailableLicenceTypes() {
    givenAllApplicationTypesEnabled();
    givenScheduleLicenceTypes();
    givenContinuationLicenceTypes();

    var result = selectApplicationTypeService.getAvailableLicenceTypes();

    assertThat(result).containsExactly(
        LicenceType.CARBON_STORAGE,
        LicenceType.LANDWARD_PRODUCTION,
        LicenceType.SEAWARD_PRODUCTION
    );
  }

  @Test
  void getLicenceTypeOrThrow_whenLicenceTypeHasApplicationTypes_thenLicenceTypeReturned() {
    givenAllApplicationTypesEnabled();
    givenScheduleLicenceTypes();
    givenContinuationLicenceTypes();

    var result = selectApplicationTypeService.getLicenceTypeOrThrow(LicenceType.CARBON_STORAGE.getUrlSlug());

    assertThat(result).isEqualTo(LicenceType.CARBON_STORAGE);
  }

  @Test
  void getLicenceTypeOrThrow_whenLicenceTypeHasNoApplicationTypes_thenNotFound() {
    givenAllApplicationTypesEnabled();
    givenScheduleLicenceTypes();
    givenContinuationLicenceTypes();
    var licenceTypeSlug = LicenceType.SEAWARD_EXPLORATION.getUrlSlug();

    assertThatThrownBy(() -> selectApplicationTypeService.getLicenceTypeOrThrow(licenceTypeSlug))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void getLicenceTypeOrThrow_whenSlugUnknown_thenNotFound() {
    givenAllApplicationTypesEnabled();
    givenScheduleLicenceTypes();
    givenContinuationLicenceTypes();

    assertThatThrownBy(() -> selectApplicationTypeService.getLicenceTypeOrThrow("not-a-licence-type"))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void getLicenceTypeOrThrow_whenApplicationTypeRelevantToLicenceType_thenLicenceTypeReturned() {
    givenAllApplicationTypesEnabled();
    givenScheduleLicenceTypes();
    givenContinuationLicenceTypes();

    var result = selectApplicationTypeService.getLicenceTypeOrThrow(
        LicenceType.LANDWARD_PRODUCTION.getUrlSlug(),
        ApplicationType.CONTINUATION_APPLICATION
    );

    assertThat(result).isEqualTo(LicenceType.LANDWARD_PRODUCTION);
  }

  @Test
  void getLicenceTypeOrThrow_whenApplicationTypeNotRelevantToLicenceType_thenNotFound() {
    givenAllApplicationTypesEnabled();
    givenScheduleLicenceTypes();
    givenContinuationLicenceTypes();
    var licenceTypeSlug = LicenceType.CARBON_STORAGE.getUrlSlug();

    assertThatThrownBy(() -> selectApplicationTypeService.getLicenceTypeOrThrow(
        licenceTypeSlug,
        ApplicationType.CONTINUATION_APPLICATION
    ))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void getJourneyStartRedirectRoute_whenScheduleAmendmentApplication_thenRedirectToScheduleStart() {
    var licenceType = LicenceType.CARBON_STORAGE;

    var result = selectApplicationTypeService.getJourneyStartRedirectRoute(
        ApplicationType.SCHEDULE_AMENDMENT_APPLICATION,
        licenceType
    );

    assertThat(result).hasToString(ReverseRouter.redirect(on(StartScheduleWorkProgrammeApplicationJourneyController.class)
        .renderStartScheduleWorkProgrammeApplicationJourney(licenceType.getUrlSlug())).toString());
  }

  @Test
  void getJourneyStartRedirectRoute_whenContinuationApplication_thenRedirectToContinuationStart() {
    var licenceType = LicenceType.SEAWARD_PRODUCTION;

    var result = selectApplicationTypeService.getJourneyStartRedirectRoute(
        ApplicationType.CONTINUATION_APPLICATION,
        licenceType
    );

    assertThat(result).hasToString(ReverseRouter.redirect(on(StartContinuationApplicationController.class)
        .render(licenceType.getUrlSlug())).toString());
  }

  private void givenAllApplicationTypesEnabled() {
    when(featureFlagService.filterEnabled(ALL_APPLICATION_TYPES)).thenReturn(ALL_APPLICATION_TYPES);
  }

  private void givenScheduleLicenceTypes() {
    when(licenceTypeRulesResolver.getLicenceTypesThatCanCreateScheduleWorkProgrammeApplications())
        .thenReturn(List.of(LicenceType.LANDWARD_PRODUCTION, LicenceType.SEAWARD_PRODUCTION, LicenceType.CARBON_STORAGE));
  }

  private void givenContinuationLicenceTypes() {
    when(licenceTypeRulesResolver.getLicenceTypesThatCanCreateContinuationApplications())
        .thenReturn(List.of(LicenceType.LANDWARD_PRODUCTION, LicenceType.SEAWARD_PRODUCTION));
  }
}
