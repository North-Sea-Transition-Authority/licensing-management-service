<#include '../../layout/layout.ftl'>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
pageSize=PageSize.TWO_THIRDS_COLUMN
breadcrumbs=breadcrumbs>
    <@fdsForm.htmlForm>

        <#list durationChangeSummaryViews as view>
            <@fdsSummaryList.summaryListCard
                headingText=view.displayName()
                headingSize="h2"
                summaryListId="duration-change-${view?index}">

                <@fdsSummaryList.summaryListRowNoAction keyText="Current duration">
                    ${view.currentDuration()}
                </@fdsSummaryList.summaryListRowNoAction>

                <@fdsSummaryList.summaryListRowNoAction keyText="Current end date">
                    ${view.currentEndDate()}
                </@fdsSummaryList.summaryListRowNoAction>

                <@fdsSummaryList.summaryListRowNoAction keyText="Change">
                    ${view.change()}
                </@fdsSummaryList.summaryListRowNoAction>

                <@fdsSummaryList.summaryListRowNoAction keyText="New duration">
                    ${view.newDuration()}
                </@fdsSummaryList.summaryListRowNoAction>

                <@fdsSummaryList.summaryListRowNoAction keyText="New end date">
                    ${view.newEndDate()}
                </@fdsSummaryList.summaryListRowNoAction>

            </@fdsSummaryList.summaryListCard>
        </#list>

        <@fdsAction.submitButtons
            primaryButtonText="Continue"
            secondaryLinkText="Back"
            linkSecondaryAction=true
            linkSecondaryActionUrl=springUrl(backUrl)/>

    </@fdsForm.htmlForm>
</@defaultPage>
