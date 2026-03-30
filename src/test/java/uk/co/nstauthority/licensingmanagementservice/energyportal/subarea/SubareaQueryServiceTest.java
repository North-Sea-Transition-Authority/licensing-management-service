package uk.co.nstauthority.licensingmanagementservice.energyportal.subarea;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.co.nstauthority.licensingmanagementservice.energyportal.subarea.SubareaQueryService.SUBAREAS_PROJECTION_ROOT;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.energyportalapi.client.subarea.SubareaApi;
import uk.co.fivium.energyportalapi.generated.types.Subarea;

@ExtendWith(MockitoExtension.class)
class SubareaQueryServiceTest {

  @Mock
  private SubareaApi subareaApi;

  @InjectMocks
  private SubareaQueryService subareaQueryService;

  @Test
  void searchSubareasByLicenceIds() {
    var subarea = new Subarea();
    subarea.setId("SA-1");
    subarea.setName("Subarea One");

    var subarea2 = new Subarea();
    subarea2.setId("SA-2");
    subarea2.setName("Subarea Two");

    when(subareaApi.searchExtantSubareasByLicenceIds(
        eq(List.of(100, 200)),
        eq(SUBAREAS_PROJECTION_ROOT),
        any(),
        any()
    )).thenReturn(List.of(subarea, subarea2));

    assertThat(subareaQueryService.searchSubareasByLicenceIds(List.of(100, 200)))
        .usingRecursiveComparison()
        .isEqualTo(List.of(subarea, subarea2));
  }
}
