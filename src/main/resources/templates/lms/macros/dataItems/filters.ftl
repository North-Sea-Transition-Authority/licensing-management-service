<#include '../../layout/layout.ftl'>

<#macro referenceFilter form>
    <@fdsSearch.searchFilterItem itemName="Licence reference" expanded=form.reference?has_content>
        <@fdsSearch.searchTextInput path="form.licenceReference" labelText="Licence reference" labelClass="govuk-visually-hidden"/>
    </@fdsSearch.searchFilterItem>
</#macro>

<#macro licenceTypeFilter form licenceTypes>
    <@fdsSearch.searchFilterItem itemName="Licence type" expanded=form.licenceTypes?has_content>
        <@fdsSearch.searchCheckboxes path="form.licenceTypes" checkboxes=licenceTypes/>
    </@fdsSearch.searchFilterItem>
</#macro>

<#macro licenseeOrgUnitFilter form licenseeOrgUnitUrl preSelectedLicenseeOrgUnit>
    <@fdsSearch.searchFilterItem itemName="Licensee" expanded=form.licenseeOrgUnitId?has_content>
        <@fdsSearchSelector.searchSelectorRest
        path="form.licenseeOrgUnitId"
        restUrl=springUrl(licenseeOrgUnitUrl)
        preselectedItems=preSelectedLicenseeOrgUnit
        inputClass="govuk-!-width-three-quarters govuk-!-margin-bottom-0"
        selectorMinInputLength=3
        labelText=""
        />
    </@fdsSearch.searchFilterItem>
</#macro>
