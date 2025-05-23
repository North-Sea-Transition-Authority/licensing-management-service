<#include '../../layout/layout.ftl'>

<#macro referenceFilter form>
    <@fdsSearch.searchFilterItem itemName="Reference" expanded=form.reference?has_content>
        <@fdsSearch.searchTextInput path="form.reference" labelText="Reference" labelClass="govuk-visually-hidden"/>
    </@fdsSearch.searchFilterItem>
</#macro>
