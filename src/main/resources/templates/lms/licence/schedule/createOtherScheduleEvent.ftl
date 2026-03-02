<#include '../../layout/layout.ftl'>
<#import '../../component/duration/threeFieldDuration.ftl' as duration>

<#assign pageTitle = "Other schedule event" />

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
caption=pageCaption
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems
>
    <@fdsForm.htmlForm>

        <@fdsRadio.radioGroup
            path="form.otherScheduleEventCategory"
            labelText="Category"
            hiddenContent=true
        >
            <#assign firstItem=true/>
            <#list categoryRadioOptions as key, value>
                <@fdsRadio.radioItem path="form.otherScheduleEventCategory" itemMap={key : value} isFirstItem=firstItem>
                    <#if key = "OTHER_ACTIVITY">
                        <@fdsTextInput.textInput
                            path="form.otherCategoryName"
                            labelText="Other category"
                            nestingPath="form.otherScheduleEventCategory"
                        />
                    </#if>
                </@fdsRadio.radioItem>
                <#assign firstItem=false/>
            </#list>
        </@fdsRadio.radioGroup>

        <@fdsTextarea.textarea
            path="form.description"
            labelText="Description"
        />

        <@fdsRadio.radioGroup path="form.otherScheduleEventDateOption" labelText="When will this event occur?" hiddenContent=true>
            <#assign firstItem=true/>
            <#list eventDateRadioOptions as key, value>
                <@fdsRadio.radioItem path="form.otherScheduleEventDateOption" itemMap={key : value} isFirstItem=firstItem>
                    <#if key = "WITHIN_A_TERM">
                        <@fdsSelect.select
                            path="form.licenceScheduleTermId"
                            options=termOptions
                            labelText="Term event will occur within"
                            nestingPath="form.otherScheduleEventDateOption"
                        />
                    </#if>

                    <#if key = "WITHIN_A_PHASE">
                        <@fdsSelect.select
                            path="form.licenceSchedulePhaseId"
                            options=phaseOptions
                            labelText="Phase event will occur within"
                            nestingPath="form.otherScheduleEventDateOption"
                        />
                    </#if>

                    <#if key = "RELATIVE_DATE">
                        <@fdsSelect.select
                            path="form.relativeEventId"
                            options=relativeOptions
                            labelText="What is the event date relative to?"
                            nestingPath="form.otherScheduleEventDateOption"
                        />

                        <@duration.threeFieldDuration
                            dayPath="form.relativeDuration.days"
                            monthPath="form.relativeDuration.months"
                            yearPath="form.relativeDuration.years"
                            fieldNamePath="form.relativeDuration.fieldName"
                            fieldDisplayTextPath="form.relativeDuration.fieldDisplayText"
                            labelText="The relative period from which the event will occur"
                            formId="activity-relative-duration"
                            nestingPath="form.otherScheduleEventDateOption"
                        />
                    </#if>
                </@fdsRadio.radioItem>
                <#assign firstItem=false/>
            </#list>
        </@fdsRadio.radioGroup>

        <@fdsTextarea.textarea
            path="form.comments"
            labelText="Comments"
            optionalLabel=true
        />

        <@fdsAction.submitButtons primaryButtonText="Save and complete" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(cancelUrl)/>

    </@fdsForm.htmlForm>
</@defaultPage>