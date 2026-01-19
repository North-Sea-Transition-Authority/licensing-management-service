<#include '../layout/layout.ftl'>
<#macro licenceTypeFilter form licenceTypes>
  <@fdsSearch.searchFilterItem itemName="Licence type" expanded=form.licenceTypes?has_content>
    <@fdsSearch.searchCheckboxes path="form.licenceTypes" checkboxes=licenceTypes/>
  </@fdsSearch.searchFilterItem>
</#macro>

<#macro documentTitleFilter form>
  <@fdsSearch.searchFilterItem itemName="Document title" expanded=form.documentTemplateTitle?has_content>
    <@fdsSearch.searchTextInput path="form.documentTemplateTitle" labelText="Document title" labelClass="govuk-visually-hidden"/>
  </@fdsSearch.searchFilterItem>
</#macro>