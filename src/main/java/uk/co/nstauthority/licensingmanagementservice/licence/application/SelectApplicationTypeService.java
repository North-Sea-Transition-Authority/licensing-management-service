package uk.co.nstauthority.licensingmanagementservice.licence.application;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.startjourney.StartContinuationApplicationController;
import uk.co.nstauthority.licensingmanagementservice.licence.rules.LicenceTypeRulesResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.startjourney.StartScheduleWorkProgrammeApplicationJourneyController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.FeatureFlagService;

@Service
public class SelectApplicationTypeService {

  private final FeatureFlagService featureFlagService;
  private final LicenceTypeRulesResolver licenceTypeRulesResolver;

  public SelectApplicationTypeService(FeatureFlagService featureFlagService,
                                      LicenceTypeRulesResolver licenceTypeRulesResolver) {
    this.featureFlagService = featureFlagService;
    this.licenceTypeRulesResolver = licenceTypeRulesResolver;
  }

  public List<LicenceType> getAvailableLicenceTypes() {
    return Arrays.stream(LicenceType.values())
        .filter(licenceType -> !getAvailableApplicationTypes(licenceType).isEmpty())
        .toList();
  }

  public List<ApplicationType> getAvailableApplicationTypes(LicenceType licenceType) {
    return featureFlagService.filterEnabled(Arrays.asList(ApplicationType.values()))
        .stream()
        .filter(applicationType -> getLicenceTypesThatCanCreate(applicationType).contains(licenceType))
        .toList();
  }

  public LicenceType getLicenceTypeOrThrow(String licenceTypeSlug, ApplicationType applicationType) {
    var licenceType = getLicenceTypeOrThrow(licenceTypeSlug);

    if (!getAvailableApplicationTypes(licenceType).contains(applicationType)) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "Application type %s cannot be started for licence type %s".formatted(applicationType, licenceType)
      );
    }

    return licenceType;
  }

  public LicenceType getLicenceTypeOrThrow(String licenceTypeSlug) {
    return getAvailableLicenceTypes().stream()
        .filter(licenceType -> licenceType.getUrlSlug().equals(licenceTypeSlug))
        .findFirst()
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "No application can be started for licence type slug %s".formatted(licenceTypeSlug)
        ));
  }

  public ModelAndView getJourneyStartRedirectRoute(ApplicationType selectedApplicationType, LicenceType licenceType) {
    return switch (selectedApplicationType) {
      case SCHEDULE_AMENDMENT_APPLICATION ->
          ReverseRouter.redirect(on(StartScheduleWorkProgrammeApplicationJourneyController.class)
          .renderStartScheduleWorkProgrammeApplicationJourney(licenceType.getUrlSlug()));
      case CONTINUATION_APPLICATION -> ReverseRouter.redirect(on(StartContinuationApplicationController.class)
          .render(licenceType.getUrlSlug()));
    };
  }

  private List<LicenceType> getLicenceTypesThatCanCreate(ApplicationType applicationType) {
    return switch (applicationType) {
      case SCHEDULE_AMENDMENT_APPLICATION ->
          licenceTypeRulesResolver.getLicenceTypesThatCanCreateScheduleWorkProgrammeApplications();
      case CONTINUATION_APPLICATION -> licenceTypeRulesResolver.getLicenceTypesThatCanCreateContinuationApplications();
    };
  }
}
