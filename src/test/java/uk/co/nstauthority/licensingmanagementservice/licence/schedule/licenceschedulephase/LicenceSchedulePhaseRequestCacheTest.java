package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceSchedulePhaseTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceScheduleTermTestUtil;

@ExtendWith(MockitoExtension.class)
class LicenceSchedulePhaseRequestCacheTest {

  @Mock
  private LicenceSchedulePhaseService licenceSchedulePhaseService;

  @InjectMocks
  private LicenceSchedulePhaseRequestCache licenceSchedulePhaseRequestCache;

  @Test
  void getPhasesByTerm() {
    var term = LicenceScheduleTermTestUtil.builder().build();
    var phases = List.of(LicenceSchedulePhaseTestUtil.builder().build());
    when(licenceSchedulePhaseService.getPhasesByTerm(term)).thenReturn(phases);

    var result = licenceSchedulePhaseRequestCache.getPhasesByTerm(term);

    assertThat(result).isEqualTo(phases);
  }
}
