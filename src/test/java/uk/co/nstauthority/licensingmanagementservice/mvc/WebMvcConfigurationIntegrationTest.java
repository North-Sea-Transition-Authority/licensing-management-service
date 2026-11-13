package uk.co.nstauthority.licensingmanagementservice.mvc;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.xml.JacksonXmlHttpMessageConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import uk.co.nstauthority.licensingmanagementservice.LicensingManagementServiceApplication;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

@IntegrationTest
@ContextConfiguration(classes = LicensingManagementServiceApplication.class)
class WebMvcConfigurationIntegrationTest {

  @Autowired
  private RequestMappingHandlerAdapter requestMappingHandlerAdapter;

  @Test
  void configureMessageConverters() {
    var converters = requestMappingHandlerAdapter.getMessageConverters();

    assertThat(converters).noneMatch(JacksonXmlHttpMessageConverter.class::isInstance);
  }
}
