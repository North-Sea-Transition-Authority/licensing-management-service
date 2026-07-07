<#include '../../layout/layout.ftl'>
<#import 'summaryCardList/licenceContinuationWpaRequirementSummaryCard.ftl' as licenceContinuationWpaRequirementCard>

<@defaultPage
htmlTitle=pageTitle
pageHeading="Work programme activities"
caption=pageCaption
captionClass="govuk-caption-m"
pageSize=PageSize.TWO_THIRDS_COLUMN
breadcrumbs=breadcrumbs
errorSummaryItems=errorSummaryItems>

    <@fdsForm.htmlForm>
        <#list workProgrammeActivities as workProgrammeActivity>
            <@licenceContinuationWpaRequirementCard.workProgrammeActivities workProgrammeActivity=workProgrammeActivity/>
        </#list>
        <@fdsRadio.radioGroup
        path="form.workProgrammeActivitiesCompletionStatus"
        labelText="Have all work programme activities been completed and evidenced to the NSTA?"
        hiddenContent=true>
            <@fdsRadio.radioYes path="form.workProgrammeActivitiesCompletionStatus">
                <@fdsTextarea.textarea
                optionalLabel=true
                nestingPath="form.workProgrammeActivitiesCompletionStatus"
                path="form.furtherInformation"
                labelText="Is there any further information regarding these activities you wish to bring to the NSTA's attention?"/>
            </@fdsRadio.radioYes>
            <@fdsRadio.radioNo path="form.workProgrammeActivitiesCompletionStatus">
                <@fdsTextarea.textarea
                nestingPath="form.workProgrammeActivitiesCompletionStatus"
                path="form.actionsToCompleteWorkProgrammeActivities"
                labelText="What actions are being taken to complete any incomplete work programme activities?"/>
            </@fdsRadio.radioNo>
        </@fdsRadio.radioGroup>
        <@fdsAction.submitButtons primaryButtonText="Save and continue" secondaryLinkText="Back" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(cancelUrl)/>
    </@fdsForm.htmlForm>
</@defaultPage>