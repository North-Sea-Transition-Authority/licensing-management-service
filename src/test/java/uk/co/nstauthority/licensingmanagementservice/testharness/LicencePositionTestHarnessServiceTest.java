package uk.co.nstauthority.licensingmanagementservice.testharness;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;

@ExtendWith(MockitoExtension.class)
class LicencePositionTestHarnessServiceTest {

  private static final Licence LICENCE = LicenceTestUtil.builder().build();

  @Mock
  private LicencePositionRepository licencePositionRepository;

  @Mock
  private LicencePositionChangeService licencePositionChangeService;

  @InjectMocks
  private LicencePositionTestHarnessService licencePositionTestHarnessService;

  @Test
  void clearPositionsForLicence_whenNoPositions() {
    when(licencePositionRepository.findByLicence(LICENCE)).thenReturn(List.of());

    licencePositionTestHarnessService.clearPositionsForLicence(LICENCE);

    verify(licencePositionChangeService, never()).deleteForPositions(any());
    verify(licencePositionRepository, never()).deleteAll(anyList());
  }

  @Test
  void clearPositionsForLicence() {
    var positions = List.of(
        LicencePositionTestUtil.newBuilder().build(),
        LicencePositionTestUtil.newBuilder().build());

    when(licencePositionRepository.findByLicence(LICENCE)).thenReturn(positions);

    licencePositionTestHarnessService.clearPositionsForLicence(LICENCE);

    var inOrder = inOrder(licencePositionChangeService, licencePositionRepository);
    inOrder.verify(licencePositionChangeService).deleteForPositions(positions);
    inOrder.verify(licencePositionRepository).deleteAll(positions);
  }
}
