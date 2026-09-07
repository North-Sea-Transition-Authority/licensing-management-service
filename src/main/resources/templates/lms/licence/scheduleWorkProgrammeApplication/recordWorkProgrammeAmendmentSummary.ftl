<#include '../../layout/layout.ftl'>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems
breadcrumbs=breadcrumbs>
    <@fdsForm.htmlForm>

        <#list workProgrammeAmendments as workProgrammeAmendment>
            <@fdsSummaryList.summaryListCard
                headingText=workProgrammeAmendment.workProgrammeDescription()
                headingSize="h2"
                summaryListId="work-programme-amendment-${workProgrammeAmendment?index}">

                <@fdsSummaryList.summaryListRowNoAction keyText="Due date">
                    ${workProgrammeAmendment.dueDate()}
                </@fdsSummaryList.summaryListRowNoAction>

                <@fdsSummaryList.summaryListRowNoAction keyText="Decision">
                    ${workProgrammeAmendment.decision()}
                </@fdsSummaryList.summaryListRowNoAction>

                <#if workProgrammeAmendment.amendedDuration()?has_content>
                    <@fdsSummaryList.summaryListRowNoAction keyText="Amended duration">
                        ${workProgrammeAmendment.amendedDuration()}
                    </@fdsSummaryList.summaryListRowNoAction>
                </#if>

                <#if workProgrammeAmendment.amendedText()?has_content>
                    <@fdsSummaryList.summaryListRowNoAction keyText="Amended text">
                        ${workProgrammeAmendment.amendedText()}
                    </@fdsSummaryList.summaryListRowNoAction>
                </#if>

            </@fdsSummaryList.summaryListCard>
        </#list>

        <@fdsRadio.radioGroup
            path="form.recordWorkProgrammeAmendmentSummaryOptions"
            labelText="Do you want to add another work programme activity to the scope of this decision?"
            fieldsetHeadingSize="h2"
            fieldsetHeadingClass="govuk-fieldset__legend--m">
            <#assign isFirstOption=true/>
            <#list summaryOptions as optionKey, optionValue>
                <@fdsRadio.radioItem
                    path="form.recordWorkProgrammeAmendmentSummaryOptions"
                    itemMap={optionKey : optionValue}
                    isFirstItem=isFirstOption/>
                <#assign isFirstOption=false/>
            </#list>
        </@fdsRadio.radioGroup>

        <@fdsAction.submitButtons
            primaryButtonText="Save and continue"
            secondaryLinkText="Cancel"
            linkSecondaryAction=true
            linkSecondaryActionUrl=springUrl(cancelUrl)/>

    </@fdsForm.htmlForm>
</@defaultPage>
