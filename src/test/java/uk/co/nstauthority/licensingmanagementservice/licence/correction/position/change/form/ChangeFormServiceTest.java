package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.form;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.AddPositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenseeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.VisibleLicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;

@ExtendWith(MockitoExtension.class)
class ChangeFormServiceTest {

  private static final LicenceCorrection PRODUCTION_CORRECTION = correctionOf(LicenceType.SEAWARD_PRODUCTION);
  private static final LicenceCorrection CARBON_STORAGE_CORRECTION = correctionOf(LicenceType.CARBON_STORAGE);

  @Mock
  private LicencePositionChangeService licencePositionChangeService;

  private final ChangeFormHandler<?> administratorHandler =
      handler(AddPositionChangeType.ADMINISTRATOR, AdministratorOperation.class);
  private final ChangeFormHandler<?> licenseeHandler =
      handler(AddPositionChangeType.LICENSEE, LicenseeOperation.class);

  @Test
  void getHandler_whenChangeTypeHasAvailableHandler_thenReturnsIt() {
    var service = service();

    var result = service.getHandler(PRODUCTION_CORRECTION, AddPositionChangeType.LICENSEE);

    assertThat(result).isSameAs(licenseeHandler);
  }

  @Test
  void getHandler_whenChangeTypeHasNoHandler_thenNotFound() {
    var service = service();

    assertThatThrownBy(() -> service.getHandler(PRODUCTION_CORRECTION, AddPositionChangeType.SET_EQUITY))
        .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
  }

  @Test
  void getHandler_whenChangeTypeIsNotAvailableForLicence_thenForbidden() {
    var service = service();

    assertThatThrownBy(() -> service.getHandler(CARBON_STORAGE_CORRECTION, AddPositionChangeType.ADMINISTRATOR))
        .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
  }

  @Test
  void getHandler_whenLiveChangeCarriesHandledOperation_thenReturnsItsHandler() {
    var service = service();
    var liveChange = LicencePositionChangeTestUtil.newBuilder()
        .withId(UUID.randomUUID())
        .withOperations(List.of(
            LicenceOperation.newSubAreaOperation().withBlockFeatureId(UUID.randomUUID()).build(),
            LicenceOperation.newLicenseeOperation().withLicenseesToAdd(List.of(1)).withLicenseesToRemove(List.of()).build()
        ))
        .build();

    var result = service.getHandler(PRODUCTION_CORRECTION, liveChange);

    assertThat(result).isSameAs(licenseeHandler);
  }

  @Test
  void getHandler_whenLiveChangeCarriesNoHandledOperation_thenNotFound() {
    var service = service();
    var liveChange = LicencePositionChangeTestUtil.newBuilder()
        .withId(UUID.randomUUID())
        .withOperations(List.of(LicenceOperation.newSubAreaOperation().withBlockFeatureId(UUID.randomUUID()).build()))
        .build();

    assertThatThrownBy(() -> service.getHandler(PRODUCTION_CORRECTION, liveChange))
        .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
  }

  @Test
  void getHandler_whenLiveChangeIsNotAvailableForLicence_thenForbidden() {
    var service = service();
    var liveChange = LicencePositionChangeTestUtil.newBuilder()
        .withId(UUID.randomUUID())
        .withOperations(List.of(LicenceOperation.newAdministratorChange().withOperator(1).build()))
        .build();

    assertThatThrownBy(() -> service.getHandler(CARBON_STORAGE_CORRECTION, liveChange))
        .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
  }

  @Test
  void checkNoLiveChangeOfType_whenPositionHasLiveChangeOfType_thenConflict() {
    var service = service();
    var position = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();
    when(licencePositionChangeService.changeExists(position.getId(), AdministratorOperation.class)).thenReturn(true);

    assertThatThrownBy(() -> service.checkNoLiveChangeOfType(administratorHandler, position))
        .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
  }

  @Test
  void checkNoLiveChangeOfType_whenPositionHasNoLiveChangeOfType_thenPasses() {
    var service = service();
    var position = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();
    when(licencePositionChangeService.changeExists(position.getId(), AdministratorOperation.class)).thenReturn(false);

    assertThatNoException().isThrownBy(() -> service.checkNoLiveChangeOfType(administratorHandler, position));
  }

  @Test
  void constructor_whenTwoHandlersShareOperationType_thenFails() {
    var duplicate = handler(AddPositionChangeType.STORAGE_OPERATOR, LicenseeOperation.class);
    var handlers = List.<ChangeFormHandler<?>>of(licenseeHandler, duplicate);

    assertThatThrownBy(() -> new ChangeFormService(licencePositionChangeService, handlers))
        .isInstanceOf(IllegalStateException.class);
  }

  private ChangeFormService service() {
    return new ChangeFormService(licencePositionChangeService, List.of(administratorHandler, licenseeHandler));
  }

  private static ChangeFormHandler<?> handler(
      AddPositionChangeType changeType,
      Class<? extends VisibleLicenceOperation> operationType
  ) {
    ChangeFormHandler<?> handler = mock(ChangeFormHandler.class);
    when(handler.changeType()).thenReturn(changeType);
    doReturn(operationType).when(handler).operationType();
    return handler;
  }

  private static LicenceCorrection correctionOf(LicenceType licenceType) {
    return LicenceCorrectionTestUtil.newBuilder()
        .withLicence(LicenceTestUtil.builder().withLicenceType(licenceType).build())
        .build();
  }
}
