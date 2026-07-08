package uk.co.nstauthority.licensingmanagementservice.licence.correction.workarea;

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
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.AddLicencePositionCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionPageView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = LicenceCorrectionController.class)
@ActiveProfiles({"test", "enable-lms2"})
class LicenceCorrectionControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicencePositionService licencePositionService;

  @MockitoBean
  private LicencePositionCorrectionService licencePositionCorrectionService;

  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_CORRECTION_ID = UUID.randomUUID();
  private static final String LICENCE_REFERENCE = "P1234";
  private static final String CORRECTION_REFERENCE = "COR-1";
  private static final String REASON = "Typo in executed position";
  private static final String PAGE_TITLE = LICENCE_REFERENCE;
  private static final String PAGE_CAPTION = "Licence - P1234";

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
        .build();
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(licence)
        .withCorrectionReference(CORRECTION_REFERENCE)
        .withReason(REASON)
        .build();

    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceService.getLicencePageCaption(licence)).thenReturn(PAGE_CAPTION);
    when(licencePositionService.getExecutedChronologicalLicencePositions(licence)).thenReturn(List.of());
    when(licencePositionCorrectionService.getAddedLicencePositionCorrections(correction)).thenReturn(List.of());

    mockMvc.perform(get(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderCorrection(CORRECTION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/viewCorrection"),
            model().attribute("pageCaption", PAGE_CAPTION),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attribute("correctionReference", CORRECTION_REFERENCE),
            model().attribute("reason", REASON),
            model().attribute("addPositionUrl",
                ReverseRouter.route(on(AddLicencePositionCorrectionController.class)
                    .renderAddLicencePositionCorrection(CORRECTION_ID, null))),
            model().attributeExists("licencePositionPageView")
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
        .build();
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(licence)
        .withCorrectionReference(CORRECTION_REFERENCE)
        .withReason(REASON)
        .build();
    var position = LicencePositionTestUtil.newBuilder().build();
    var pageView = LicencePositionPageView.fromExecutedPosition(
        List.of(), "1 Jan 2026", "REF-1", Map.of(), null, position.getId());

    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licencePositionService.getPositionForLicence(licence, position.getId())).thenReturn(position);
    when(licencePositionService.getCorrectionPositionPageView(correction, position)).thenReturn(pageView);

    mockMvc.perform(get(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderLicencePosition(CORRECTION_ID, position.getId(), null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/viewCorrection"),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attribute("licencePositionPageView", pageView)
        );
  }

  @Test
  void renderAddedPosition_whenAllocatedToUser() throws Exception {
    var licence = LicenceTestUtil.builder()
        .withLicenceReference(LICENCE_REFERENCE)
        .build();
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(licence)
        .withCorrectionReference(CORRECTION_REFERENCE)
        .withReason(REASON)
        .build();
    var positionCorrection = new LicencePositionCorrection();
    var pageView = LicencePositionPageView.fromNonExecutedPosition(
        List.of(), "1 Jan 2026", "REF-1", POSITION_CORRECTION_ID);

    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    when(licencePositionService.getCorrectionAddedPositionPageView(correction, positionCorrection)).thenReturn(pageView);

    mockMvc.perform(get(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            model().attribute("licencePositionPageView", pageView)
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