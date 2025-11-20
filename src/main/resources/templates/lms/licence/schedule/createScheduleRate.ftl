<#include '../../layout/layout.ftl'>

<#assign pageTitle = "Schedule rate" />

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
caption=pageCaption
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems
>
    <@fdsForm.htmlForm>

        <@fdsRadio.radioGroup path="form.rateDefinitionOption" labelText="Rate defined for" hiddenContent=true>
            <#assign firstItem=true/>
            <#list rateDefinitionOptions as key, value>
                <@fdsRadio.radioItem path="form.rateDefinitionOption" itemMap={key : value} isFirstItem=firstItem>
                    <#if key = "TERM">
                        <@fdsSelect.select
                            path="form.licenceScheduleTermId"
                            options=termOptions
                            labelText="What term is the rate for?"
                            nestingPath="form.rateDefinitionOption"
                        />
                    </#if>

                    <#if key = "PHASE">
                        <@fdsSelect.select
                            path="form.licenceSchedulePhaseId"
                            options=phaseOptions
                            labelText="Which phase is the rate for?"
                            nestingPath="form.rateDefinitionOption"
                        />
                    </#if>

                    <#if key = "CUSTOM_PERIOD">
                        <@fdsDateInput.dateInput
                            dayPath="form.startDate.dayInput.inputValue"
                            monthPath="form.startDate.monthInput.inputValue"
                            yearPath="form.startDate.yearInput.inputValue"
                            labelText="Rate start date"
                            formId="start-date-input"
                            nestingPath="form.rateDefinitionOption"
                        />
                    </#if>
                </@fdsRadio.radioItem>
                <#assign firstItem=false/>
            </#list>
        </@fdsRadio.radioGroup>

        <@fdsTextInput.textInput
            path="form.rentalRate.inputValue"
            labelText="Rate by area factor"
            prefix="£"
            suffix="km2"
            inputClass="govuk-!-width-one-third"
        />

        <@fdsTextarea.textarea
            path="form.comments"
            labelText="Comments"
        />

        <@fdsAction.submitButtons primaryButtonText="Save and complete" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(cancelUrl)/>
    </@fdsForm.htmlForm>
</@defaultPage>