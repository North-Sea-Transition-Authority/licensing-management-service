<#include '../layout/layout.ftl'>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems>
    <@fdsForm.htmlForm>

        <@fdsRadio.radioGroup
            path="form.licenceType"
            labelText="Select the licence type"
            hiddenContent=true>
            <#assign firstItem=true/>
            <#list licenceTypeOptions as key, value>
                <@fdsRadio.radioItem path="form.licenceType" itemMap={key : value} isFirstItem=firstItem>
                    <#if key = "CARBON_STORAGE">
                        <@fdsSelect.select
                            path="form.responsibleTeam"
                            options=csResponsibleTeamOptions
                            labelText="Who is the licence allocated to?"
                            nestingPath="form.licenceType"
                        />
                    </#if>
                </@fdsRadio.radioItem>
                <#assign firstItem=false/>
            </#list>
        </@fdsRadio.radioGroup>

        <@fdsTextInput.textInput
            path="form.licenceNumber"
            labelText="What is the licence number?"
            hintText="For example, 100 or 100a. The prefix will be applied automatically based on the licence type."
            inputClass="govuk-!-width-one-third"
        />

        <@fdsFieldset.fieldset legendHeading="Add licensees" showHeadingOnly=true legendHeadingSize="h2">
            <@fdsAddToList.addToList
                pathForList="form.organisationUnitIds"
                pathForSelector="form.organisationUnitSelector"
                restUrl=springUrl(organisationUnitSearchEndpoint)
                alreadyAdded=preselectedOrgUnits
                itemName="Licensees"
            />
        </@fdsFieldset.fieldset>

        <@fdsAction.submitButtons
            primaryButtonText="Create licence"
            secondaryLinkText="Cancel"
            linkSecondaryAction=true
            linkSecondaryActionUrl=springUrl(backUrl)
        />
    </@fdsForm.htmlForm>
</@defaultPage>