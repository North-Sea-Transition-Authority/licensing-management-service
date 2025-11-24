<#include '../../layout/layout.ftl'>
<#import '../../component/duration/threeFieldDuration.ftl' as duration>

<#assign pageTitle = "Schedule phase" />

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems>
    <@fdsForm.htmlForm>

        <@fdsRadio.radio
            path="form.phaseType"
            radioItems=radioOptions
            labelText="Phase type"
        />

        <@duration.threeFieldDuration
            dayPath="form.phaseDuration.days"
            monthPath="form.phaseDuration.months"
            yearPath="form.phaseDuration.years"
            fieldNamePath="form.phaseDuration.fieldName"
            fieldDisplayTextPath="form.phaseDuration.fieldDisplayText"
            labelText="How long is the phase?"
            formId="licence-phase-duration"
        />

        <@fdsTextarea.textarea
            path="form.comments"
            labelText="Comments"
        />

        <@fdsAction.submitButtons primaryButtonText="Save" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(cancelUrl)/>

    </@fdsForm.htmlForm>
</@defaultPage>