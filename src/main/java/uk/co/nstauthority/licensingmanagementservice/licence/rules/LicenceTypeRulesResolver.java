package uk.co.nstauthority.licensingmanagementservice.licence.rules;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.EventType;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;

@Service
public interface LicenceTypeRulesResolver {
  boolean hasTerms(@NotNull LicenceType licenceType);

  Set<TermType> getTerms(@NotNull LicenceType licenceType);

  boolean hasPhases(@NotNull LicenceType licenceType);

  Set<PhaseType> getPhases(@NotNull LicenceType licenceType, @NotNull TermType termType);

  boolean arePhasesCaptured(@NotNull LicenceType licenceType);

  boolean isRentalRatesFlat(LicenceType licenceType);

  boolean isRentalRatesExponential(LicenceType licenceType);

  boolean hasWorkProgramme(@NotNull LicenceType licenceType);

  boolean hasRentalRate(@NotNull LicenceType licenceType);

  boolean isWorkProgrammesTermTied(LicenceType licenceType, TermType termType);

  boolean isWorkProgrammesPhaseTied(LicenceType licenceType);

  Set<EventType> getSupportedEvents(@NotNull LicenceType licenceType);

  List<LicenceType> getLicenceTypesThatCanCreateScheduleWorkProgrammeApplications();
}
