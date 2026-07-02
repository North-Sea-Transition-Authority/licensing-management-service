package uk.co.nstauthority.licensingmanagementservice.licence.correction.workarea;

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
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.AddLicencePositionCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = LicenceCorrectionController.class)
@ActiveProfiles({"test", "enable-lms2"})
class LicenceCorrectionControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicencePositionService licencePositionService;

  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final String LICENCE_REFERENCE = "P1234";
  private static final String CORRECTION_REFERENCE = "COR-1";
  private static final String REASON = "Typo in executed position";
  private static final String PAGE_TITLE = LICENCE_REFERENCE;

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
    when(licencePositionService.getTimelineView(licence)).thenReturn(List.of());

    mockMvc.perform(get(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderCorrection(CORRECTION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/viewCorrection"),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attribute("correctionReference", CORRECTION_REFERENCE),
            model().attribute("reason", REASON),
            model().attributeExists("licencePositionTimelineView"),
            model().attribute("addPositionUrl",
                ReverseRouter.route(on(AddLicencePositionCorrectionController.class)
                    .renderAddLicencePositionCorrection(CORRECTION_ID, null)))
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