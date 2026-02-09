<#include '../../layout/layout.ftl'>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
caption=pageCaption
captionClass="govuk-caption-l"
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems>
    <@fdsForm.htmlForm>

        <@fdsSelect.select
        path="form.responsibleOrganisationUnitId"
        options=responsibleOrgUnitOptions
        labelText="Who is the licensee for this application?"
        />

        <@fdsRadio.radioGroup
        path="form.allLicenseesPermissionConfirmed"
        labelText="Have you confirmed this request is made on behalf of all licensees?"
        showLabelOnly=true>
            <@fdsRadio.radioYes path="form.allLicenseesPermissionConfirmed"/>
            <@fdsRadio.radioNo path="form.allLicenseesPermissionConfirmed"/>
        </@fdsRadio.radioGroup>

        <br>

        <@fdsAction.submitButtons primaryButtonText="Continue" secondaryLinkText="Back" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(backUrl)/>

    </@fdsForm.htmlForm>
</@defaultPage>