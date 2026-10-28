package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subareaequity;

import java.util.List;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaSetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SetEquityRow;

@Service
public class SubareaSetEquityCorrectionService {

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final OrganisationUnitQueryService organisationUnitQueryService;

  SubareaSetEquityCorrectionService(
      LicencePositionCorrectionService licencePositionCorrectionService,
      OrganisationUnitQueryService organisationUnitQueryService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.organisationUnitQueryService = organisationUnitQueryService;
  }

  public List<SetEquityRow> getSubareaSetEquityViews(List<SubareaSetEquityOperation> operations) {
    var organisationIds = operations.stream().map(SubareaSetEquityOperation::transferTo).toList();
    var organisationNames = organisationUnitQueryService.getOrganisationUnitNamesByIds(organisationIds);

    return operations.stream()
        .map(operation -> new SetEquityRow(
            organisationNames.getOrDefault(operation.transferTo(), ""),
            operation.equity()
        ))
        .toList();
  }

  List<SubareaSetEquityOperation> getCommittedSubareaSetEquityOperations(
      LicencePositionCorrection licencePositionCorrection,
      SubareaOption subareaOption
  ) {
    return licencePositionCorrectionService
        .getAddOperationsOfType(licencePositionCorrection.getPayload().changes(), SubareaSetEquityOperation.class)
        .stream()
        .filter(isForSubarea(subareaOption))
        .toList();
  }

  List<SubareaSetEquityOperation> getCommittedSubareaSetEquityOperationsForExecutedPosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      SubareaOption subareaOption
  ) {
    return licencePositionCorrectionService.findUpdatePositionCorrection(licenceCorrection, licencePosition)
        .map(positionCorrection -> getCommittedSubareaSetEquityOperations(positionCorrection, subareaOption))
        .orElse(List.of());
  }

  @Transactional
  void commitSubareaSetEquity(
      LicencePositionCorrection licencePositionCorrection,
      SubareaOption subareaOption,
      List<SubareaSetEquityOperation> operations
  ) {
    licencePositionCorrectionService.replaceAddChangeFor(
        licencePositionCorrection,
        SubareaSetEquityOperation.class,
        isForSubarea(subareaOption),
        withCurrentSubarea(operations, subareaOption)
    );
  }

  @Transactional
  void commitSubareaSetEquityForExecutedPosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      SubareaOption subareaOption,
      List<SubareaSetEquityOperation> operations
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getOrBuildUpdatePositionCorrection(licenceCorrection, licencePosition);

    // the subarea may have been created by a live change, so the equity is ordered after the live changes too
    licencePositionCorrectionService.replaceAddChangeForExecutedPosition(
        licenceCorrection,
        licencePosition,
        positionCorrection,
        SubareaSetEquityOperation.class,
        isForSubarea(subareaOption),
        withCurrentSubarea(operations, subareaOption)
    );
  }

  private static Predicate<SubareaSetEquityOperation> isForSubarea(SubareaOption subareaOption) {
    return operation -> operation.subareaKey().equals(subareaOption.key());
  }

  private static List<SubareaSetEquityOperation> withCurrentSubarea(
      List<SubareaSetEquityOperation> operations,
      SubareaOption subareaOption
  ) {
    return operations.stream()
        .map(operation -> operation.withSubarea(subareaOption.subarea()))
        .toList();
  }
}