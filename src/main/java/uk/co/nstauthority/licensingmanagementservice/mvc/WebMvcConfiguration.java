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
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceArgumentResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailArgumentResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityArgumentResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailArgumentResolver;
import uk.co.nstauthority.licensingmanagementservice.mvc.error.ErrorSummaryItemsHandlerInterceptor;
import uk.co.nstauthority.licensingmanagementservice.teams.management.access.TeamManagementHandlerInterceptor;
import uk.co.nstauthority.licensingmanagementservice.xyzapplication.XyzApplicationArgumentResolver;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

  private static final String STATIC_ASSETS_PATH = "/assets/**";

  private final ServiceUserDetailArgumentResolver serviceUserDetailArgumentResolver;
  private final XyzApplicationArgumentResolver xyzApplicationArgumentResolver;
  private final TeamManagementHandlerInterceptor teamManagementHandlerInterceptor;
  private final AccessHandlerInterceptor accessHandlerInterceptor;
  private final ErrorSummaryItemsHandlerInterceptor errorSummaryItemsHandlerInterceptor;
  private final LicenceArgumentResolver licenceArgumentResolver;
  private final ScheduleWorkProgrammeApplicationDetailArgumentResolver scheduleWorkProgrammeApplicationDetailArgumentResolver;
  private final WorkProgrammeActivityArgumentResolver workProgrammeActivityArgumentResolver;
  private final LicenceScheduleDetailArgumentResolver licenceScheduleDetailArgumentResolver;

  public WebMvcConfiguration(
      ServiceUserDetailArgumentResolver serviceUserDetailArgumentResolver,
      XyzApplicationArgumentResolver xyzApplicationArgumentResolver,
      TeamManagementHandlerInterceptor teamManagementHandlerInterceptor,
      AccessHandlerInterceptor accessHandlerInterceptor,
      ErrorSummaryItemsHandlerInterceptor errorSummaryItemsHandlerInterceptor,
      LicenceArgumentResolver licenceArgumentResolver,
      ScheduleWorkProgrammeApplicationDetailArgumentResolver scheduleWorkProgrammeApplicationDetailArgumentResolver,
      WorkProgrammeActivityArgumentResolver workProgrammeActivityArgumentResolver,
      LicenceScheduleDetailArgumentResolver licenceScheduleDetailArgumentResolver
  ) {
    this.serviceUserDetailArgumentResolver = serviceUserDetailArgumentResolver;
    this.xyzApplicationArgumentResolver = xyzApplicationArgumentResolver;
    this.teamManagementHandlerInterceptor = teamManagementHandlerInterceptor;
    this.accessHandlerInterceptor = accessHandlerInterceptor;
    this.errorSummaryItemsHandlerInterceptor = errorSummaryItemsHandlerInterceptor;
    this.licenceArgumentResolver = licenceArgumentResolver;
    this.scheduleWorkProgrammeApplicationDetailArgumentResolver = scheduleWorkProgrammeApplicationDetailArgumentResolver;
    this.workProgrammeActivityArgumentResolver = workProgrammeActivityArgumentResolver;
    this.licenceScheduleDetailArgumentResolver = licenceScheduleDetailArgumentResolver;
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
    resolvers.add(xyzApplicationArgumentResolver);
    resolvers.add(licenceArgumentResolver);
    resolvers.add(scheduleWorkProgrammeApplicationDetailArgumentResolver);
    resolvers.add(licenceScheduleDetailArgumentResolver);
    resolvers.add(workProgrammeActivityArgumentResolver);
  }
}