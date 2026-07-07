<#include '../../layout/layout.ftl'>
<#import '../../component/duration/threeFieldDuration.ftl' as duration>

<@defaultPage htmlTitle=pageTitle pageHeading=pageTitle pageSize=PageSize.TWO_THIRDS_COLUMN breadcrumbs=breadcrumbs errorSummaryItems=errorSummaryItems>
    <@fdsForm.htmlForm>

        <@fdsTextarea.textarea
        path="form.reasonForAmendment"
        labelText="Why are you requesting the amendment or extension?"
        />

        <@fdsTextarea.textarea
        path="form.licenceProgress"
        labelText="What progress has the licensee made against the requirements of the licence?"
        hintText="Outline the progress on the licence work programme to date"
        />

        <@fdsDetails.summaryDetails summaryTitle="What information do I need to provide?">
          <p class="govuk-body">You must provide details of:</p>
          <ul class="govuk-list govuk-list--bullet">
            <li>Progress towards meeting the work programme commitments<#if isCarbonStorageLicence> including towards a potential storage permit application</#if></li>
            <li>Funding for the work programme activities</li>
            <li>Status of any operational activities on the licence</li>
          </ul>
        </@fdsDetails.summaryDetails>

        <#if (isExtension)>
            <@fdsTextarea.textarea
            path="form.planDuringExtension"
            labelText="What do you plan to do during the period of extension?"
            />
        </#if>

        <@fdsTextarea.textarea
        path="form.impactOnDeliverables"
        labelText="How do your requested changes impact on current or future deliverables?"
        />

        <@fdsFieldset.fieldset
        legendHeading="Provide documents supporting your request"
        legendHeadingClass="govuk-heading-m"
        optionalLabel=true>
            <@fdsFileUpload.fileUpload
            path=fileUploadAttributes.path()
            allowedExtensions=fileUploadAttributes.allowedExtensions()
            uploadUrl=fileUploadAttributes.uploadUrl()
            downloadUrl=fileUploadAttributes.downloadUrl()
            deleteUrl=fileUploadAttributes.deleteUrl()
            existingFiles=fileUploadAttributes.existingFiles()
            maxAllowedSize=fileUploadAttributes.maxAllowedSize()/>
        </@fdsFieldset.fieldset>

        <@fdsDetails.summaryDetails summaryTitle="What documents do I need to provide?">
          <p class="govuk-body">You must demonstrate that the proposed timeline is capable of delivering the licence
            work programme and any required investment decisions.</p>
          <p class="govuk-body">Example documents include:</p>
          <ul class="govuk-list govuk-list--bullet">
            <li>Present the impact on further work programme requirements in the form of a schedule</li>
            <li>Approved work programmes</li>
            <li>Budgets</li>
            <li>Agreed firm exploration schedule</li>
          </ul>
        </@fdsDetails.summaryDetails>
        <@fdsAction.submitButtons primaryButtonText="Save and complete" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(cancelUrl)/>

    </@fdsForm.htmlForm>
</@defaultPage>