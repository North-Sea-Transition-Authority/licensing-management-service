<#include '../../layout/layout.ftl'>
<#import '../../component/duration/threeFieldDuration.ftl' as duration>

<#assign pageTitle = "Schedule term" />

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems>
    <@fdsForm.htmlForm>

        <@fdsRadio.radio
            path="form.termType"
            radioItems=radioOptions
            labelText="Term type"
        />

        <@duration.threeFieldDuration
            dayPath="form.termDuration.days"
            monthPath="form.termDuration.months"
            yearPath="form.termDuration.years"
            labelText="How long is the term?"
            formId="licence-term-duration"
        />

        <@fdsAction.submitButtons primaryButtonText="Save" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(cancelUrl)/>

    </@fdsForm.htmlForm>
</@defaultPage>