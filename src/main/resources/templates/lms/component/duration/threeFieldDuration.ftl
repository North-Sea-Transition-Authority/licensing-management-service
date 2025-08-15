<#import '/spring.ftl' as spring>
<#import '../../../fds/components/error/error.ftl' as error>
<#import '../../../fds/components/fieldset/fieldset.ftl' as dateFieldset>
<#import '../../../fds/utilities/utilities.ftl' as fdsUtil>

<#macro threeFieldDuration
dayPath
monthPath
yearPath
labelText
formId
hintText=""
defaultHint=true
fieldsetHeadingSize="h2"
fieldsetHeadingClass="govuk-fieldset__legend--s"
optionalLabel=false
nestingPath=""
formGroupClass=""
inputClass=""
caption=""
captionClass="govuk-caption-s"
showLabelOnly=false
noFieldsetHeadingSize="--s"
moreNestedContent="">

    <@spring.bind dayPath/>
    <#local dayId=fdsUtil.sanitiseId(spring.status.expression)>
    <#local dayName=fdsUtil.getSpringStatusExpression()>
    <#local dayValue=fdsUtil.getSpringStatusValue()>
    <#local hasDayError=fdsUtil.hasSpringStatusErrors()>

    <@spring.bind monthPath/>
    <#local monthId=fdsUtil.sanitiseId(spring.status.expression)>
    <#local monthName=fdsUtil.getSpringStatusExpression()>
    <#local monthValue=fdsUtil.getSpringStatusValue()>
    <#local hasMonthError=fdsUtil.hasSpringStatusErrors()>

    <@spring.bind yearPath/>
    <#local yearId=fdsUtil.sanitiseId(spring.status.expression)>
    <#local yearName=fdsUtil.getSpringStatusExpression()>
    <#local yearValue=fdsUtil.getSpringStatusValue()>
    <#local hasYearError=fdsUtil.hasSpringStatusErrors()>

<#--Assign variable, left hand red border styling-->
    <#local hasError = hasDayError || hasMonthError || hasYearError>

    <@dateFieldset.fieldset
    legendHeading=labelText
    legendHeadingSize=fieldsetHeadingSize
    legendHeadingClass=fieldsetHeadingClass
    caption=caption
    captionClass=captionClass
    optionalLabel=optionalLabel
    hintText=hintText
    formErrorId=formId
    formHasError=hasError
    showHeadingOnly=showLabelOnly
    noFieldsetHeadingSize=noFieldsetHeadingSize
    formGroupClass=formGroupClass>

    <#--Re-bind for single error message-->
        <#if hasYearError>
            <@spring.bind yearPath/>
            <@error.inputError inputId="${formId}"/>
        <#elseif hasMonthError>
            <@spring.bind monthPath/>
            <@error.inputError inputId="${formId}"/>
        <#elseif hasDayError>
            <@spring.bind dayPath/>
            <@error.inputError inputId="${formId}"/>
        </#if>

        ${moreNestedContent}

        <div class="govuk-date-input" id="${formId}-date-input">
            <div class="govuk-date-input__item">
                <div class="govuk-form-group">
                    <label class="govuk-label govuk-date-input__label" for="${yearId}">Years</label>
                    <input class="govuk-input ${inputClass}<#if hasYearError> govuk-input--error</#if> govuk-date-input__input govuk-input--width-2" id="${yearId}" name="${yearName}" type="text" value="${yearValue}">
                </div>
            </div>

            <div class="govuk-date-input__item">
                <div class="govuk-form-group">
                    <label class="govuk-label govuk-date-input__label" for="${monthId}">Months</label>
                    <input class="govuk-input ${inputClass}<#if hasMonthError> govuk-input--error</#if> govuk-date-input__input govuk-input--width-2" id="${monthId}" name="${monthName}" type="text" value="${monthValue}">
                </div>
            </div>

            <div class="govuk-date-input__item">
                <div class="govuk-form-group">
                    <label class="govuk-label govuk-date-input__label" for="${dayId}">Days</label>
                    <input class="govuk-input ${inputClass}<#if hasDayError> govuk-input--error</#if> govuk-date-input__input govuk-input--width-2" id="${dayId}" name="${dayName}" type="text" value="${dayValue}">
                </div>
            </div>
        </div>
    </@dateFieldset.fieldset>

<#--Rebind your form when a component is used inside show/hide radio groups-->
    <#if nestingPath?has_content>
        <@spring.bind nestingPath/>
    </#if>
</#macro>