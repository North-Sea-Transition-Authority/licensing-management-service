<#-- Mockup copy of lms/licence/schedule/updateWorkProgrammeActivityStatus.ftl. The TRANSFERRED status is replaced by
     ALTERNATIVE_WORK_PROGRAMME, which reveals an add to list of the licences delivering the alternative work programme
     and a comment explaining the reasons for it. -->
<#include '../../layout/layout.ftl'>

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
                    <#if key = "ALTERNATIVE_WORK_PROGRAMME">
                        <@fdsAddToList.addToList
                            pathForList="form.alternativeWorkProgrammeLicenceIds"
                            pathForSelector="form.alternativeWorkProgrammeLicenceSelector"
                            alreadyAdded=alternativeWorkProgrammeLicences
                            addToListId="alternative-work-programme-licences"
                            title="Licences delivering the alternative work programme activity"
                            itemName="Licence"
                            noItemText="No licences have been added yet."
                            selectorLabelText="Add a licence"
                            selectorHintText="Search for a licence by reference, for example P2411"
                            selectorNestingPath="form.status"
                            restUrl=springUrl(licenceSearchUrl)
                        />

                        <@fdsTextarea.textarea
                            path="form.alternativeWorkProgrammeComment"
                            nestingPath="form.status"
                            labelText="Why is the work programme being delivered against alternative licence(s)?"
                            rows="5"
                        />
                    </#if>
                </@fdsRadio.radioItem>
                <#assign firstItem=false/>
            </#list>
        </@fdsRadio.radioGroup>

        <@fdsAction.submitButtons primaryButtonText="Apply" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(cancelUrl)/>
    </@fdsForm.htmlForm>
</@defaultPage>
