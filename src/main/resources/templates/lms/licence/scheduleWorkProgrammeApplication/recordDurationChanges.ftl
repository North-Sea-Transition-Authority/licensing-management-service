<#include '../../layout/layout.ftl'>
<#import '../../component/duration/threeFieldDuration.ftl' as duration>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems
breadcrumbs=breadcrumbs>
    <@fdsForm.htmlForm>

        <#list durationChangeViews as view>
            <h2 class="govuk-heading-m">${view.displayName()}</h2>

            <@fdsRadio.radioGroup
                path="form.changeType[${view.id()}]"
                labelText="Duration"
                fieldsetHeadingSize="h3"
                fieldsetHeadingClass="govuk-fieldset__legend--s"
                hiddenContent=true>

                <@fdsRadio.radioItem
                    path="form.changeType[${view.id()}]"
                    itemMap={"MAINTAIN": changeTypeOptions["MAINTAIN"]}
                    isFirstItem=true/>

                <@fdsRadio.radioItem
                    path="form.changeType[${view.id()}]"
                    itemMap={"REDUCE": changeTypeOptions["REDUCE"]}>
                    <@duration.threeFieldDuration
                        dayPath="form.reduceDuration[${view.id()}].days"
                        monthPath="form.reduceDuration[${view.id()}].months"
                        yearPath="form.reduceDuration[${view.id()}].years"
                        fieldNamePath="form.reduceDuration[${view.id()}].fieldName"
                        fieldDisplayTextPath="form.reduceDuration[${view.id()}].fieldDisplayText"
                        nestingPath="form.changeType[${view.id()}]"
                        labelText="How long is this to be reduced by?"
                        formId="reduce-${view.id()}"/>
                </@fdsRadio.radioItem>

                <@fdsRadio.radioItem
                    path="form.changeType[${view.id()}]"
                    itemMap={"EXTEND": changeTypeOptions["EXTEND"]}>
                    <@duration.threeFieldDuration
                        dayPath="form.extendDuration[${view.id()}].days"
                        monthPath="form.extendDuration[${view.id()}].months"
                        yearPath="form.extendDuration[${view.id()}].years"
                        fieldNamePath="form.extendDuration[${view.id()}].fieldName"
                        fieldDisplayTextPath="form.extendDuration[${view.id()}].fieldDisplayText"
                        nestingPath="form.changeType[${view.id()}]"
                        labelText="How long is this to be extended by?"
                        formId="extend-${view.id()}"/>
                </@fdsRadio.radioItem>

            </@fdsRadio.radioGroup>

            <@fdsSummaryList.summaryList>
                <@fdsSummaryList.summaryListRowNoAction keyText="Current end date">
                    ${view.currentEndDate()}
                </@fdsSummaryList.summaryListRowNoAction>
                <@fdsSummaryList.summaryListRowNoAction keyText="Current duration">
                    ${view.currentDuration()}
                </@fdsSummaryList.summaryListRowNoAction>
            </@fdsSummaryList.summaryList>
        </#list>

        <@fdsAction.submitButtons
            primaryButtonText="Save and complete"
            secondaryLinkText="Cancel"
            linkSecondaryAction=true
            linkSecondaryActionUrl=springUrl(cancelUrl)/>

    </@fdsForm.htmlForm>
</@defaultPage>
