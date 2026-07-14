<#include '../../layout/layout.ftl'>
<#import 'timeline/scheduleComponents.ftl'as scheduleComponents>

<#assign pageTitle = "Do you want to apply changes to the licence schedule?"/>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
caption=pageCaption
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems>
    <@fdsForm.htmlForm>
        <#if initialTermPhaseValidationError>
            <@fdsCard.card cardClass="fds-card--error">
                <@fdsCard.cardHeader cardHeadingText="This schedule cannot be applied"/>
                <p class="govuk-body">The schedule cannot be applied as the Initial Term end date does not match the date of the final phase in the term</p>
            </@fdsCard.card>
        </#if>

        <@scheduleComponents.timelineSummaryCard
            timelineSummaryCardView=summaryCardView
        />

        <#if initialTermPhaseValidationError>
            <@fdsBackLink.backLink backLinkUrl=springUrl(cancelUrl)/>
        <#else>
            <@fdsAction.submitButtons
            primaryButtonText="Apply"
            secondaryLinkText="Cancel"
            linkSecondaryAction=true
            linkSecondaryActionUrl=springUrl(cancelUrl)
            />
        </#if>

    </@fdsForm.htmlForm>
</@defaultPage>