package uk.co.nstauthority.licensingmanagementservice.licence.correction.reviewandapply;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;
import static uk.co.nstauthority.licensingmanagementservice.util.RedirectedToLoginUrlMatcher.redirectionToLoginUrl;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.CorrectionDetailsView;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.CorrectionDetailsViewService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.CorrectionMarker;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.tab.TabbedLicencePageService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRoleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamTestUtil;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@ContextConfiguration(classes = CorrectionSummaryController.class)
@ActiveProfiles("test")
class CorrectionSummaryControllerTest extends AbstractControllerTest {

  @MockitoBean
  private CorrectionDetailsViewService correctionDetailsViewService;

  @MockitoBean
  private CorrectionSummaryService correctionSummaryService;

  @MockitoBean
  private TabbedLicencePageService tabbedLicencePageService;

  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final String PAGE_CAPTION = "Licence - P1234";
  private static final String DEFAULT_TAB_URL = "/licences/1/default-tab";
  private static final long OTHER_USER_WUA_ID = 123L;

  @Test
  void renderCorrectionSummary_whenNotLoggedIn_thenRedirectToLogin() throws Exception {
    var correction = buildCorrection(LicenceCorrectionStatus.IN_PROGRESS);

    mockMvc.perform(get(ReverseRouter.route(on(CorrectionSummaryController.class)
            .renderCorrectionSummary(correction))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderCorrectionSummary_whenUserOnlyHasCorrectorRoleForOtherLicenceType_thenForbidden() throws Exception {
    var correction = buildCorrection(LicenceCorrectionStatus.IN_PROGRESS);

    givenCorrectionExists(correction);
    givenRegulatorUserHasRole(Role.CARBON_STORAGE_LICENCE_CORRECTOR);

    mockMvc.perform(get(ReverseRouter.route(on(CorrectionSummaryController.class)
            .renderCorrectionSummary(correction)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(correctionSummaryService);
  }

  @Test
  void renderCorrectionSummary_whenCorrectionIsCancelled_thenForbidden() throws Exception {
    var correction = buildCorrection(LicenceCorrectionStatus.CANCELLED);

    givenCorrectionExists(correction);
    givenRegulatorUserHasRole(Role.PRODUCTION_LICENCE_CORRECTOR);

    mockMvc.perform(get(ReverseRouter.route(on(CorrectionSummaryController.class)
            .renderCorrectionSummary(correction)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(correctionSummaryService);
  }

  @ParameterizedTest
  @EnumSource(value = LicenceCorrectionStatus.class, names = {"IN_PROGRESS", "COMPLETE"})
  void renderCorrectionSummary_whenCorrectorIsNotAllocatedToCorrection_thenRenderSummaryPage(
      LicenceCorrectionStatus correctionStatus
  ) throws Exception {
    var correction = buildCorrection(correctionStatus);
    var correctionDetails = new CorrectionDetailsView(
        "COR-1", "a reason", "Jane Doe", correctionStatus.getDisplayName(), "P1234", "5 June 2026");
    var positions = List.of(new ReviewPositionView(
        UUID.randomUUID(), "1 January 2026", "COR-1", CorrectionMarker.POSITION_ADDED, List.of()));

    givenCorrectionExists(correction);
    givenRegulatorUserHasRole(Role.PRODUCTION_LICENCE_CORRECTOR);
    when(licenceService.getLicencePageCaption(correction.getLicence())).thenReturn(PAGE_CAPTION);
    when(tabbedLicencePageService.getDefaultTabUrl(correction.getLicence())).thenReturn(DEFAULT_TAB_URL);
    when(correctionDetailsViewService.getDetailsView(correction)).thenReturn(correctionDetails);
    when(correctionSummaryService.getSummaryPositions(correction)).thenReturn(positions);

    mockMvc.perform(get(ReverseRouter.route(on(CorrectionSummaryController.class)
            .renderCorrectionSummary(correction)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/reviewandapply/correctionSummary"),
            model().attribute("pageTitle", "Correction summary"),
            model().attribute("pageCaption", PAGE_CAPTION),
            model().attribute("correction", correction),
            model().attribute("correctionDetails", correctionDetails),
            model().attribute("positions", positions),
            model().attribute("backLinkUrl", DEFAULT_TAB_URL)
        );
  }

  private LicenceCorrection buildCorrection(LicenceCorrectionStatus correctionStatus) {
    return LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(LicenceTestUtil.builder()
            .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
            .withLicenceReference("P1234")
            .build())
        .withAllocatedToWuaId(OTHER_USER_WUA_ID)
        .withStatus(correctionStatus)
        .build();
  }

  private void givenCorrectionExists(LicenceCorrection correction) {
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
  }

  private void givenRegulatorUserHasRole(Role role) {
    when(teamQueryService.getTeamRolesForUser(REGULATOR_USER_WUA_ID)).thenReturn(Set.of(
        TeamRoleTestUtil.newBuilder()
            .withTeam(TeamTestUtil.newBuilder()
                .withTeamType(TeamType.LICENCE_MANAGEMENT)
                .build())
            .withRole(role)
            .withWuaId(REGULATOR_USER_WUA_ID)
            .build()
    ));
  }
}