package uk.co.nstauthority.licensingmanagementservice.mvc;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.ResourceUrlEncodingFilter;
import org.springframework.web.servlet.resource.VersionResourceResolver;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailArgumentResolver;
import uk.co.nstauthority.licensingmanagementservice.authorisation.AccessHandlerInterceptor;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentTemplateArgumentResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceArgumentResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationTypeArgumentResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetailArgumentResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailArgumentResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityArgumentResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailArgumentResolver;
import uk.co.nstauthority.licensingmanagementservice.mvc.error.ErrorSummaryItemsHandlerInterceptor;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.PhasedReleaseInterceptor;
import uk.co.nstauthority.licensingmanagementservice.teams.management.access.TeamManagementHandlerInterceptor;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

  private static final String STATIC_ASSETS_PATH = "/assets/**";

  private final ServiceUserDetailArgumentResolver serviceUserDetailArgumentResolver;
  private final TeamManagementHandlerInterceptor teamManagementHandlerInterceptor;
  private final PhasedReleaseInterceptor phasedReleaseInterceptor;
  private final AccessHandlerInterceptor accessHandlerInterceptor;
  private final ErrorSummaryItemsHandlerInterceptor errorSummaryItemsHandlerInterceptor;
  private final LicenceArgumentResolver licenceArgumentResolver;
  private final ScheduleWorkProgrammeApplicationDetailArgumentResolver scheduleWorkProgrammeApplicationDetailArgumentResolver;
  private final WorkProgrammeActivityArgumentResolver workProgrammeActivityArgumentResolver;
  private final LicenceScheduleDetailArgumentResolver licenceScheduleDetailArgumentResolver;
  private final LicenceContinuationApplicationDetailArgumentResolver licenceContinuationApplicationDetailArgumentResolver;
  private final DocumentTemplateArgumentResolver  documentTemplateArgumentResolver;
  private final ApplicationTypeArgumentResolver applicationTypeArgumentResolver;

  public WebMvcConfiguration(
      ServiceUserDetailArgumentResolver serviceUserDetailArgumentResolver,
      TeamManagementHandlerInterceptor teamManagementHandlerInterceptor,
      PhasedReleaseInterceptor phasedReleaseInterceptor,
      AccessHandlerInterceptor accessHandlerInterceptor,
      ErrorSummaryItemsHandlerInterceptor errorSummaryItemsHandlerInterceptor,
      LicenceArgumentResolver licenceArgumentResolver,
      ScheduleWorkProgrammeApplicationDetailArgumentResolver scheduleWorkProgrammeApplicationDetailArgumentResolver,
      WorkProgrammeActivityArgumentResolver workProgrammeActivityArgumentResolver,
      LicenceScheduleDetailArgumentResolver licenceScheduleDetailArgumentResolver,
      LicenceContinuationApplicationDetailArgumentResolver licenceContinuationApplicationDetailArgumentResolver,
      DocumentTemplateArgumentResolver documentTemplateArgumentResolver,
      ApplicationTypeArgumentResolver applicationTypeArgumentResolver
  ) {
    this.serviceUserDetailArgumentResolver = serviceUserDetailArgumentResolver;
    this.teamManagementHandlerInterceptor = teamManagementHandlerInterceptor;
    this.phasedReleaseInterceptor = phasedReleaseInterceptor;
    this.accessHandlerInterceptor = accessHandlerInterceptor;
    this.errorSummaryItemsHandlerInterceptor = errorSummaryItemsHandlerInterceptor;
    this.licenceArgumentResolver = licenceArgumentResolver;
    this.scheduleWorkProgrammeApplicationDetailArgumentResolver = scheduleWorkProgrammeApplicationDetailArgumentResolver;
    this.workProgrammeActivityArgumentResolver = workProgrammeActivityArgumentResolver;
    this.licenceScheduleDetailArgumentResolver = licenceScheduleDetailArgumentResolver;
    this.licenceContinuationApplicationDetailArgumentResolver = licenceContinuationApplicationDetailArgumentResolver;
    this.documentTemplateArgumentResolver = documentTemplateArgumentResolver;
    this.applicationTypeArgumentResolver = applicationTypeArgumentResolver;
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler(STATIC_ASSETS_PATH)
        .addResourceLocations("classpath:/public/assets/")
        .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS))
        .resourceChain(false)
        .addResolver(new VersionResourceResolver().addContentVersionStrategy("/**"));
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new ResponseBufferSizeHandlerInterceptor())
        .excludePathPatterns(STATIC_ASSETS_PATH);
    // Phased go-live gate — runs before the access rules so a locked feature 404s rather than leaking a 403
    registry.addInterceptor(phasedReleaseInterceptor)
        .excludePathPatterns(STATIC_ASSETS_PATH);
    registry.addInterceptor(accessHandlerInterceptor)
        .excludePathPatterns(STATIC_ASSETS_PATH);
    registry.addInterceptor(errorSummaryItemsHandlerInterceptor)
        .excludePathPatterns(STATIC_ASSETS_PATH);

    registry.addInterceptor(teamManagementHandlerInterceptor)
        .addPathPatterns("/team-management/**");
  }

  @Bean
  public ResourceUrlEncodingFilter resourceUrlEncodingFilter() {
    return new ResourceUrlEncodingFilter();
  }

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(serviceUserDetailArgumentResolver);
    resolvers.add(licenceArgumentResolver);
    resolvers.add(scheduleWorkProgrammeApplicationDetailArgumentResolver);
    resolvers.add(licenceScheduleDetailArgumentResolver);
    resolvers.add(workProgrammeActivityArgumentResolver);
    resolvers.add(licenceContinuationApplicationDetailArgumentResolver);
    resolvers.add(documentTemplateArgumentResolver);
    resolvers.add(applicationTypeArgumentResolver);
  }
}