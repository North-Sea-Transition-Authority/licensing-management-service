package uk.co.nstauthority.template;

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
import uk.co.nstauthority.template.authentication.ServiceLogoutSuccessHandler;
import uk.co.nstauthority.template.authentication.ServiceUserDetail;
import uk.co.nstauthority.template.authentication.ServiceUserDetailArgumentResolver;
import uk.co.nstauthority.template.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.template.authentication.UserDetailService;
import uk.co.nstauthority.template.authentication.saml.SamlResponseParser;
import uk.co.nstauthority.template.authorisation.AccessHandlerInterceptor;
import uk.co.nstauthority.template.authorisation.rules.HasRolesInTeamTypeInterceptorRule;
import uk.co.nstauthority.template.branding.CustomerConfigurationProperties;
import uk.co.nstauthority.template.branding.ServiceConfigurationProperties;
import uk.co.nstauthority.template.configuration.EnergyPortalConfiguration;
import uk.co.nstauthority.template.configuration.ErrorConfigurationProperties;
import uk.co.nstauthority.template.configuration.SamlProperties;
import uk.co.nstauthority.template.configuration.WebSecurityConfiguration;
import uk.co.nstauthority.template.energyportal.epa.EpaRequestHandler;
import uk.co.nstauthority.template.energyportal.organisationgroup.OrganisationGroupQueryService;
import uk.co.nstauthority.template.hibernate.HibernateQueryCounter;
import uk.co.nstauthority.template.mvc.ControllerAdviceService;
import uk.co.nstauthority.template.mvc.DefaultPageControllerAdvice;
import uk.co.nstauthority.template.mvc.PostAuthenticationRequestMdcFilter;
import uk.co.nstauthority.template.mvc.RequestLogFilter;
import uk.co.nstauthority.template.mvc.WebMvcConfiguration;
import uk.co.nstauthority.template.mvc.error.ErrorService;
import uk.co.nstauthority.template.mvc.error.ErrorSummaryItemsHandlerInterceptor;
import uk.co.nstauthority.template.teams.TeamQueryService;
import uk.co.nstauthority.template.teams.TeamType;
import uk.co.nstauthority.template.teams.management.TeamManagementService;
import uk.co.nstauthority.template.teams.management.access.TeamManagementHandlerInterceptor;
import uk.co.nstauthority.template.topnavigation.TopNavigationService;
import uk.co.nstauthority.template.xyzapplication.XyzApplicationService;
import uk.co.nstauthority.template.xyzapplication.XyzApplicationArgumentResolver;

@WebMvcTest
@AutoConfigureMockMvc
@Import({
    AbstractControllerTest.TestConfig.class,
    ControllerAdviceService.class,
    DefaultPageControllerAdvice.class,
    ErrorService.class,
    ServiceUserDetailArgumentResolver.class,
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
    XyzApplicationArgumentResolver.class
})
@EnableConfigurationProperties({
  SamlProperties.class,
  EnergyPortalConfiguration.class,
  ErrorConfigurationProperties.class,
  CustomerConfigurationProperties.class,
  ServiceConfigurationProperties.class
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

    when(teamManagementService.getTeamTypesUserIsMemberOf(regulatorUser.wuaId())).thenReturn(Set.of(TeamType.REGULATOR));
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
