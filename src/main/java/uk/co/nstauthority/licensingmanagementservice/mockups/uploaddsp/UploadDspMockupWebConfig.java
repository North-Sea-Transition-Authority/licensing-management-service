package uk.co.nstauthority.licensingmanagementservice.mockups.uploaddsp;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Profile("mockups")
class UploadDspMockupWebConfig implements WebMvcConfigurer {

  private final UploadDspMockupNavigationInterceptor navigationInterceptor;

  UploadDspMockupWebConfig(UploadDspMockupNavigationInterceptor navigationInterceptor) {
    this.navigationInterceptor = navigationInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(navigationInterceptor).addPathPatterns("/mockups/upload-dsp/" + "**");
  }
}
