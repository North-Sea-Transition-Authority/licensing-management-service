<#include '../../layout/layout.ftl'>
<#import '../../component/duration/threeFieldDuration.ftl' as duration>

<#assign pageTitle = "Update the work programme activity status" />

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
caption=pageCaption
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems
>
    <@fdsSummaryList.summaryListCard summaryListId="summary-card-list">
        <@fdsSummaryList.summaryListRowNoAction keyText="Category">
            ${summaryView.category()}
        </@fdsSummaryList.summaryListRowNoAction>
        <@fdsSummaryList.summaryListRowNoAction keyText="Description">
            ${summaryView.description()}
        </@fdsSummaryList.summaryListRowNoAction>
        <@fdsSummaryList.summaryListRowNoAction keyText="Commitment">
            ${summaryView.commitment()}
        </@fdsSummaryList.summaryListRowNoAction>
        <#if summaryView.dueDate()?has_content>
            <@fdsSummaryList.summaryListRowNoAction keyText="Due date">
                ${summaryView.dueDate()}
            </@fdsSummaryList.summaryListRowNoAction>
        </#if>
    </@fdsSummaryList.summaryListCard>

    <@fdsForm.htmlForm>
        <@fdsRadio.radioGroup
            path="form.status"
            labelText="Status"
            hiddenContent=true
        >
            <#assign firstItem=true/>
            <#list statusRadioOptions as key, value>
                <@fdsRadio.radioItem path="form.status" itemMap={key : value} isFirstItem=firstItem>
                    <#if key = "TRANSFERRED">
                        <@fdsSearchSelector.searchSelectorRest
                            path="form.transferredToLicenceId"
                            restUrl=springUrl(licenceSearchUrl)
                            labelText="Licence transferred to"
                            nestingPath="form.status"
                        />
                    </#if>
                </@fdsRadio.radioItem>
                <#assign firstItem=false/>
            </#list>
        </@fdsRadio.radioGroup>

        <@fdsAction.submitButtons primaryButtonText="Apply" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(cancelUrl)/>
    </@fdsForm.htmlForm>
</@defaultPage>