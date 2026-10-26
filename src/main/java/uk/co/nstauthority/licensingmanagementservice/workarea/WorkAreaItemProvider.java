package uk.co.nstauthority.licensingmanagementservice.workarea;

import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.PhaseGated;

/**
 * Contributes a category of items to the work area. Extends {@link PhaseGated} so each provider declares the release
 * feature (hence phase) its category belongs to — {@code WorkAreaService} filters the providers accordingly.
 *
 * <p>Each provider loads its rows first and returns them as a {@link WorkAreaSection}. Once every section is loaded,
 * the data they share is looked up once in a {@link WorkAreaContext}, and each section builds its items from it.</p>
 */
public interface WorkAreaItemProvider extends PhaseGated {

  WorkAreaSection loadSection(
      WorkAreaFilterForm workAreaFilterForm,
      ServiceUserDetail serviceUserDetail,
      WorkAreaScope workAreaScope
  );
}
