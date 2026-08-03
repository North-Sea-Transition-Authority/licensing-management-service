package uk.co.nstauthority.licensingmanagementservice.licence.position.change;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;

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
  void getByIdOrThrow() {
    var licencePositionChangeId = UUID.randomUUID();

    var licencePositionChange = new LicencePositionChange();

    when(licencePositionChangeRepository.findById(licencePositionChangeId)).thenReturn(Optional.of(licencePositionChange));

    assertThat(licencePositionChangeService.getByIdOrThrow(licencePositionChangeId)).isEqualTo(licencePositionChange);
  }

  @Test
  void getByIdOrThrow_licencePositionChangeNotFound() {
    var licencePositionChangeId = UUID.randomUUID();

    when(licencePositionChangeRepository.findById(licencePositionChangeId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> licencePositionChangeService.getByIdOrThrow(licencePositionChangeId)).isInstanceOf(
        LmsEntityNotFoundException.class);
  }

  @Test
  void changeExists_whenPositionHasMatchingOperation_returnsTrue() {
    var licencePositionId = UUID.randomUUID();
    var change = LicencePositionChangeTestUtil.newBuilder()
        .withOperations(List.of(new SetEquityOperation(1, BigDecimal.valueOf(40))))
        .build();

    when(licencePositionChangeRepository.findByLicencePosition_Id(licencePositionId)).thenReturn(List.of(change));

    assertThat(licencePositionChangeService.changeExists(licencePositionId, SetEquityOperation.class)).isTrue();
  }

  @Test
  void changeExists_whenPositionHasOnlyOtherOperationType_returnsFalse() {
    var licencePositionId = UUID.randomUUID();
    var change = LicencePositionChangeTestUtil.newBuilder()
        .withOperations(List.of(LicenceOperation.newAdministratorChange().withOperator(1).build()))
        .build();

    when(licencePositionChangeRepository.findByLicencePosition_Id(licencePositionId)).thenReturn(List.of(change));

    assertThat(licencePositionChangeService.changeExists(licencePositionId, SetEquityOperation.class)).isFalse();
    assertThat(licencePositionChangeService.changeExists(licencePositionId, AdministratorOperation.class)).isTrue();
  }

  @Test
  void changeExists_whenPositionHasNoChanges_returnsFalse() {
    var licencePositionId = UUID.randomUUID();

    when(licencePositionChangeRepository.findByLicencePosition_Id(licencePositionId)).thenReturn(List.of());

    assertThat(licencePositionChangeService.changeExists(licencePositionId, SetEquityOperation.class)).isFalse();
  }

  @Test
  void changeExists_whenLicencePositionIdNull_returnsFalseWithoutQuery() {
    assertThat(licencePositionChangeService.changeExists(null, SetEquityOperation.class)).isFalse();

    verifyNoInteractions(licencePositionChangeRepository);
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