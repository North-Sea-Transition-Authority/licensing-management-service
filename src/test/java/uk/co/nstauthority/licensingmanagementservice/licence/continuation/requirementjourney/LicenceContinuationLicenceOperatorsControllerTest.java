package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;
import static uk.co.nstauthority.licensingmanagementservice.util.RedirectedToLoginUrlMatcher.redirectionToLoginUrl;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.fivium.energyportalapi.generated.types.Licence;
import uk.co.fivium.energyportalapi.generated.types.LicenceBlock;
import uk.co.fivium.energyportalapi.generated.types.OrganisationUnit;
import uk.co.fivium.energyportalapi.generated.types.Subarea;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist.LicenceContinuationApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.AuthorisationSecurityTest;

@WebMvcTest(LicenceContinuationLicenceOperatorsController.class)
@ContextConfiguration(classes = LicenceContinuationLicenceOperatorsController.class)
class LicenceContinuationLicenceOperatorsControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceContinuationLicenceOperatorsService licenceContinuationLicenceOperatorsService;

  @MockitoBean
  private LicenceContinuationLicenceOperatorsValidator validator;

  private final UUID applicationId = UUID.randomUUID();
  private LicenceContinuationApplicationDetail applicationDetail;
  private Subarea subarea;

  @BeforeEach
  void setUp() {
    applicationDetail = new LicenceContinuationApplicationDetail();
    applicationDetail.setId(applicationId);
    applicationDetail.setStatus(LicenceContinuationApplicationStatus.DRAFT);

    subarea = new Subarea();
    subarea.setId("test");
    subarea.setName("Test Subarea");
    subarea.setLicence(new Licence());
    OrganisationUnit organisationUnit = new OrganisationUnit();
    organisationUnit.setName("test");
    subarea.setOperator(organisationUnit);
    subarea.setLicenceBlock(new LicenceBlock("test", 1, "test", "test"));
  }

  @AuthorisationSecurityTest
  void renderForm_whenNotLoggedIn_thenRedirectToLoginPage() throws Exception {
    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceContinuationLicenceOperatorsController.class).renderForm(applicationId, null)))
        )
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderForm_whenValid_rendersCorrectly() throws Exception {
    var form = new LicenceContinuationLicenceOperatorsForm();
    List<Subarea> subareas = List.of(subarea);

    when(licenceContinuationLicenceOperatorsService.getSubareasForApplication(any())).thenReturn(subareas);
    when(licenceContinuationLicenceOperatorsService.hasMissingOperators(any())).thenReturn(true);
    when(licenceContinuationLicenceOperatorsService.getLicenceContinuationLicenceOperatorsForm(any())).thenReturn(form);
    when(licenceContinuationService.getDetailByIdOrThrow(applicationDetail.getId())).thenReturn(applicationDetail);
    when(applicationAccessService.userHasAccessToApplication(String.valueOf(applicationDetail.getId()), ApplicationType.CONTINUATION_APPLICATION, null, regulatorUser.wuaId())).thenReturn(true);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceContinuationLicenceOperatorsController.class).renderForm(applicationId, applicationDetail)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/continuation/licenceOperator/licenceContinuationLicenceOperators"))
        .andExpect(model().attribute("pageTitle", "Licence operators"))
        .andExpect(model().attribute("form", form))
        .andExpect(model().attribute("hasMissingOperators", true))
        .andExpect(model().attribute("subareas", subareas))
        .andExpect(model().attribute("cancelUrl", ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class).getTaskList(applicationId, null, null))));
  }

  @Test
  void submitForm_whenInvalid_returnsToForm() throws Exception {
    List<Subarea> subareas = List.of(subarea);
    when(licenceContinuationLicenceOperatorsService.getSubareasForApplication(any())).thenReturn(subareas);
    when(licenceContinuationLicenceOperatorsService.hasMissingOperators(any())).thenReturn(true);
    when(validator.isValid(any(), anyBoolean())).thenReturn(false);
    when(licenceContinuationService.getDetailByIdOrThrow(applicationDetail.getId())).thenReturn(applicationDetail);
    when(applicationAccessService.userHasAccessToApplication(String.valueOf(applicationDetail.getId()), ApplicationType.CONTINUATION_APPLICATION, null, regulatorUser.wuaId())).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceContinuationLicenceOperatorsController.class).submitForm(applicationId, applicationDetail, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/continuation/licenceOperator/licenceContinuationLicenceOperators"))
        .andExpect(model().attributeExists("form"))
        .andExpect(model().attribute("hasMissingOperators", true))
        .andExpect(model().attribute("subareas", subareas));
  }

  @Test
  void submitForm_whenValid_savesAndRedirectsToTaskList() throws Exception {
    when(licenceContinuationLicenceOperatorsService.getSubareasForApplication(any())).thenReturn(List.of());
    when(licenceContinuationLicenceOperatorsService.hasMissingOperators(any())).thenReturn(false);
    when(validator.isValid(any(), anyBoolean())).thenReturn(true);
    when(licenceContinuationService.getDetailByIdOrThrow(applicationDetail.getId())).thenReturn(applicationDetail);
    when(applicationAccessService.userHasAccessToApplication(String.valueOf(applicationDetail.getId()), ApplicationType.CONTINUATION_APPLICATION, null, regulatorUser.wuaId())).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceContinuationLicenceOperatorsController.class).submitForm(applicationId, null, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class).getTaskList(applicationId, null, null))));

    verify(licenceContinuationLicenceOperatorsService).saveLicenceContinuationLicenceOperatorsForm(any(), any());
  }
}