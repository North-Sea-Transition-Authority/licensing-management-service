package uk.co.nstauthority.licensingmanagementservice;

import static org.mockito.Mockito.when;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateService;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceLogoutSuccessHandler;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailArgumentResolver;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.authentication.saml.SamlResponseParser;
import uk.co.nstauthority.licensingmanagementservice.authorisation.AccessHandlerInterceptor;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.HasAnyRoleInTeamTypeInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.HasRolesInTeamTypeInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.InvokingUserCanStartApplicationInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.LicenceActionEndPointInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication.ContinuationApplicationHasStatusInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication.InvokingUserCanAccessContinuationApplicationInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.InvokingUserCanAccessScheduleApplicationInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.ScheduleAmendmentApplicationHasStatusInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationActionEndPointInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.branding.CustomerConfigurationProperties;
import uk.co.nstauthority.licensingmanagementservice.branding.ServiceConfigurationProperties;
import uk.co.nstauthority.licensingmanagementservice.configuration.AnalyticsConfiguration;
import uk.co.nstauthority.licensingmanagementservice.configuration.AnalyticsConfigurationProperties;
import uk.co.nstauthority.licensingmanagementservice.configuration.EnergyPortalConfiguration;
import uk.co.nstauthority.licensingmanagementservice.configuration.ErrorConfigurationProperties;
import uk.co.nstauthority.licensingmanagementservice.configuration.SamlProperties;
import uk.co.nstauthority.licensingmanagementservice.configuration.WebSecurityConfiguration;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentTemplateArgumentResolver;
import uk.co.nstauthority.licensingmanagementservice.document.search.DocumentTemplateSearchStringToTabConverter;
import uk.co.nstauthority.licensingmanagementservice.energyportal.epa.EpaRequestHandler;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupQueryService;
import uk.co.nstauthority.licensingmanagementservice.hibernate.HibernateQueryCounter;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceArgumentResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationTypeArgumentResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetailArgumentResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.action.LicenceActionService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailArgumentResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityArgumentResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailArgumentResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.action.ScheduleWorkProgrammeApplicationActionService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ControllerAdviceService;
import uk.co.nstauthority.licensingmanagementservice.mvc.DefaultPageControllerAdvice;
import uk.co.nstauthority.licensingmanagementservice.mvc.PostAuthenticationRequestMdcFilter;
import uk.co.nstauthority.licensingmanagementservice.mvc.RequestLogFilter;
import uk.co.nstauthority.licensingmanagementservice.mvc.WebMvcConfiguration;
import uk.co.nstauthority.licensingmanagementservice.mvc.error.ErrorService;
import uk.co.nstauthority.licensingmanagementservice.mvc.error.ErrorSummaryItemsHandlerInterceptor;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementService;
import uk.co.nstauthority.licensingmanagementservice.teams.management.access.TeamManagementHandlerInterceptor;
import uk.co.nstauthority.licensingmanagementservice.topnavigation.TopNavigationService;
import uk.co.nstauthority.licensingmanagementservice.xyzapplication.XyzApplicationArgumentResolver;
import uk.co.nstauthority.licensingmanagementservice.xyzapplication.XyzApplicationService;

