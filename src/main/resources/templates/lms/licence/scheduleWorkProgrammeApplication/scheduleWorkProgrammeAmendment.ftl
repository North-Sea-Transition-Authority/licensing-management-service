<#include '../../layout/layout.ftl'>
<#import '../../component/duration/threeFieldDuration.ftl' as duration>


<#assign pageTitle = "Work programme amendments" />

<@defaultPage htmlTitle=pageTitle pageHeading=pageTitle pageSize=PageSize.TWO_THIRDS_COLUMN breadcrumbs=breadcrumbs errorSummaryItems=errorSummaryItems>

    <@fdsForm.htmlForm>

        <@fdsSummaryList.summaryListCard headingText=workProgrammeActivityDetails.category() summaryListId="Work programme amendment activity detail" >

            <@fdsSummaryList.summaryListRowNoAction keyText="Description">
                ${workProgrammeActivityDetails.description()}
            </@fdsSummaryList.summaryListRowNoAction>

            <@fdsSummaryList.summaryListRowNoAction  keyText="Due date">
                ${workProgrammeActivityDetails.dueDate()}
            </@fdsSummaryList.summaryListRowNoAction>

        </@fdsSummaryList.summaryListCard>

        <#if isLinkedRelativeDate>
            <@fdsRadio.radioGroup
                path="form.durationExtensionRequired"
                labelText="Are you requesting an extension to the work programme completion date?"
                hiddenContent=true
            >
                <@fdsRadio.radioYes path="form.durationExtensionRequired">
                    <@duration.threeFieldDuration
                    dayPath="form.workProgrammeExtensionDuration.days"
                    monthPath="form.workProgrammeExtensionDuration.months"
                    yearPath="form.workProgrammeExtensionDuration.years"
                    fieldNamePath="form.workProgrammeExtensionDuration.fieldName"
                    fieldDisplayTextPath="form.workProgrammeExtensionDuration.fieldDisplayText"
                    nestingPath="form.durationExtensionRequired"
                    labelText="How long would you like to request the completion date to be extended by?"
                    formId="amendment-duration"/>
                </@fdsRadio.radioYes>

                <@fdsRadio.radioNo path="form.durationExtensionRequired"/>

            </@fdsRadio.radioGroup>
            <@fdsRadio.radioGroup
                path="form.additionalInfoRequired"
                labelText="Are you requesting an amendment to the work programme activity content?"
                hiddenContent=true
            >
                <@fdsRadio.radioYes path="form.additionalInfoRequired">
                    <@fdsTextarea.textarea
                        path="form.workProgrammeAmendmentInformation"
                        nestingPath="form.additionalInfoRequired"
                        labelText="What amendments would you like to request?"
                    />
                </@fdsRadio.radioYes>

                <@fdsRadio.radioNo path="form.additionalInfoRequired"/>
            </@fdsRadio.radioGroup>
        <#else>
            <@fdsTextarea.textarea
                path="form.workProgrammeAmendmentInformation"
                nestingPath="form.additionalInfoRequired"
                labelText="What amendments would you like to request?"
            />
        </#if>

      <br>

        <@fdsAction.submitButtons
        primaryButtonText="Save and complete"
        secondaryLinkText="Cancel"
        linkSecondaryAction=true
        linkSecondaryActionUrl=springUrl(cancelUrl)/>
    </@fdsForm.htmlForm>

</@defaultPage>