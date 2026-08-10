package uk.co.nstauthority.licensingmanagementservice.testharness;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.fivium.formlibrary.input.StringInput;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.internalapi.LicenceInternalApiRestController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/test-harness")
@Profile("test-harness")
public class TestHarnessController {

  private final LicencePositionTestHarnessFormValidator licencePositionTestHarnessFormValidator;
  private final LicencePositionFeatureTestHarnessFormValidator licencePositionFeatureTestHarnessFormValidator;
  private final LicenceService licenceService;
  private final TestHarnessService testHarnessService;
  private final LicencePositionFeatureTestHarnessService licencePositionFeatureTestHarnessService;

  public TestHarnessController(
      LicencePositionTestHarnessFormValidator licencePositionTestHarnessFormValidator,
      LicencePositionFeatureTestHarnessFormValidator licencePositionFeatureTestHarnessFormValidator,
      LicenceService licenceService,
      TestHarnessService testHarnessService,
      LicencePositionFeatureTestHarnessService licencePositionFeatureTestHarnessService
  ) {
    this.licencePositionTestHarnessFormValidator = licencePositionTestHarnessFormValidator;
    this.licencePositionFeatureTestHarnessFormValidator = licencePositionFeatureTestHarnessFormValidator;
    this.licenceService = licenceService;
    this.testHarnessService = testHarnessService;
    this.licencePositionFeatureTestHarnessService = licencePositionFeatureTestHarnessService;
  }

  @GetMapping
  public ModelAndView renderTestHarness() {
    return new ModelAndView("lms/testHarness/testHarness")
        .addObject("licencePositionTestHarnessUrl",
            ReverseRouter.route(on(TestHarnessController.class).renderGenerateLicencePosition())
        )
        .addObject("licencePositionFeatureTestHarnessUrl",
            ReverseRouter.route(on(TestHarnessController.class).renderLinkLicencePositionFeatures())
        );
  }

  @GetMapping("/licence-position")
  public ModelAndView renderGenerateLicencePosition() {
    return licencePositionTransactionTestHarnessModelAndView(new LicencePositionTestHarnessForm());
  }

  @PostMapping("licence-position")
  public ModelAndView generateLicencePosition(
      @ModelAttribute("form") LicencePositionTestHarnessForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    if (licencePositionTestHarnessFormValidator.hasErrors(form, bindingResult)) {
      return licencePositionTransactionTestHarnessModelAndView(form);
    }

    var licence = licenceService.findLicenceByIdOrThrow(Integer.parseInt(form.getLicenceId().getInputValue()));
    var secondaryLicence = licenceService.findLicenceByIdOrThrow(Integer.parseInt(form.getSecondaryLicenceId().getInputValue()));

    testHarnessService.generateLicencePositions(licence, secondaryLicence);

    NotificationBanner.newSuccessBanner()
        .withHeadingContent("Licence positions generated for licence %s and secondary licence %s".formatted(
            licence.getLicenceReference(),
            secondaryLicence.getLicenceReference()
        ))
        .applyTo(redirectAttributes);

    return ReverseRouter.redirect(on(TestHarnessController.class).renderTestHarness());
  }

  @GetMapping("/licence-position-features")
  public ModelAndView renderLinkLicencePositionFeatures() {
    return licencePositionFeatureTestHarnessModelAndView(new LicencePositionFeatureTestHarnessForm());
  }

  @PostMapping("/licence-position-features")
  public ModelAndView linkLicencePositionFeatures(
      @ModelAttribute("form") LicencePositionFeatureTestHarnessForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var licence = findLicence(form.getLicenceId()).orElse(null);
    var seedState = licence == null ? null : licencePositionFeatureTestHarnessService.getSeedState(licence);

    if (licencePositionFeatureTestHarnessFormValidator.hasErrors(form, bindingResult, seedState)) {
      return licencePositionFeatureTestHarnessModelAndView(form);
    }

    var createdFeatureCount = licencePositionFeatureTestHarnessService.createAndLinkFeatures(licence);

    NotificationBanner.newSuccessBanner()
        .withHeadingContent("%s features created and linked across %s positions on licence %s".formatted(
            createdFeatureCount,
            seedState.positionCount(),
            licence.getLicenceReference()
        ))
        .applyTo(redirectAttributes);

    return ReverseRouter.redirect(on(TestHarnessController.class).renderTestHarness());
  }

  private ModelAndView licencePositionFeatureTestHarnessModelAndView(LicencePositionFeatureTestHarnessForm form) {
    var slugList = Stream.of(LicenceType.LANDWARD_PRODUCTION, LicenceType.SEAWARD_PRODUCTION, LicenceType.CARBON_STORAGE)
        .map(LicenceType::getUrlSlug)
        .collect(Collectors.joining(","));

    return new ModelAndView("lms/testHarness/licencePositionFeatures")
        .addObject("form", form)
        .addObject("searchUrl",
            SearchSelectorService.route(on(LicenceInternalApiRestController.class).searchLicencesByReferenceAndType(
                slugList,
                null
            )))
        .addObject("preSelectedLicence", preSelectedLicenceMap(form.getLicenceId()))
        .addObject("cancelUrl", ReverseRouter.route(on(TestHarnessController.class).renderTestHarness()));
  }

  private ModelAndView licencePositionTransactionTestHarnessModelAndView(LicencePositionTestHarnessForm form) {
    var slugList = Stream.of(LicenceType.LANDWARD_PRODUCTION, LicenceType.SEAWARD_PRODUCTION, LicenceType.CARBON_STORAGE)
        .map(LicenceType::getUrlSlug)
        .collect(Collectors.joining(","));

    return new ModelAndView("lms/testHarness/licencePosition")
        .addObject("form", form)
        .addObject("searchUrl",
            SearchSelectorService.route(on(LicenceInternalApiRestController.class).searchLicencesByReferenceAndType(
                slugList,
                null
            )))
        .addObject("preSelectedLicence", preSelectedLicenceMap(form.getLicenceId()))
        .addObject("preSelectedSecondaryLicence", preSelectedLicenceMap(form.getSecondaryLicenceId()))
        .addObject("cancelUrl", ReverseRouter.route(on(TestHarnessController.class).renderTestHarness()));
  }

  private Map<String, String> preSelectedLicenceMap(StringInput licenceIdInput) {
    return findLicence(licenceIdInput)
        .map(licence -> Map.of(licenceIdInput.getInputValue(), licence.getLicenceReference()))
        .orElseGet(Collections::emptyMap);
  }

  private Optional<Licence> findLicence(StringInput licenceIdInput) {
    if (licenceIdInput.getInputValue() == null || licenceIdInput.getInputValue().isBlank()) {
      return Optional.empty();
    }

    return Optional.of(licenceService.findLicenceByIdOrThrow(Integer.parseInt(licenceIdInput.getInputValue())));
  }
}