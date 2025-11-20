<#include '../../layout/layout.ftl'>

<#assign pageTitle = "Work programme activity" />

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems
>
    <@fdsForm.htmlForm>

        <@fdsRadio.radioGroup
            path="form.workProgrammeActivityCategory"
            labelText="Category"
            hiddenContent=true
        >
            <#assign firstItem=true/>
            <#list categoryRadioOptions as key, value>
                <@fdsRadio.radioItem path="form.workProgrammeActivityCategory" itemMap={key : value} isFirstItem=firstItem>
                    <#if key = "OTHER_ACTIVITY">
                        <@fdsTextInput.textInput
                            path="form.otherCategoryName"
                            labelText="Other category"
                            nestingPath="form.workProgrammeActivityCategory"
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

        <@fdsRadio.radio
            path="form.workProgrammeActivityCommitment"
            radioItems=commitmentRadioOptions
            labelText="Commitment"
        />

        <@fdsRadio.radioGroup path="form.workProgrammeActivityDateOption" labelText="By when must this work programme activity be completed?" hiddenContent=true>
            <#assign firstItem=true/>
            <#list activityDateRadioOptions as key, value>
                <@fdsRadio.radioItem path="form.workProgrammeActivityDateOption" itemMap={key : value} isFirstItem=firstItem>
                    <#if key = "FIXED_DATE">
                        <@fdsDateInput.dateInput
                            dayPath="form.dueDateInput.dayInput.inputValue"
                            monthPath="form.dueDateInput.monthInput.inputValue"
                            yearPath="form.dueDateInput.yearInput.inputValue"
                            labelText="Due date"
                            formId="due-date-input"
                            nestingPath="form.workProgrammeActivityDateOption"
                        />
                    </#if>

                    <#if key = "WITHIN_A_TERM">
                        <@fdsSelect.select
                            path="form.licenceScheduleTermId"
                            options=termOptions
                            labelText="Term work programme activity must be completed within"
                            nestingPath="form.workProgrammeActivityDateOption"
                        />
                    </#if>

                    <#if key = "WITHIN_A_PHASE">
                        <@fdsSelect.select
                            path="form.licenceSchedulePhaseId"
                            options=phaseOptions
                            labelText="Phase work programme activity must be completed within"
                            nestingPath="form.workProgrammeActivityDateOption"
                        />
                    </#if>
                </@fdsRadio.radioItem>
                <#assign firstItem=false/>
            </#list>
        </@fdsRadio.radioGroup>

        <@fdsTextarea.textarea
            path="form.comments"
            labelText="Comments"
        />

        <@fdsAction.submitButtons primaryButtonText="Save and complete" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(cancelUrl)/>

    </@fdsForm.htmlForm>
</@defaultPage>