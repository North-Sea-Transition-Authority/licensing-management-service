package uk.co.nstauthority.licensingmanagementservice.licence.correction;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;
import static uk.co.nstauthority.licensingmanagementservice.util.RedirectedToLoginUrlMatcher.redirectionToLoginUrl;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.AddLicencePositionCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionPageView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTimelineView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.PartialSurrenderChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.AdministratorStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.LicencePositionStateView;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.EnergyPortalUserTestUtil;

@ContextConfiguration(classes = LicenceCorrectionController.class)
@ActiveProfiles({"test", "enable-lms2"})
class LicenceCorrectionControllerTest extends AbstractControllerTest {

  @MockitoBean
  private EnergyPortalUserService energyPortalUserService;

  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_CORRECTION_ID = UUID.randomUUID();
  private static final String LICENCE_REFERENCE = "P1234";
  private static final String CORRECTION_REFERENCE = "COR-1";
  private static final String REASON = "Typo in executed position";
  private static final String PAGE_CAPTION = "Licence - P1234";
  private static final LicenceType LICENCE_TYPE = LicenceType.SEAWARD_PRODUCTION;
  private static final long ALLOCATED_TO_WUA_ID = 123L;
  private static final String USER_LOOKUP_PURPOSE = "Get correction allocated to user details";
  private static final String PAGE_TITLE = "%s - licence correction".formatted(LICENCE_REFERENCE);

