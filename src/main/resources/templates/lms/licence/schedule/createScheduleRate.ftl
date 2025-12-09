<#include '../../layout/layout.ftl'>
<#import '../../component/duration/threeFieldDuration.ftl' as duration>

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
                        <@fdsSelect.select
                            path="form.relativeEventId"
                            options=relativeEventOptions
                            labelText="What is the due date relative to?"
                            nestingPath="form.rateDefinitionOption"
                        />

                        <@fdsRadio.radioGroup path="form.rateRelativeDateOption" labelText="When does the rate start?" hiddenContent=true nestingPath="form.rateDefinitionOption">
                            <#assign firstRelativeOption = true>
                            <#list relativeDateOptions as key, value>
                                <@fdsRadio.radioItem path="form.rateRelativeDateOption" itemMap={key : value} isFirstItem=firstRelativeOption>
                                    <#if key = "RELATIVE_TO_START_DATE">
                                        <@duration.threeFieldDuration
                                            dayPath="form.relativeDuration.days"
                                            monthPath="form.relativeDuration.months"
                                            yearPath="form.relativeDuration.years"
                                            fieldNamePath="form.relativeDuration.fieldName"
                                            fieldDisplayTextPath="form.relativeDuration.fieldDisplayText"
                                            labelText="The relative period by which the rate starts from"
                                            formId="rate-relative-duration"
                                            nestingPath="form.rateRelativeDateOption"
                                        />
                                    </#if>
                                </@fdsRadio.radioItem>
                                <#assign firstRelativeOption = false>
                            </#list>
                        </@fdsRadio.radioGroup>
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