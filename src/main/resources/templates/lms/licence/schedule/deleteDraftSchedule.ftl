<#include '../../layout/layout.ftl'>
<#import 'timeline/scheduleComponents.ftl'as scheduleComponents>

<#assign pageTitle = "Do you want to delete the draft licence schedule?"/>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
caption=pageCaption
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems>
    <@fdsForm.htmlForm>
        <@scheduleComponents.timelineSummaryCard
            timelineSummaryCardView=summaryCardView
        />

        <@fdsAction.submitButtons
            primaryButtonText="Delete"
            secondaryLinkText="Cancel"
            linkSecondaryAction=true
            linkSecondaryActionUrl=springUrl(cancelUrl)
            primaryButtonClass="govuk-button govuk-button--warning"
        />
    </@fdsForm.htmlForm>
</@defaultPage>