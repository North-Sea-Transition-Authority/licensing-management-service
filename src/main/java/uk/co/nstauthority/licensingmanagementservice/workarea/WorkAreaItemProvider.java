package uk.co.nstauthority.licensingmanagementservice.workarea;

import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.PhaseGated;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;

/**
 * Contributes a category of items to the work area. Extends {@link PhaseGated} so each provider declares the release
 * feature (hence phase) its category belongs to — {@code WorkAreaService} filters the providers accordingly.
 */
public interface WorkAreaItemProvider extends PhaseGated {

  List<SearchResultItem> getWorkAreaItems(WorkAreaFilterForm workAreaFilterForm, ServiceUserDetail serviceUserDetail);
}
