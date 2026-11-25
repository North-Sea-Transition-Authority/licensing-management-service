package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subareaequity;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionLicenceIsType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionIsNotRemovedInCorrection;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.CorrectionPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadRouteUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.LicencePositionAddChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity.LicencePositionSetEquityForm;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity.LicencePositionSetEquityFormValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity.SetEquityCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaSetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@InvokingUserCanViewCorrection
@CorrectionHasStatus(LicenceCorrectionStatus.IN_PROGRESS)
@CorrectionLicenceIsType({LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION})
public class LicencePositionSubareaSetEquityController {

  private static final String SUBAREA_PARAM = "subarea";

  private final LicencePositionSetEquityFormValidator licencePositionSetEquityFormValidator;
  private final SubareaSetEquityCorrectionService subareaSetEquityCorrectionService;
  private final SetEquityCorrectionService setEquityCorrectionService;
  private final SubareaEquityOptionsService subareaEquityOptionsService;
  private final OrganisationUnitQueryService organisationUnitQueryService;

  LicencePositionSubareaSetEquityController(
      LicencePositionSetEquityFormValidator licencePositionSetEquityFormValidator,
      SubareaSetEquityCorrectionService subareaSetEquityCorrectionService,
      SetEquityCorrectionService setEquityCorrectionService,
      SubareaEquityOptionsService subareaEquityOptionsService,
      OrganisationUnitQueryService organisationUnitQueryService
  ) {
    this.licencePositionSetEquityFormValidator = licencePositionSetEquityFormValidator;
    this.subareaSetEquityCorrectionService = subareaSetEquityCorrectionService;
    this.setEquityCorrectionService = setEquityCorrectionService;
    this.subareaEquityOptionsService = subareaEquityOptionsService;
    this.organisationUnitQueryService = organisationUnitQueryService;
  }

  @GetMapping("/position/{licencePositionId}/subarea-set-equity")
  @LicencePositionIsNotRemovedInCorrection
  public ModelAndView renderForPosition(
      CorrectionPosition correctionPosition,
      @RequestParam(SUBAREA_PARAM) String subareaKey
  ) {
    var subareaOption = subareaEquityOptionsService.getSubareaOptionOrThrow(correctionPosition, subareaKey);

    return setEquityModelAndView(
        correctionPosition.correction(),
        subareaOption,
        new LicencePositionSetEquityForm(),
        addChangeUrl(correctionPosition)
    );
  }

  @PostMapping("/position/{licencePositionId}/subarea-set-equity")
  @LicencePositionIsNotRemovedInCorrection
  ModelAndView submitForPosition(
      CorrectionPosition correctionPosition,
      @RequestParam(SUBAREA_PARAM) String subareaKey,
      @ModelAttribute("form") LicencePositionSetEquityForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var subareaOption = subareaEquityOptionsService.getSubareaOptionOrThrow(correctionPosition, subareaKey);
    var operations = new ArrayList<>(
        subareaSetEquityCorrectionService.getCommittedSubareaSetEquityOperations(correctionPosition, subareaOption)
    );

    var organisationIds = operations.stream().map(SubareaSetEquityOperation::transferTo).toList();
    if (licencePositionSetEquityFormValidator.hasErrors(form, bindingResult, organisationIds)) {
      return setEquityModelAndView(
          correctionPosition.correction(),
          subareaOption,
          form,
          addChangeUrl(correctionPosition)
      );
    }

    operations.add(toOperation(subareaOption, form));
    subareaSetEquityCorrectionService.commitSubareaSetEquity(correctionPosition, subareaOption, operations);

    NotificationBanner.newSuccessBannerWithHeader("Equity set updated", redirectAttributes);
    return ReverseRouter.redirect(on(this.getClass()).renderSummaryForPosition(correctionPosition, subareaKey));
  }

  @GetMapping("/position/{licencePositionId}/subarea-set-equity/summary")
  @LicencePositionIsNotRemovedInCorrection
  public ModelAndView renderSummaryForPosition(
      CorrectionPosition correctionPosition,
      @RequestParam(SUBAREA_PARAM) String subareaKey
  ) {
    var subareaOption = subareaEquityOptionsService.getSubareaOptionOrThrow(correctionPosition, subareaKey);
    var operations = subareaSetEquityCorrectionService
        .getCommittedSubareaSetEquityOperations(correctionPosition, subareaOption);

    var removeUrls = operations.stream()
        .map(operation -> ReverseRouter.route(on(this.getClass())
            .removeForPosition(correctionPosition, subareaKey, operation.transferTo())))
        .toList();

    return setEquitySummaryModelAndView(
        correctionPosition.correction(),
        subareaOption,
        operations,
        ReverseRouter.route(on(this.getClass()).renderForPosition(correctionPosition, subareaKey)),
        LicencePositionStagedPayloadRouteUtil.getPositionPageUrl(correctionPosition),
        ReverseRouter.route(on(this.getClass()).submitSummaryForPosition(correctionPosition)),
        removeUrls
    );
  }

