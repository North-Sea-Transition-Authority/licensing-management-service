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
        <@fdsAction.submitButtons primaryButtonText="Continue" secondaryLinkText="Back" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(backUrl)/>
    </@fdsForm.htmlForm>

</@defaultPage>