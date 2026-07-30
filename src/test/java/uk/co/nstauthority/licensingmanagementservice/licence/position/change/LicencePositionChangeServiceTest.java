package uk.co.nstauthority.licensingmanagementservice.licence.position.change;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;

@ExtendWith(MockitoExtension.class)
class LicencePositionChangeServiceTest {

  @Mock
  private LicencePositionChangeRepository licencePositionChangeRepository;

  @InjectMocks
  private LicencePositionChangeService licencePositionChangeService;

  @Captor
  private ArgumentCaptor<LicencePositionChange> changeCaptor;

  @Test
  void findByLicencePositionIn() {
    var position = LicencePositionTestUtil.newBuilder().build();
    var changes = List.of(LicencePositionChangeTestUtil.newBuilder().withLicencePosition(position).build());

    when(licencePositionChangeRepository.findByLicencePositionIn(List.of(position))).thenReturn(changes);

    assertThat(licencePositionChangeService.findByLicencePositionIn(List.of(position))).isEqualTo(changes);
  }

  @Test
  void findByLicencePositionId() {
    var licencePositionId = UUID.randomUUID();
    var position = LicencePositionTestUtil.newBuilder().withId(licencePositionId).build();
    var changes = List.of(LicencePositionChangeTestUtil.newBuilder().withLicencePosition(position).build());

    when(licencePositionChangeRepository.findByLicencePosition_Id(licencePositionId)).thenReturn(changes);

    assertThat(licencePositionChangeService.findByLicencePositionId(licencePositionId)).isEqualTo(changes);
  }

  @Test
  void findById_whenFound() {
    var id = UUID.randomUUID();
    var change = LicencePositionChangeTestUtil.newBuilder().withId(id).build();

    when(licencePositionChangeRepository.findById(id)).thenReturn(Optional.of(change));

    assertThat(licencePositionChangeService.findById(id)).contains(change);
  }

  @Test
  void findById_whenNotFound() {
    var id = UUID.randomUUID();

    when(licencePositionChangeRepository.findById(id)).thenReturn(Optional.empty());

    assertThat(licencePositionChangeService.findById(id)).isEmpty();
  }

  @Test
  void createLicencePositionChange() {
    var position = LicencePositionTestUtil.newBuilder().build();
    var administratorChange = LicenceOperation.newAdministratorChange().withOperator(1).build();

    licencePositionChangeService.createLicencePositionChange(
        position, List.of(administratorChange), 1, LicencePositionChangeStatus.CONSENTED);

    verify(licencePositionChangeRepository).save(changeCaptor.capture());

    var saved = changeCaptor.getValue();
    assertThat(saved.getLicencePosition()).isEqualTo(position);
    assertThat(saved.getOperations()).isEqualTo(List.of(administratorChange));
    assertThat(saved.getChangeOrder()).isEqualTo(1L);
    assertThat(saved.getStatus()).isEqualTo(LicencePositionChangeStatus.CONSENTED);
  }

  @Test
  void deleteForPositions_whenEmpty() {
    licencePositionChangeService.deleteForPositions(List.of());

    verifyNoInteractions(licencePositionChangeRepository);
  }

  @Test
  void deleteForPositions() {
    var positions = List.of(LicencePositionTestUtil.newBuilder().build());
    var changes = List.of(new LicencePositionChange(), new LicencePositionChange());
    when(licencePositionChangeRepository.findByLicencePositionIn(positions)).thenReturn(changes);

    licencePositionChangeService.deleteForPositions(positions);

    verify(licencePositionChangeRepository).deleteAll(changes);
  }
}