<#include '../../../../layout/layoutWithGisAssets.ftl'>
<#import "../../../../../gis/components/mapWithTextualDescription/mapWithTextualDescription.ftl" as mapWithTextualDescription>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle caption=pageCaption
  backLinkUrl=springUrl(backLinkUrl)
  pageSize=PageSize.FULL_COLUMN
>
    <@fdsForm.htmlForm>
      <#-- selectionInputName must match the checkboxes path (form.<name>) so map selection syncs with them -->
      <@mapWithTextualDescription.mapWithTextualDescription
        featureIds=activeFeatureIds
        srsWkid=srsWkid 
        selectionInputName="surrenderedFeatureIds"
      />

      <@fdsCheckbox.checkboxes
        path="form.surrenderedFeatureIds"
        checkboxes=areaCheckboxOptions
        fieldsetHeadingText="Areas to surrender"
        fieldsetHeadingSize="h2"
        fieldsetHeadingClass="govuk-fieldset__legend--m"
      />

      <@fdsAction.submitButtons
        primaryButtonText="Save and continue"
        secondaryLinkText="Back"
        linkSecondaryAction=true
        linkSecondaryActionUrl=springUrl(backLinkUrl)
      />
    </@fdsForm.htmlForm>
</@defaultPage>