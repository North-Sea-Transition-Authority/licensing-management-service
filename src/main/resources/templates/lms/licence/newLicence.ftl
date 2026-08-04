<#include '../layout/layout.ftl'>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems>
    <@fdsForm.htmlForm>

        <@fdsRadio.radioGroup
            path="form.licenceType"
            labelText="Select the licence type"
            hiddenContent=true>
            <#assign firstItem=true/>
            <#list licenceTypeOptions as key, value>
                <@fdsRadio.radioItem path="form.licenceType" itemMap={key : value} isFirstItem=firstItem/>
                <#assign firstItem=false/>
            </#list>
        </@fdsRadio.radioGroup>

        <@fdsTextInput.textInput
            path="form.licenceNumber"
            labelText="What is the licence number?"
            hintText="For example, 100 or 100a. The prefix will be applied automatically based on the licence type."
            inputClass="govuk-!-width-one-third"
        />

        <@fdsFieldset.fieldset legendHeading="Add licensees" showHeadingOnly=true legendHeadingSize="h2">
            <@fdsAddToList.addToList
                pathForList="form.organisationUnitIds"
                pathForSelector="form.organisationUnitSelector"
                restUrl=springUrl(organisationUnitSearchEndpoint)
                alreadyAdded=preselectedOrgUnits
                itemName="Licensees"
            />
            <@fdsDetails.summaryDetails
              summaryTitle="The licensee I want to select is not shown in the list"
            >
              <p class="govuk-body">
                If the licensee you want to select is not shown in the list then you can <@requestNewCompany.requestCompanyLink/>
              </p>
            </@fdsDetails.summaryDetails>
        </@fdsFieldset.fieldset>

        <@fdsAction.submitButtons
            primaryButtonText="Create licence"
            secondaryLinkText="Cancel"
            linkSecondaryAction=true
            linkSecondaryActionUrl=springUrl(backUrl)
        />
    </@fdsForm.htmlForm>
</@defaultPage>