@WebMvcTest
@AutoConfigureMockMvc
@Import({
    AbstractControllerTest.TestConfig.class,
    ControllerAdviceService.class,
    DefaultPageControllerAdvice.class,
    ErrorService.class,
    ServiceUserDetailArgumentResolver.class,
    WorkProgrammeActivityArgumentResolver.class,
    XyzApplicationArgumentResolver.class,
    WebSecurityConfiguration.class,
    WebMvcConfiguration.class,
    UserDetailService.class,
    TopNavigationService.class,
    TeamManagementHandlerInterceptor.class,
    AccessHandlerInterceptor.class,
    ErrorSummaryItemsHandlerInterceptor.class,
    RequestLogFilter.class,
    PostAuthenticationRequestMdcFilter.class,
    HasRolesInTeamTypeInterceptorRule.class,
    ScheduleAmendmentApplicationHasStatusInterceptorRule.class,
    XyzApplicationArgumentResolver.class,
    AnalyticsConfiguration.class,
    LicenceArgumentResolver.class,
    ScheduleWorkProgrammeApplicationDetailArgumentResolver.class,
    LicenceContinuationApplicationDetailArgumentResolver.class,
    LicenceScheduleDetailArgumentResolver.class,
    InvokingUserCanAccessScheduleApplicationInterceptorRule.class,
    InvokingUserCanStartApplicationInterceptorRule.class,
    DocumentTemplateSearchStringToTabConverter.class,
    LicenceActionEndPointInterceptorRule.class,
    InvokingUserCanAccessContinuationApplicationInterceptorRule.class,
    ContinuationApplicationHasStatusInterceptorRule.class,
    DocumentTemplateArgumentResolver.class,
    ApplicationTypeArgumentResolver.class,
    HasAnyRoleInTeamTypeInterceptorRule.class,
    ContinuationApplicationHasStatusInterceptorRule.class,
    ScheduleWorkProgrammeApplicationActionEndPointInterceptorRule.class
})
@EnableConfigurationProperties({
    SamlProperties.class,
    EnergyPortalConfiguration.class,
    ErrorConfigurationProperties.class,
    CustomerConfigurationProperties.class,
    ServiceConfigurationProperties.class,
    AnalyticsConfigurationProperties.class,
})
@ActiveProfiles("test")
public abstract class AbstractControllerTest {

  @MockitoBean
  protected SamlResponseParser samlResponseParser;

  @MockitoBean
  protected ServiceLogoutSuccessHandler serviceLogoutSuccessHandler;

  @MockitoBean
  protected TeamManagementService teamManagementService;

  @MockitoBean
  protected TeamQueryService teamQueryService;

  @MockitoBean
  protected XyzApplicationService xyzApplicationService;

  @MockitoBean
  protected WorkProgrammeActivityService workProgrammeActivityService;

  @MockitoBean
  protected LicenceService licenceService;

  @MockitoBean
  protected ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;

  @MockitoBean
  protected LicenceScheduleDetailService licenceScheduleDetailService;

  @MockitoBean
  protected LicenceContinuationService licenceContinuationService;

  @MockitoBean
  protected ApplicationAccessService applicationAccessService;

  @MockitoBean
  protected LicenceActionService licenceActionService;

  @MockitoBean
  protected ScheduleWorkProgrammeApplicationActionService scheduleWorkProgrammeApplicationActionService;

  @Autowired
  protected TeamManagementHandlerInterceptor teamManagementHandlerInterceptor;

  @Autowired
  protected AccessHandlerInterceptor accessHandlerInterceptor;

  @Autowired
  protected ControllerAdviceService controllerAdviceService;

  @Autowired
  protected ErrorService errorService;

  @Autowired
  protected UserDetailService userDetailService;

  @MockitoBean
  protected OrganisationGroupQueryService organisationGroupQueryService;

  @MockitoBean
  protected DocumentTemplateService documentTemplateService;

  @MockitoBean
  protected LicenceScheduleService licenceScheduleService;

  @Autowired
  protected TopNavigationService topNavigationService;

  @Autowired
  protected MockMvc mockMvc;

  protected ServiceUserDetail regulatorUser;
  protected static final Long REGULATOR_USER_WUA_ID = 1L;

  @BeforeEach
  void setupAbstractControllerTest() {
    regulatorUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(REGULATOR_USER_WUA_ID)
        .build();

    when(teamManagementService.getTeamTypesUserIsMemberOf(regulatorUser.wuaId())).thenReturn(Set.of(TeamType.LICENCE_MANAGEMENT));
  }

  @TestConfiguration
  public static class TestConfig {

    @Bean
    public HibernateQueryCounter hibernateQueryInterceptor() {
      return new HibernateQueryCounter();
    }

    @Bean
    public EpaRequestHandler epaRequestHandler() {
      return new EpaRequestHandler();
    }

    @Bean("messageSource")
    public MessageSource messageSource() {
      ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
      messageSource.setBasename("messages");
      messageSource.setDefaultEncoding("UTF-8");
      return messageSource;
    }
  }
}