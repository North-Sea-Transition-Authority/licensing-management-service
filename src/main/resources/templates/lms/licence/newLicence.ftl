<#include '../layout/layout.ftl'>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems>
    <@fdsForm.htmlForm>
        <@fdsSelect.select
            path="form.licenceType"
            options=licenceTypeOptions
            labelText="Select the licence type"
        />

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
                alreadyAdded=preselectedItems
                itemName="Licensees"
            />
        </@fdsFieldset.fieldset>

        <@fdsAction.submitButtons
            primaryButtonText="Save and continue"
            secondaryLinkText="Cancel"
            linkSecondaryAction=true
            linkSecondaryActionUrl=springUrl(backUrl)
        />
    </@fdsForm.htmlForm>
</@defaultPage>