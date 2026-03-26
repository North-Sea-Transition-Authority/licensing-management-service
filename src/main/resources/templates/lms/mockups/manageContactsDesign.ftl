<#include '../layout/layout.ftl'>

<@defaultPage
htmlTitle="Manage licence contact details"
pageHeading="Manage licence contact details"
pageSize=PageSize.FULL_COLUMN
extendContainerWidth=true>

  <@fdsSearch.searchPage>

    <@fdsSearch.searchFilter>
      <@fdsSearch.searchFilterList clearFilterText="Clear filters" clearFilterUrl="#">
        <@fdsSearch.searchFilterItem itemName="Licence reference" expanded=true>
          <div class="govuk-form-group">
              <label for="filter-licence" class="govuk-visually-hidden">Licence reference</label>
            <input class="govuk-input" id="filter-licence" name="filter-licence" type="text">
          </div>
        </@fdsSearch.searchFilterItem>
        <@fdsSearch.searchFilterItem itemName="Licensee" expanded=true>
          <div class="govuk-form-group">
              <label for="filter-licensee" class="govuk-visually-hidden">Licensee</label>
              <input class="govuk-input" id="filter-licensee" name="filter-licensee" type="text">
          </div>
        </@fdsSearch.searchFilterItem>
      </@fdsSearch.searchFilterList>
    </@fdsSearch.searchFilter>

    <@fdsSearch.searchPageContent>

      <p class="govuk-body"><strong>${contactCount}</strong> results</p>

      <@fdsTable.sortableTable tableContents=contactsTableJson tableId="contacts-table" tableCaption="Licence contact details"/>

    </@fdsSearch.searchPageContent>
  </@fdsSearch.searchPage>

</@defaultPage>