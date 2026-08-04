<#include '../layout/layout.ftl'>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
caption=pageCaption
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems>
    <@fdsForm.htmlForm>
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