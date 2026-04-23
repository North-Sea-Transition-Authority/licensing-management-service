package uk.co.nstauthority.licensingmanagementservice.energyportal.fox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.configuration.FoxRedirectConfiguration;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;

@ExtendWith(MockitoExtension.class)
class FoxRedirectServiceTest {

  private static final String EPAS_REDIRECT_URL = "https://test.itportal.dev.fivium.co.uk/fox/nsta/EPAS_REDIRECT";

  private static final String REDIRECT_URL = "https://test.itportal.dev.fivium.co.uk/fox/nsta/LMS_REDIRECT";

  @Mock
  private FoxRedirectConfiguration foxRedirectConfiguration;

  @InjectMocks
  private FoxRedirectService foxRedirectService;

  @Test
  void getViewPearsLicenceUrl() {
    var licence = LicenceTestUtil.builder()
        .withLicenceReference("P123")
        .build();

    when(foxRedirectConfiguration.epasRedirectUrl()).thenReturn(EPAS_REDIRECT_URL);
    when(foxRedirectConfiguration.pearsRedirectUrl()).thenReturn(REDIRECT_URL);

    var result = foxRedirectService.getViewPearsLicenceUrl(licence);

    assertThat(result).isEqualTo(
        EPAS_REDIRECT_URL + FoxRedirectService.VIEW_LICENCE_URL.formatted(REDIRECT_URL, licence.getLicenceReference())
    );
  }
}

