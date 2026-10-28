package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subareaequity;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.spatial.LicencePositionSpatialService;

@Service
public class SubareaEquityOptionsService {

  private final LicencePositionSpatialService licencePositionSpatialService;

  public SubareaEquityOptionsService(LicencePositionSpatialService licencePositionSpatialService) {
    this.licencePositionSpatialService = licencePositionSpatialService;
  }

  public Map<String, String> getSubareaOptions(LicenceCorrection correction, LicencePosition licencePosition) {
    return toOptionMap(getOptions(correction, licencePosition.getId()));
  }

  public Map<String, String> getSubareaOptions(LicencePositionCorrection licencePositionCorrection) {
    return toOptionMap(getOptions(
        licencePositionCorrection.getLicenceCorrection(),
        licencePositionCorrection.getPositionId()
    ));
  }

  SubareaOption getSubareaOptionOrThrow(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      String subareaKey
  ) {
    return findOptionOrThrow(getOptions(correction, licencePosition.getId()), subareaKey);
  }

  SubareaOption getSubareaOptionOrThrow(LicencePositionCorrection licencePositionCorrection, String subareaKey) {
    return findOptionOrThrow(
        getOptions(licencePositionCorrection.getLicenceCorrection(), licencePositionCorrection.getPositionId()),
        subareaKey
    );
  }

  private List<SubareaOption> getOptions(LicenceCorrection correction, UUID licencePositionId) {
    return licencePositionSpatialService.getBlockSubareasGoingIntoChange(correction, licencePositionId, null)
        .stream()
        .flatMap(blockSubareas -> blockSubareas.subareas()
            .stream()
            .map(subarea -> new SubareaOption(blockSubareas.block(), subarea)))
        .toList();
  }

  private static Map<String, String> toOptionMap(List<SubareaOption> options) {
    return options.stream()
        .sorted(Comparator.comparing(SubareaOption::label))
        .collect(Collectors.toMap(
            SubareaOption::key,
            SubareaOption::label,
            (firstLabel, duplicateLabel) -> firstLabel,
            LinkedHashMap::new
        ));
  }

  private static SubareaOption findOptionOrThrow(List<SubareaOption> options, String subareaKey) {
    return options.stream()
        .filter(option -> option.key().equals(subareaKey))
        .findFirst()
        .orElseThrow(() -> new LmsEntityNotFoundException("subarea", subareaKey));
  }
}