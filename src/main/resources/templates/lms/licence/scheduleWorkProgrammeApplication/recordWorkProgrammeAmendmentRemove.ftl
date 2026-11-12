<#include '../../layout/layout.ftl'>
<#import 'summarylistcard/recordWorkProgrammeAmendmentSummaryCard.ftl' as recordWorkProgrammeAmendmentSummaryCard>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
pageSize=PageSize.TWO_THIRDS_COLUMN
breadcrumbs=breadcrumbs>
    <@fdsForm.htmlForm actionUrl=springUrl(actionUrl)>

        <@recordWorkProgrammeAmendmentSummaryCard.recordWorkProgrammeAmendmentCard
            workProgrammeAmendment=workProgrammeAmendment
            summaryListId="work-programme-amendment"
            showActions=false/>

        <@fdsAction.submitButtons
            primaryButtonText="Remove"
            primaryButtonClass="govuk-button govuk-button--warning"
            secondaryLinkText="Cancel"
            linkSecondaryAction=true
            linkSecondaryActionUrl=springUrl(cancelUrl)/>

    </@fdsForm.htmlForm>
</@defaultPage>
