package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney.LicenceScheduleExtensionService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney.LicenceScheduleTermAndPhases;

@Service
public class RecordExtensionDetailsService {

  private final RecordOfDecisionExtensionRepository recordOfDecisionExtensionRepository;
  private final LicenceScheduleExtensionService licenceScheduleExtensionService;
  private final ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;
  private final LicenceSchedulePhaseRepository licenceSchedulePhaseRepository;
  private final LicenceScheduleTermRepository licenceScheduleTermRepository;

  public RecordExtensionDetailsService(
      RecordOfDecisionExtensionRepository recordOfDecisionExtensionRepository,
      LicenceScheduleExtensionService licenceScheduleExtensionService,
      ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService,
      LicenceSchedulePhaseRepository licenceSchedulePhaseRepository,
      LicenceScheduleTermRepository licenceScheduleTermRepository
  ) {
    this.recordOfDecisionExtensionRepository = recordOfDecisionExtensionRepository;
    this.licenceScheduleExtensionService = licenceScheduleExtensionService;
    this.scheduleWorkProgrammeApplicationService = scheduleWorkProgrammeApplicationService;
    this.licenceSchedulePhaseRepository = licenceSchedulePhaseRepository;
    this.licenceScheduleTermRepository = licenceScheduleTermRepository;
  }

  public boolean hasExtensionDetails(ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    return recordOfDecisionExtensionRepository.existsByScheduleWorkProgrammeApplicationDetail(applicationDetail);
  }