  @Test
  void renderCorrection_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderCorrection(CORRECTION_ID, null))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderCorrection_whenAllocatedToUser() throws Exception {
    var licence = LicenceTestUtil.builder()
        .withLicenceReference(LICENCE_REFERENCE)
        .withLicenceType(LICENCE_TYPE)
        .build();
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(licence)
        .withCorrectionReference(CORRECTION_REFERENCE)
        .withReason(REASON)
        .withAllocatedToWuaId(ALLOCATED_TO_WUA_ID)
        .build();

    var allocatedToUser = EnergyPortalUserTestUtil.newBuilder()
        .withWebUserAccountId(ALLOCATED_TO_WUA_ID)
        .withForename("Jane")
        .withSurname("Doe")
        .buildJson();

    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceService.getLicencePageCaption(licence)).thenReturn(PAGE_CAPTION);
    when(licencePositionService.getExecutedChronologicalLicencePositions(licence)).thenReturn(List.of());
    when(licencePositionCorrectionService.getAddedLicencePositionCorrections(correction)).thenReturn(List.of());
    when(energyPortalUserService.getByWuaId(WebUserAccountId.from(ALLOCATED_TO_WUA_ID), USER_LOOKUP_PURPOSE))
        .thenReturn(allocatedToUser);

    mockMvc.perform(get(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderCorrection(CORRECTION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/viewCorrection"),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attribute("pageCaption", PAGE_CAPTION),
            model().attribute("correction", correction),
            model().attribute("allocatedToUser", allocatedToUser.displayName()),
            model().attribute("addPositionUrl",
                ReverseRouter.route(on(AddLicencePositionCorrectionController.class)
                    .renderAddLicencePositionCorrection(CORRECTION_ID, null))),
            model().attributeExists("licencePositionPageView"),
            model().attribute("cancelCorrectionUrl", ReverseRouter.route(on(LicenceCorrectionCancelController.class)
                .renderCancelCorrection(CORRECTION_ID, null)))
        );
  }

  @Test
  void renderCorrection_whenExecutedPositionsExist_redirectsToLatestExecutedPosition() throws Exception {
    var licence = LicenceTestUtil.builder()
        .withLicenceReference(LICENCE_REFERENCE)
        .build();
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(licence)
        .withCorrectionReference(CORRECTION_REFERENCE)
        .withReason(REASON)
        .build();

    var earlierExecuted = LicencePositionTestUtil.newBuilder()
        .withPositionDate(LocalDate.of(2026, Month.JUNE, 1)).withIsExecuted(true).build();
    var latestExecuted = LicencePositionTestUtil.newBuilder()
        .withPositionDate(LocalDate.of(2026, Month.JUNE, 1)).withIsExecuted(true).build();

    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licencePositionService.getExecutedChronologicalLicencePositions(licence))
        .thenReturn(List.of(earlierExecuted, latestExecuted));
    when(licencePositionCorrectionService.getAddedLicencePositionCorrections(correction)).thenReturn(List.of());

    mockMvc.perform(get(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderCorrection(CORRECTION_ID, null)))
            .with(user(regulatorUser)))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderLicencePosition(CORRECTION_ID, latestExecuted.getId(), null))));
  }

  @Test
  void renderCorrection_whenOnlyAddedPositionsExist_redirectsToAddedPosition() throws Exception {
    var licence = LicenceTestUtil.builder()
        .withLicenceReference(LICENCE_REFERENCE)
        .build();
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(licence)
        .withCorrectionReference(CORRECTION_REFERENCE)
        .withReason(REASON)
        .build();

    var addedPosition = LicencePositionCorrectionTestUtil.newBuilder().withId(POSITION_CORRECTION_ID).build();

    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licencePositionService.getExecutedChronologicalLicencePositions(licence)).thenReturn(List.of());
    when(licencePositionCorrectionService.getAddedLicencePositionCorrections(correction))
        .thenReturn(List.of(addedPosition));

    mockMvc.perform(get(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderCorrection(CORRECTION_ID, null)))
            .with(user(regulatorUser)))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null))));
  }

  @Test
  void renderLicencePosition_whenAllocatedToUser() throws Exception {
    var licence = LicenceTestUtil.builder()
        .withLicenceReference(LICENCE_REFERENCE)
        .withLicenceType(LICENCE_TYPE)
        .build();
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(licence)
        .withCorrectionReference(CORRECTION_REFERENCE)
        .withReason(REASON)
        .withAllocatedToWuaId(ALLOCATED_TO_WUA_ID)
        .build();
    var allocatedToUser = EnergyPortalUserTestUtil.newBuilder()
        .withWebUserAccountId(ALLOCATED_TO_WUA_ID)
        .withForename("Jane")
        .withSurname("Doe")
        .buildJson();
    var position = LicencePositionTestUtil.newBuilder().build();
    var pageView = LicencePositionPageView.fromExecutedPosition(
        List.of(),
        "1 Jan 2026",
        "REF-1",
        Map.of(),
        null,
        position.getId(),
        LicencePositionPageView.Actions.none(),
        LICENCE_TYPE,
        List.of()
    );

    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceService.getLicencePageCaption(licence)).thenReturn(PAGE_CAPTION);
    when(licencePositionService.getPositionForLicence(licence, position.getId())).thenReturn(position);
    when(licencePositionViewService.getCorrectionPositionPageView(correction, position)).thenReturn(pageView);
    when(energyPortalUserService.getByWuaId(WebUserAccountId.from(ALLOCATED_TO_WUA_ID), USER_LOOKUP_PURPOSE))
        .thenReturn(allocatedToUser);

    mockMvc.perform(get(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderLicencePosition(CORRECTION_ID, position.getId(), null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/viewCorrection"),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attribute("pageCaption", PAGE_CAPTION),
            model().attribute("correction", correction),
            model().attribute("allocatedToUser", allocatedToUser.displayName()),
            model().attribute("licencePositionPageView", pageView),
            model().attribute("cancelCorrectionUrl", ReverseRouter.route(on(LicenceCorrectionCancelController.class)
                .renderCancelCorrection(CORRECTION_ID, null)))
        );
  }

  @Test
  void renderLicencePosition_whenPartialSurrenderStaged_rendersThePartialSurrenderCard() throws Exception {
    var licence = LicenceTestUtil.builder()
        .withLicenceReference(LICENCE_REFERENCE)
        .withLicenceType(LICENCE_TYPE)
        .build();
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(licence)
        .withCorrectionReference(CORRECTION_REFERENCE)
        .withReason(REASON)
        .withAllocatedToWuaId(ALLOCATED_TO_WUA_ID)
        .build();
    var allocatedToUser = EnergyPortalUserTestUtil.newBuilder()
        .withWebUserAccountId(ALLOCATED_TO_WUA_ID)
        .withForename("Jane")
        .withSurname("Doe")
        .buildJson();
    var position = LicencePositionTestUtil.newBuilder().build();
    var pageView = LicencePositionPageView.fromExecutedPosition(
        List.of(LicencePositionTimelineView.builder()
            .withPositionId(position.getId())
            .withUrl("/position")
            .withRegulatorReference("REF-1")
            .withFormattedPositionDate("1 Jan 2026")
            .build()),
        "1 Jan 2026",
        "REF-1",
        Map.of(LicenceOperation.PARTIAL_SURRENDER,
            new PartialSurrenderChangeView("1 August 2026",
                List.of(
                    new PartialSurrenderChangeView.BlockRow("30/1a", "Full surrender"),
                    new PartialSurrenderChangeView.BlockRow("30/2", "Partial surrender")),
                LicencePositionChangeType.ADD_CHANGE)),
        new LicencePositionStateView(new AdministratorStateView("Operator Ltd"), List.of()),
        position.getId(),
        LicencePositionPageView.Actions.none(),
        LICENCE_TYPE,
        List.of()
    );

    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceService.getLicencePageCaption(licence)).thenReturn(PAGE_CAPTION);
    when(licencePositionService.getPositionForLicence(licence, position.getId())).thenReturn(position);
    when(licencePositionViewService.getCorrectionPositionPageView(correction, position)).thenReturn(pageView);
    when(energyPortalUserService.getByWuaId(WebUserAccountId.from(ALLOCATED_TO_WUA_ID), USER_LOOKUP_PURPOSE))
        .thenReturn(allocatedToUser);

    mockMvc.perform(get(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderLicencePosition(CORRECTION_ID, position.getId(), null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/viewCorrection"),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attribute("pageCaption", PAGE_CAPTION),
            model().attribute("correction", correction),
            model().attribute("allocatedToUser", allocatedToUser.displayName()),
            model().attribute("licencePositionPageView", pageView),
            model().attribute("cancelCorrectionUrl", ReverseRouter.route(on(LicenceCorrectionCancelController.class)
                .renderCancelCorrection(CORRECTION_ID, null))));
  }

  @Test
  void renderAddedPosition_whenAllocatedToUser() throws Exception {
    var licence = LicenceTestUtil.builder()
        .withLicenceReference(LICENCE_REFERENCE)
        .withLicenceType(LICENCE_TYPE)
        .build();
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(licence)
        .withCorrectionReference(CORRECTION_REFERENCE)
        .withReason(REASON)
        .withAllocatedToWuaId(ALLOCATED_TO_WUA_ID)
        .build();
    var allocatedToUser = EnergyPortalUserTestUtil.newBuilder()
        .withWebUserAccountId(ALLOCATED_TO_WUA_ID)
        .withForename("Jane")
        .withSurname("Doe")
        .buildJson();
    var positionCorrection = new LicencePositionCorrection();
    var pageView = LicencePositionPageView.fromAddedPosition(
        List.of(),
        "1 Jan 2026",
        "REF-1",
        Map.of(),
        null,
        POSITION_CORRECTION_ID,
        LicencePositionPageView.Actions.none(),
        LICENCE_TYPE,
        List.of()
    );

    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    when(licenceService.getLicencePageCaption(licence)).thenReturn(PAGE_CAPTION);
    when(licencePositionViewService.getCorrectionAddedPositionPageView(correction, positionCorrection)).thenReturn(pageView);
    when(energyPortalUserService.getByWuaId(WebUserAccountId.from(ALLOCATED_TO_WUA_ID), USER_LOOKUP_PURPOSE))
        .thenReturn(allocatedToUser);

    mockMvc.perform(get(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/viewCorrection"),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attribute("pageCaption", PAGE_CAPTION),
            model().attribute("correction", correction),
            model().attribute("allocatedToUser", allocatedToUser.displayName()),
            model().attribute("licencePositionPageView", pageView),
            model().attribute("cancelCorrectionUrl", ReverseRouter.route(on(LicenceCorrectionCancelController.class)
                .renderCancelCorrection(CORRECTION_ID, null)))
        );

  }

  @Test
  void renderCorrection_whenNotAllocatedToUser() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());

    mockMvc.perform(get(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderCorrection(CORRECTION_ID, null)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }
}