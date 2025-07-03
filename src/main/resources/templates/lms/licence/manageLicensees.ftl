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

        <@fdsAction.submitButtons
            primaryButtonText="Save and continue"
            secondaryLinkText="Cancel"
            linkSecondaryAction=true
            linkSecondaryActionUrl=springUrl(backUrl)
        />
    </@fdsForm.htmlForm>
</@defaultPage>