  public ThreeFieldDuration getTotalExtensionDuration(ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    return ThreeFieldDuration.total(
        recordOfDecisionExtensionRepository.findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail)
            .stream()
            .map(RecordOfDecisionExtension::getExtensionDuration)
            .toList());
  }

  private List<LicenceScheduleTermAndPhases> getExtendableTermAndPhases(
      ScheduleWorkProgrammeApplicationDetail applicationDetail
  ) {
    var licenceScheduleDetail = scheduleWorkProgrammeApplicationService
        .getScheduleDetailFromApplicationDetail(applicationDetail);
    return licenceScheduleExtensionService.getExtendableTermAndPhases(licenceScheduleDetail);
  }

  public List<RecordExtensionDetailsView> getExtensionDetailsViews(
      ScheduleWorkProgrammeApplicationDetail applicationDetail
  ) {
    var extensionMap = recordOfDecisionExtensionRepository
        .findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail)
        .stream()
        .collect(Collectors.toMap(this::getExtensionIdString, extension -> extension));

    var extendableTermAndPhases = getExtendableTermAndPhases(applicationDetail);

    List<RecordExtensionDetailsView> views = new ArrayList<>();

    extendableTermAndPhases.forEach(termAndPhases -> {
      if (termAndPhases.termId() != null) {
        var endDate = licenceScheduleTermRepository.findById(UUID.fromString(termAndPhases.termId()))
            .map(term -> DateFormatUtil.convertToDisplayText(term.getEndDate()))
            .orElse("");
        views.add(createViewItem(termAndPhases.termId(), termAndPhases.termName(), endDate, false, extensionMap));
      }
      if (termAndPhases.phases() != null) {
        for (var phase : termAndPhases.phases()) {
          var endDate = licenceSchedulePhaseRepository.findById(UUID.fromString(phase.phaseId()))
              .map(licenceSchedulePhase -> DateFormatUtil.convertToDisplayText(licenceSchedulePhase.getEndDate()))
              .orElse("");
          views.add(createViewItem(phase.phaseId(), phase.phaseName(), endDate, true, extensionMap));
        }
      }
    });

    return views;
  }

  public RecordExtensionDetailsForm getFilledForm(ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    var form = new RecordExtensionDetailsForm();

    getExtensionDetailsViews(applicationDetail).forEach(view -> {
      if (view.isPhase()) {
        form.getSelectedPhase().put(view.id(), view.isRequested());
      } else {
        form.getSelectedTerm().put(view.id(), view.isRequested());
      }
      populateExtensionDurationMap(form, view.id(), view.duration());
    });

    return form;
  }

  @Transactional
  public void saveExtensionDetails(
      RecordExtensionDetailsForm form,
      ScheduleWorkProgrammeApplicationDetail applicationDetail
  ) {
    Set<String> selectedIds = new HashSet<>();

    saveSelectedExtensions(form.getSelectedPhase(), true, form, applicationDetail, selectedIds);
    saveSelectedExtensions(form.getSelectedTerm(), false, form, applicationDetail, selectedIds);

    if (selectedIds.isEmpty() && form.getExtensionDuration() != null && form.getExtensionDuration().size() == 1) {
      saveSingleExtension(form, applicationDetail, selectedIds);
    }

    if (selectedIds.isEmpty()) {
      return;
    }

    deleteUnselectedExtensions(applicationDetail, selectedIds);
  }

  private void saveSelectedExtensions(
      Map<String, Boolean> selectedMap,
      boolean isPhase,
      RecordExtensionDetailsForm form,
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      Set<String> selectedIds
  ) {
    if (selectedMap == null || selectedMap.isEmpty()) {
      return;
    }

    for (Map.Entry<String, Boolean> entry : selectedMap.entrySet()) {
      if (BooleanUtils.isTrue(entry.getValue())) {
        var idStr = entry.getKey();
        selectedIds.add(idStr);
        var id = UUID.fromString(idStr);
        var duration = form.getExtensionDuration().get(idStr).toThreeFieldDuration();
        saveExtension(applicationDetail, id, duration, isPhase);
      }
    }
  }

  private void saveSingleExtension(
      RecordExtensionDetailsForm form,
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      Set<String> selectedIds
  ) {
    var idStr = form.getExtensionDuration().keySet().iterator().next();
    var id = UUID.fromString(idStr);
    var duration = form.getExtensionDuration().get(idStr).toThreeFieldDuration();

    var hasPhase = licenceSchedulePhaseRepository.existsById(id);
    var hasTerm = licenceScheduleTermRepository.existsById(id);

    if (hasPhase || hasTerm) {
      selectedIds.add(idStr);
      saveExtension(applicationDetail, id, duration, hasPhase);
    }
  }

  private void saveExtension(
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      UUID id,
      ThreeFieldDuration duration,
      boolean isPhase
  ) {
    Optional<RecordOfDecisionExtension> existingExtension;

    if (isPhase) {
      existingExtension = recordOfDecisionExtensionRepository
          .findByScheduleWorkProgrammeApplicationDetailAndLicenceSchedulePhaseId(applicationDetail, id);
    } else {
      existingExtension = recordOfDecisionExtensionRepository
          .findByScheduleWorkProgrammeApplicationDetailAndLicenceScheduleTermId(applicationDetail, id);
    }

    var extension = existingExtension.orElseGet(RecordOfDecisionExtension::new);

    extension.setExtensionDuration(duration);
    extension.setScheduleWorkProgrammeApplicationDetail(applicationDetail);

    if (isPhase) {
      var phase = licenceSchedulePhaseRepository.findById(id)
          .orElseThrow(() -> new IllegalArgumentException("LicenceSchedulePhase not found for ID: " + id));
      extension.setLicenceSchedulePhase(phase);
      extension.setLicenceScheduleTerm(null);
    } else {
      var term = licenceScheduleTermRepository.findById(id)
          .orElseThrow(() -> new IllegalArgumentException("LicenceScheduleTerm not found for ID: " + id));
      extension.setLicenceScheduleTerm(term);
      extension.setLicenceSchedulePhase(null);
    }

    recordOfDecisionExtensionRepository.save(extension);
  }

  private void deleteUnselectedExtensions(
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      Set<String> selectedIds
  ) {
    recordOfDecisionExtensionRepository.findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail)
        .stream()
        .filter(extension -> !selectedIds.contains(getExtensionIdString(extension)))
        .forEach(recordOfDecisionExtensionRepository::delete);
  }

  private RecordExtensionDetailsView createViewItem(
      String id,
      String displayName,
      String endDate,
      boolean isPhase,
      Map<String, RecordOfDecisionExtension> extensionMap
  ) {
    var extension = extensionMap.get(id);
    return new RecordExtensionDetailsView(
        id,
        displayName,
        endDate,
        isPhase,
        extension != null,
        extension != null ? extension.getExtensionDuration() : null
    );
  }

  private String getExtensionIdString(RecordOfDecisionExtension extension) {
    return extension.getLicenceSchedulePhase() != null
        ? extension.getLicenceSchedulePhase().getId().toString()
        : extension.getLicenceScheduleTerm().getId().toString();
  }

  private void populateExtensionDurationMap(
      RecordExtensionDetailsForm form,
      String id,
      ThreeFieldDuration durationValue
  ) {
    var durationInput = RecordExtensionDetailsForm.newDurationInput(id);

    if (durationValue != null) {
      durationInput.setFromThreeFieldDuration(durationValue);
    }

    form.getExtensionDuration().put(id, durationInput);
  }
}
