package uk.co.nstauthority.licensingmanagementservice.workarea;

import java.util.List;
import java.util.function.Function;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;

/**
 * A category of work area items whose rows have been loaded, but not yet turned into items. The work area loads every
 * section first, so the data the sections share (licensees, organisation groups, viewed items) can be looked up once
 * for all of them before the items are built.
 *
 * @param itemType the type the section's items are logged as viewed under
 * @param licences the licences whose licensees the section shows or filters by
 * @param organisationGroupLookup whether the section needs the organisation group of each licensee
 * @param itemBuilder builds the section's items from the shared context
 */
public record WorkAreaSection(
    WorkAreaDataItemType itemType,
    List<Licence> licences,
    OrganisationGroupLookup organisationGroupLookup,
    Function<WorkAreaContext, List<SearchResultItem>> itemBuilder
) {

  public static WorkAreaSection empty(WorkAreaDataItemType itemType) {
    return new WorkAreaSection(itemType, List.of(), OrganisationGroupLookup.NOT_REQUIRED, context -> List.of());
  }

  public List<SearchResultItem> buildItems(WorkAreaContext context) {
    return itemBuilder.apply(context);
  }
}