  @PostMapping("/position/{licencePositionId}/subarea-set-equity/summary")
  @LicencePositionIsNotRemovedInCorrection
  ModelAndView submitSummaryForPosition(CorrectionPosition correctionPosition) {
    return LicencePositionStagedPayloadRouteUtil.redirectToPositionPage(correctionPosition);
  }

  @PostMapping("/position/{licencePositionId}/subarea-set-equity/remove")
  @LicencePositionIsNotRemovedInCorrection
  ModelAndView removeForPosition(
      CorrectionPosition correctionPosition,
      @RequestParam(SUBAREA_PARAM) String subareaKey,
      @RequestParam Integer transferTo
  ) {
    var subareaOption = subareaEquityOptionsService.getSubareaOptionOrThrow(correctionPosition, subareaKey);
    var operations = subareaSetEquityCorrectionService
        .getCommittedSubareaSetEquityOperations(correctionPosition, subareaOption)
        .stream()
        .filter(operation -> !operation.transferTo().equals(transferTo))
        .toList();

    subareaSetEquityCorrectionService.commitSubareaSetEquity(correctionPosition, subareaOption, operations);

    return ReverseRouter.redirect(on(this.getClass()).renderSummaryForPosition(correctionPosition, subareaKey));
  }

  private SubareaSetEquityOperation toOperation(SubareaOption subareaOption, LicencePositionSetEquityForm form) {
    return LicenceOperation.newSubareaSetEquityOperation()
        .withBlockFeatureId(subareaOption.feature().getId())
        .withSubarea(subareaOption.subarea())
        .withTransferTo(Integer.parseInt(form.getTransferTo()))
        .withEquity(form.getEquity().getAsBigDecimal().orElseThrow())
        .build();
  }

  private ModelAndView setEquityModelAndView(
      LicenceCorrection licenceCorrection,
      SubareaOption subareaOption,
      LicencePositionSetEquityForm form,
      String backLinkUrl
  ) {
    var pageCaption = pageCaption(licenceCorrection, subareaOption);

    return new ModelAndView("lms/licence/correction/setEquity")
        .addObject("pageTitle", "Add equity")
        .addObject("pageCaption", pageCaption)
        .addObject("form", form)
        .addObject("licenseeOrgUnitUrl",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null)))
        .addObject("preselectedTransferTo", organisationUnitQueryService.getOrganisationUnitSelectOption(form.getTransferTo()))
        .addObject("backLinkUrl", backLinkUrl);
  }

  private ModelAndView setEquitySummaryModelAndView(
      LicenceCorrection licenceCorrection,
      SubareaOption subareaOption,
      List<SubareaSetEquityOperation> operations,
      String addOrganisationUrl,
      String backLinkUrl,
      String saveAndContinueUrl,
      List<String> removeUrls
  ) {
    var pageCaption = pageCaption(licenceCorrection, subareaOption);
    var setEquityViews = setEquityCorrectionService.getSetEquityViews(operations);
    var totalEquity = operations.stream().map(SubareaSetEquityOperation::equity).reduce(BigDecimal.ZERO, BigDecimal::add);

    return new ModelAndView("lms/licence/correction/setEquitySummary")
        .addObject("pageTitle", "Add subarea equity")
        .addObject("pageCaption", pageCaption)
        .addObject("setEquityViews", setEquityViews)
        .addObject("totalEquity", totalEquity)
        .addObject("addOrganisationUrl", addOrganisationUrl)
        .addObject("backLinkUrl", backLinkUrl)
        .addObject("saveAndContinueUrl", saveAndContinueUrl)
        .addObject("removeUrls", removeUrls);
  }

  private String pageCaption(LicenceCorrection licenceCorrection, SubareaOption subareaOption) {
    return "%s - %s".formatted(licenceCorrection.getLicence().getLicenceReference(), subareaOption.label());
  }

  private String addChangeUrl(CorrectionPosition correctionPosition) {
    return ReverseRouter.route(on(LicencePositionAddChangeController.class).renderForPosition(correctionPosition));
  }
}
