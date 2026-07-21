package uk.co.nstauthority.licensingmanagementservice.licence.schedule.common;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;

@Service
public class LicenceScheduleRelativeOptionsService {

  private final LicenceScheduleTermService licenceScheduleTermService;
  private final LicenceSchedulePhaseService licenceSchedulePhaseService;

  public LicenceScheduleRelativeOptionsService(
      LicenceScheduleTermService licenceScheduleTermService,
      LicenceSchedulePhaseService licenceSchedulePhaseService
  ) {
    this.licenceScheduleTermService = licenceScheduleTermService;
    this.licenceSchedulePhaseService = licenceSchedulePhaseService;
  }

  public Map<String, String> getScheduleTermOptions(LicenceScheduleDetail licenceScheduleDetail) {
    return licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail).stream()
        .sorted(Comparator.comparingInt(term -> term.getTermType().getDisplayOrder()))
        .collect(StreamUtil.toLinkedHashMap(
            term -> term.getId().toString(),
            term -> term.getTermType().getDisplayName())
        );
  }

  public Map<String, String> getSchedulePhaseOptions(LicenceScheduleDetail licenceScheduleDetail) {
    return licenceSchedulePhaseService.getPhasesByLicenceScheduleDetail(licenceScheduleDetail).stream()
        .sorted(Comparator.comparingInt(phase -> phase.getPhaseType().getDisplayOrder()))
        .collect(StreamUtil.toLinkedHashMap(
            phase -> phase.getId().toString(),
            phase -> phase.getPhaseType().getDisplayName())
        );
  }

  public Map<String, String> getRelativeEventOptions(LicenceScheduleDetail licenceScheduleDetail) {
    var termPhaseMap = licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail).stream()
        .sorted(Comparator.comparingInt(term -> term.getTermType().getDisplayOrder()))
        .collect(StreamUtil.toLinkedHashMap(Function.identity(), this::getPhaseMap));

    LinkedHashMap<String, String> combinedOptions = new LinkedHashMap<>();

    for (var termPhase : termPhaseMap.entrySet()) {
      var phases = termPhase.getValue();

      if (phases.isEmpty()) {
        combinedOptions.put(
            termPhase.getKey().getId().toString(),
            "Start of %s (%s)".formatted(
                termPhase.getKey().getTermType().getDisplayName(),
                DateFormatUtil.convertToDisplayText(termPhase.getKey().getStartDate())
            )
        );
      } else {
        combinedOptions.putAll(phases);
      }
    }

    return combinedOptions;
  }

  private Map<String, String> getPhaseMap(LicenceScheduleTerm term) {
    return licenceSchedulePhaseService.getPhasesByTerm(term).stream()
        .sorted(Comparator.comparingInt(phase -> phase.getPhaseType().getDisplayOrder()))
        .collect(StreamUtil.toLinkedHashMap(
            phase -> phase.getId().toString(),
            phase -> "Start of %s (%s)".formatted(
                phase.getPhaseType().getDisplayName(),
                DateFormatUtil.convertToDisplayText(phase.getStartDate()))
            )
        );
  }
}
