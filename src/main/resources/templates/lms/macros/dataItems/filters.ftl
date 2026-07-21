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

<#macro applicationReferenceFilter form>
    <@fdsSearch.searchFilterItem itemName="Application reference" expanded=form.applicationReference?has_content>
        <@fdsSearch.searchTextInput path="form.applicationReference" labelText="Application reference" labelClass="govuk-visually-hidden"/>
    </@fdsSearch.searchFilterItem>
</#macro>

<#macro applicationTypeFilter form applicationTypes>
    <@fdsSearch.searchFilterItem itemName="Application type" expanded=form.applicationTypes?has_content>
        <@fdsSearch.searchCheckboxes path="form.applicationTypes" checkboxes=applicationTypes/>
    </@fdsSearch.searchFilterItem>
</#macro>

<#macro applicationStatusFilter form applicationStatuses>
    <@fdsSearch.searchFilterItem itemName="Application status" expanded=form.applicationStatuses?has_content>
        <@fdsSearch.searchCheckboxes path="form.applicationStatuses" checkboxes=applicationStatuses/>
    </@fdsSearch.searchFilterItem>
</#macro>

<#macro licenseeGroupFilter form licenseeGroupOrgUnitUrl preSelectedLicenseeGroup={}>
    <@fdsSearch.searchFilterItem itemName="Licensee group" expanded=form.licenseeOrgGroupId?has_content>
        <@fdsSearchSelector.searchSelectorRest
        path="form.licenseeOrgGroupId"
        restUrl=springUrl(licenseeGroupOrgUnitUrl)
        preselectedItems=preSelectedLicenseeGroup
        inputClass="govuk-!-width-three-quarters govuk-!-margin-bottom-0"
        selectorMinInputLength=2
        labelText=""
        />
    </@fdsSearch.searchFilterItem>
</#macro>

<#macro licenseeOrgUnitFilter form licenseeOrgUnitUrl preSelectedLicenseeOrgUnit>
    <@fdsSearch.searchFilterItem itemName="Licensee" expanded=form.licenseeOrgUnitId?has_content>
        <@fdsSearchSelector.searchSelectorRest
        path="form.licenseeOrgUnitId"
        restUrl=springUrl(licenseeOrgUnitUrl)
        preselectedItems=preSelectedLicenseeOrgUnit
        inputClass="govuk-!-width-three-quarters govuk-!-margin-bottom-0"
        selectorMinInputLength=2
        labelText=""
        />
    </@fdsSearch.searchFilterItem>
</#macro>

<#macro contactEmailFilter form>
    <@fdsSearch.searchFilterItem itemName="Contact email" expanded=form.contactEmail?has_content>
        <@fdsSearch.searchTextInput path="form.contactEmail" labelText="Contact email" labelClass="govuk-visually-hidden"/>
    </@fdsSearch.searchFilterItem>
</#macro>

<#macro noContactAssignedFilter form>
    <@fdsSearch.searchFilterItem itemName="Contact" expanded=(form.noContactAssigned!false) == true>
        <@fdsCheckbox.checkbox path="form.noContactAssigned" labelText="No contact provided" smallCheckboxes=true/>
    </@fdsSearch.searchFilterItem>
</#macro>