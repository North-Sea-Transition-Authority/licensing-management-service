<#include '../layout/layout.ftl'>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
caption=pageCaption
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems>
    <@fdsForm.htmlForm>
        <@fdsRadio.radio
            path="form.licenceStatus"
            labelText="What is the status of the licence?"
            radioItems=licenceStatusOptions
        />

        <@fdsDateInput.dateInput
            dayPath="form.licenceStatusDate.dayInput.inputValue"
            monthPath="form.licenceStatusDate.monthInput.inputValue"
            yearPath="form.licenceStatusDate.yearInput.inputValue"
            labelText="What date did the licence enter this status?"
            formId="form-licence-status-date"
        />

        <@fdsAddToList.addToList
            pathForList="form.organisationUnitIds"
            pathForSelector="form.organisationUnitSelector"
            restUrl=springUrl(organisationUnitSearchEndpoint)
            alreadyAdded=preselectedOrgUnits
            itemName="Licensees"
        />

        <@fdsDetails.summaryDetails
          summaryTitle="The licensee I want to select is not shown in the list"
        >
          <p class="govuk-body">
            If the licensee you want to select is not shown in the list then you can <@requestNewCompany.requestCompanyLink/>
          </p>
        </@fdsDetails.summaryDetails>

        <@fdsAction.submitButtons
            primaryButtonText="Save and continue"
            secondaryLinkText="Cancel"
            linkSecondaryAction=true
            linkSecondaryActionUrl=springUrl(backUrl)
        />
    </@fdsForm.htmlForm>
</@defaultPage>