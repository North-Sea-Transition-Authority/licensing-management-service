package uk.co.nstauthority.licensingmanagementservice.workarea;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupRestController;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.SelectApplicationTypeController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.FeatureFlagService;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.ReleaseFeature;
import uk.co.nstauthority.licensingmanagementservice.teams.RegulatorRoleService;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@Controller
@RequestMapping("/work-area")
@SessionAttributes("workAreaSession")
public class WorkAreaController {
  public static final String WORK_AREA_PAGE_NAME = "Work area";

  private final WorkAreaService workAreaService;
  private final ApplicationAccessService applicationAccessService;
  private final FeatureFlagService featureFlagService;
  private final RegulatorRoleService regulatorRoleService;
  private final OrganisationUnitQueryService organisationUnitQueryService;
  private final OrganisationGroupQueryService organisationGroupQueryService;

  public WorkAreaController(
      WorkAreaService workAreaService,
      ApplicationAccessService applicationAccessService,
      FeatureFlagService featureFlagService,
      RegulatorRoleService regulatorRoleService,
      OrganisationUnitQueryService organisationUnitQueryService,
      OrganisationGroupQueryService organisationGroupQueryService
  ) {
    this.workAreaService = workAreaService;
    this.applicationAccessService = applicationAccessService;
    this.featureFlagService = featureFlagService;
    this.regulatorRoleService = regulatorRoleService;
    this.organisationUnitQueryService = organisationUnitQueryService;
    this.organisationGroupQueryService = organisationGroupQueryService;
  }

  @GetMapping
  public ModelAndView getWorkArea(@ModelAttribute("workAreaSession") WorkAreaSession workAreaSession, ServiceUserDetail user) {
    return getModelAndView(workAreaSession.getWorkAreaFilterForm(), user);
  }

  @PostMapping
  ModelAndView renderWorkAreaResults(@ModelAttribute("form") WorkAreaFilterForm form,
                                     @ModelAttribute("workAreaSession") WorkAreaSession workAreaSession) {
    workAreaSession.update(form);
    return ReverseRouter.redirect(on(WorkAreaController.class).getWorkArea(null, null));
  }

  @GetMapping("/clear-filters")
  public ModelAndView clearWorkAreaFilters(@ModelAttribute("workAreaSession") WorkAreaSession workAreaSession,
                                           SessionStatus sessionStatus) {
    sessionStatus.setComplete();
    workAreaSession.clearSession();
    return ReverseRouter.redirect(on(WorkAreaController.class).getWorkArea(null, null));
  }

  @ModelAttribute("workAreaSession")
  private WorkAreaSession getWorkAreaSessionWithDefaultFilters(@ModelAttribute("form") WorkAreaFilterForm form) {
    return new WorkAreaSession(form);
  }

  private @NotNull ModelAndView getModelAndView(WorkAreaFilterForm form, ServiceUserDetail user) {

    return new ModelAndView("lms/workarea/workArea")
        .addObject("pageTitle", WORK_AREA_PAGE_NAME)
        .addObject("workAreaItems", workAreaService.getWorkAreaResults(form, user))
        .addObject("canStartApplication",
            featureFlagService.isEnabled(ReleaseFeature.START_APPLICATION)
                && applicationAccessService.userHasAccessToStartApplication(user.wuaId()))
        .addObject("startApplicationUrl", ReverseRouter
                .route(on(SelectApplicationTypeController.class).render()))
        .addObject("form", form)
        .addObject("licenceTypes", DisplayableEnumOptionUtil.getDisplayableOptions(LicenceType.getDisplayableTypes()))
        .addObject("applicationTypes", DisplayableEnumOptionUtil.getDisplayableOptions(ApplicationType.class))
        .addObject("applicationStatuses", DisplayableEnumOptionUtil.getDisplayableOptions(
            ApplicationStatus.getSearchableStatuses()))
        .addObject("isRegulatorUser", regulatorRoleService.isRegulator(user))
        .addObject("licenseeOrgUnitUrl",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null)))
        .addObject("preSelectedLicenseeOrgUnit", organisationUnitQueryService.getOrganisationUnitSelectOption(
            form.getLicenseeOrgUnitId() == null ? null : form.getLicenseeOrgUnitId().toString()))
        .addObject("licenseeGroupOrgUnitUrl",
            SearchSelectorService.route(on(OrganisationGroupRestController.class).getOrganisationGroupSearchResults(null)))
        .addObject("preSelectedLicenseeGroupOrgUnit",
            organisationGroupQueryService.getOrganisationGroupSelectOption(form.getLicenseeOrgGroupId()))
        .addObject("clearFilterUrl",
        ReverseRouter.route(on(WorkAreaController.class).clearWorkAreaFilters(null, null)));
  }
}