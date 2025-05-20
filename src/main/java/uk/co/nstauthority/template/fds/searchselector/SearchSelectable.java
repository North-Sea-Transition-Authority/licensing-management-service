package uk.co.nstauthority.template.fds.searchselector;

/**
 * SearchSelectable provides the information required to produce a RestSearchItem.
 * A list of SearchSelectable implementations are used by the SearchSelectorService.
 */
public interface SearchSelectable {

  String getSelectionId();

  String getSelectionText();
}
