package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate;

import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.rules.LicenceTypeRulesResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@Service
public class LicenceScheduleRateFormService {

  private final LicenceScheduleRateRepository licenceScheduleRateRepository;
  private final LicenceScheduleTermService licenceScheduleTermService;
  private final LicenceSchedulePhaseService licenceSchedulePhaseService;
  private final LicenceTypeRulesResolver licenceTypeRulesResolver;

  public LicenceScheduleRateFormService(
      LicenceScheduleRateRepository licenceScheduleRateRepository,
      LicenceScheduleTermService licenceScheduleTermService,
      LicenceSchedulePhaseService licenceSchedulePhaseService,
      LicenceTypeRulesResolver licenceTypeRulesResolver
  ) {
    this.licenceScheduleRateRepository = licenceScheduleRateRepository;
    this.licenceScheduleTermService = licenceScheduleTermService;
    this.licenceSchedulePhaseService = licenceSchedulePhaseService;
    this.licenceTypeRulesResolver = licenceTypeRulesResolver;
  }

  @Transactional
  void saveRateFromForm(LicenceScheduleRateForm form, LicenceScheduleDetail licenceScheduleDetail) {
    var licenceScheduleRate = new LicenceScheduleRate();
    licenceScheduleRate.setLicenceScheduleDetail(licenceScheduleDetail);
    licenceScheduleRate.setRateDefinitionOption(form.getRateDefinitionOption());

    if (form.getRateDefinitionOption().equals(RateDefinitionOption.TERM)) {
      licenceScheduleRate.setLicenceScheduleTerm(
          licenceScheduleTermService.getTermByIdOrThrow(UUID.fromString(form.getLicenceScheduleTermId()))
      );
    } else {
      licenceScheduleRate.setLicenceScheduleTerm(null);
    }

    if (form.getRateDefinitionOption().equals(RateDefinitionOption.PHASE)) {
      licenceScheduleRate.setLicenceSchedulePhase(
          licenceSchedulePhaseService.getPhaseByIdOrThrow(UUID.fromString(form.getLicenceSchedulePhaseId()))
      );
    } else {
      licenceScheduleRate.setLicenceSchedulePhase(null);
    }

    if (form.getRateDefinitionOption().equals(RateDefinitionOption.CUSTOM_PERIOD)) {
      licenceScheduleRate.setStartDate(form.getStartDate().getAsLocalDate().orElse(null));
    } else {
      licenceScheduleRate.setStartDate(null);
    }

    licenceScheduleRate.setRentalRate(form.getRentalRate().getAsBigDecimal().orElse(null));
    licenceScheduleRate.setComments(form.getComments());

    licenceScheduleRateRepository.save(licenceScheduleRate);
  }

  public Map<String, String> getScheduleTermOptions(LicenceScheduleDetail licenceScheduleDetail) {
    return licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail).stream()
        .sorted(Comparator.comparingInt(term -> term.getTermType().getDisplayOrder()))
        .collect(StreamUtil.toLinkedHashMap(
            term -> term.getId().toString(),
            term -> term.getTermType().getDisplayName())
        );
  }

  public Map<String, String> getSchedulePhaseOptions(LicenceScheduleDetail licenceScheduleDetail) {
    return licenceSchedulePhaseService.getActivePhasesByLicenceScheduleDetail(licenceScheduleDetail).stream()
        .sorted(Comparator.comparingInt(phase -> phase.getPhaseType().getDisplayOrder()))
        .collect(StreamUtil.toLinkedHashMap(
            phase -> phase.getId().toString(),
            phase -> phase.getPhaseType().getDisplayName())
        );
  }

  public Map<String, String> getRateDefinitionOptions(LicenceScheduleDetail licenceScheduleDetail) {
    var options = new ArrayList<>(Arrays.asList(RateDefinitionOption.values()));

    var licenceType = licenceScheduleDetail.getLicenceSchedule().getLicence().getType();

    if (!licenceTypeRulesResolver.arePhasesCaptured(licenceType) || getSchedulePhaseOptions(licenceScheduleDetail).isEmpty()) {
      options.remove(RateDefinitionOption.PHASE);
    }

    return DisplayableEnumOptionUtil.getDisplayableOptions(options);
  }

}
