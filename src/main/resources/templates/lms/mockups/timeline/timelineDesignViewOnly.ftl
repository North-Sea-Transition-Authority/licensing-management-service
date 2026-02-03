<#include '../../layout/layout.ftl'>
<#import '../../search/search.ftl' as search>
<#import 'customTimeline.ftl' as custom>

<@defaultPage
htmlTitle="P2664 - Licence schedule and work programme"
pageHeading="P2664 - Licence schedule and work programme"
pageSize=PageSize.FULL_COLUMN
extendContainerWidth=true>

    <@fdsSummaryList.summaryListCard headingText="Schedule details" summaryListId="summary-card-list">
        <@fdsSummaryList.summaryListRowNoAction keyText="Start date">
            1 June 2024
        </@fdsSummaryList.summaryListRowNoAction>
        <@fdsSummaryList.summaryListRowNoAction keyText="End date">
            31 May 2052
        </@fdsSummaryList.summaryListRowNoAction>
        <@fdsSummaryList.summaryListRowNoAction keyText="Duration">
            28 years
        </@fdsSummaryList.summaryListRowNoAction>
        <@fdsSummaryList.summaryListRowNoAction keyText="Licence round">
            6
        </@fdsSummaryList.summaryListRowNoAction>
    </@fdsSummaryList.summaryListCard>

    <@fdsSearch.searchPage>

        <@fdsSearch.searchFilter>
            <@fdsSearch.searchFilterList
            clearFilterText="Clear filters"
            clearFilterUrl="#">
                <@fdsSearch.searchFilterItem itemName="Show" expanded=true>
                    <@fdsSearch.searchCheckboxes path="form.filter" checkboxes=options/>
                </@fdsSearch.searchFilterItem>
            </@fdsSearch.searchFilterList>
        </@fdsSearch.searchFilter>

        <@fdsSearch.searchPageContent>

            <@fdsAccordion.accordion accordionId="accordion-example">
                <@fdsAccordion.accordionSection sectionHeading="Initial Term" summaryText="1 June 2024 to 31 May 2030 (6 years)">
                    <@fdsTimeline.timeline>
                        <@fdsTimeline.timelineSection>
                            <@fdsTimeline.timelineTimeStamp
                            timeStampHeading="Phase A"
                            timeStampHeadingHint="1 June 2024 to 31 May 2028 (4 years)"
                            >
                            </@fdsTimeline.timelineTimeStamp>
                            <@custom.timelineTimeStampSmall
                            timeStampHeading="Phase A yearly rate"
                            timeStampHeadingHint="1 June 2024 to 31 May 2028 (4 years)"
                            timeStampHeadingSize="h4">
                                <@fdsTimeline.timelineEvent>
                                    <p class="govuk-body">
                                        £15 per km<sup>2</sup>
                                    </p>
                                </@fdsTimeline.timelineEvent>
                            </@custom.timelineTimeStampSmall>
                            <@fdsTimeline.timelineTimeStamp
                            timeStampHeading="End of phase requirements"
                            >
                            </@fdsTimeline.timelineTimeStamp>
                            <@custom.timelineTimeStampSmall
                            timeStampHeading="Obtain 3D Seismic Data"
                            >
                                <@fdsTimeline.timelineEvent>
                                    <p class="govuk-body">
                                        Obtain and interpret 270 sq kms 3D seismic data reprocessed to Pre-SDM.
                                    </p>

                                    <@fdsDetails.summaryDetails summaryTitle="Comments">
                                        <p class="govuk-body">
                                            This is a comment
                                        </p>
                                    </@fdsDetails.summaryDetails>
                                </@fdsTimeline.timelineEvent>
                            </@custom.timelineTimeStampSmall>
                            <@custom.timelineTimeStampSmall
                            timeStampHeading="Complete a biostratigraphy study"
                            >
                                <@fdsTimeline.timelineEvent>
                                    <p class="govuk-body">
                                        Complete a biostratigraphy study focused on the Carboniferous.
                                    </p>
                                </@fdsTimeline.timelineEvent>
                            </@custom.timelineTimeStampSmall>
                            <@custom.timelineTimeStampSmall
                            timeStampHeading="Complete a fault seal study"
                            >
                                <@fdsTimeline.timelineEvent>
                                    <p class="govuk-body">
                                        Complete a fault seal study including fault juxtaposition and membrane seal analysis.
                                    </p>
                                </@fdsTimeline.timelineEvent>
                            </@custom.timelineTimeStampSmall>
                            <@custom.timelineTimeStampSmall
                            timeStampHeading="Complete an engineering study"
                            >
                                <@fdsTimeline.timelineEvent>
                                    <p class="govuk-body">
                                        Complete an engineering study on processing and abating emission of the expected high carbon dioxide in the production flow-stream.
                                    </p>
                                </@fdsTimeline.timelineEvent>
                            </@custom.timelineTimeStampSmall>
                            <@fdsTimeline.timelineTimeStamp
                            timeStampHeading="End of Phase A"
                            timeStampHeadingHint="31 May 2028"
                            timeStampClass="fds-timeline__time-stamp--no-border">
                            </@fdsTimeline.timelineTimeStamp>
                            <@fdsTimeline.timelineTimeStamp
                            timeStampHeading="Phase C"
                            timeStampHeadingHint="1 June 2028 to 31 May 2030 (2 years)"
                            >
                            </@fdsTimeline.timelineTimeStamp>
                            <@custom.timelineTimeStampSmall
                            timeStampHeading="Phase C yearly rate"
                            timeStampHeadingHint="1 June 2028 to 31 May 2030 (2 years)"
                            timeStampHeadingSize="h4">
                                <@fdsTimeline.timelineEvent>
                                    <p class="govuk-body">
                                        £150 per km<sup>2</sup>
                                    </p>
                                </@fdsTimeline.timelineEvent>
                            </@custom.timelineTimeStampSmall>
                            <@fdsTimeline.timelineTimeStamp
                            timeStampHeading="End of phase requirements"
                            timeStampHeadingHint="31 May 2030">
                            </@fdsTimeline.timelineTimeStamp>
                            <@custom.timelineTimeStampSmall
                            timeStampHeading="Mandatory relinquishment"
                            >
                                <@fdsTimeline.timelineEvent>
                                    <p class="govuk-body">
                                        50%
                                    </p>
                                </@fdsTimeline.timelineEvent>
                            </@custom.timelineTimeStampSmall>
                            <@custom.timelineTimeStampSmall
                            timeStampHeading="Well drill"
                            >
                                <@fdsTimeline.timelineEvent>
                                    <p class="govuk-body">
                                        Drill a well to 4,350m TVDSS or to the top Westphalian A, whichever is the shallower.
                                    </p>
                                </@fdsTimeline.timelineEvent>
                            </@custom.timelineTimeStampSmall>
                            <@fdsTimeline.timelineTimeStamp
                            timeStampHeading="End of Phase C"
                            timeStampHeadingHint="31 May 2030"
                            timeStampClass="fds-timeline__time-stamp--no-border">
                            </@fdsTimeline.timelineTimeStamp>
                        </@fdsTimeline.timelineSection>
                    </@fdsTimeline.timeline>
                </@fdsAccordion.accordionSection>
                <@fdsAccordion.accordionSection sectionHeading="Second Term" summaryText="1 June 2030 to 31 May 2034 (4 years)">
                    <@fdsTimeline.timeline>
                        <@fdsTimeline.timelineSection>
                            <@fdsTimeline.timelineTimeStamp
                            timeStampHeading="Start of Second Term"
                            timeStampHeadingHint="1 June 2030 to 31 May 2034 (4 years)"
                            >
                            </@fdsTimeline.timelineTimeStamp>
                            <@custom.timelineTimeStampSmall
                            timeStampHeading="Rate"
                            timeStampHeadingHint="1 June 2030 to 31 May 2031 (1 year)"
                            >
                                <@fdsTimeline.timelineEvent>
                                    <p class="govuk-body">
                                        £300 per km<sup>2</sup>
                                    </p>
                                </@fdsTimeline.timelineEvent>
                            </@custom.timelineTimeStampSmall>
                            <@custom.timelineTimeStampSmall
                            timeStampHeading="Rate"
                            timeStampHeadingHint="1 June 2031 to 31 May 2032 (1 year)"
                            >
                                <@fdsTimeline.timelineEvent>
                                    <p class="govuk-body">
                                        £900 per km<sup>2</sup>
                                    </p>
                                </@fdsTimeline.timelineEvent>
                            </@custom.timelineTimeStampSmall>
                            <@custom.timelineTimeStampSmall
                            timeStampHeading="Rate"
                            timeStampHeadingHint="1 June 2032 to 31 May 2033 (1 year)"
                            >
                                <@fdsTimeline.timelineEvent>
                                    <p class="govuk-body">
                                        £1800 per km<sup>2</sup>
                                    </p>
                                </@fdsTimeline.timelineEvent>
                            </@custom.timelineTimeStampSmall>
                            <@custom.timelineTimeStampSmall
                            timeStampHeading="Rate"
                            timeStampHeadingHint="1 June 2033 to 31 May 2034 (1 year)"
                            >
                                <@fdsTimeline.timelineEvent>
                                    <p class="govuk-body">
                                        £2700 per km<sup>2</sup>
                                    </p>
                                </@fdsTimeline.timelineEvent>
                            </@custom.timelineTimeStampSmall>
                            <@fdsTimeline.timelineTimeStamp
                            timeStampHeading="End of term requirements"
                            >
                            </@fdsTimeline.timelineTimeStamp>
                            <@custom.timelineTimeStampSmall
                            timeStampHeading="Mandatory relinquishment"
                            >
                            </@custom.timelineTimeStampSmall>
                            <@fdsTimeline.timelineTimeStamp
                            timeStampHeading="End of Second Term"
                            timeStampHeadingHint="31 May 2034"
                            timeStampClass="fds-timeline__time-stamp--no-border">
                            </@fdsTimeline.timelineTimeStamp>
                        </@fdsTimeline.timelineSection>
                    </@fdsTimeline.timeline>
                </@fdsAccordion.accordionSection>
                <@fdsAccordion.accordionSection sectionHeading="Third Term" summaryText="1 June 2034 to 31 May 2052 (18 years)">
                    <@fdsTimeline.timeline>
                        <@fdsTimeline.timelineSection>
                            <@fdsTimeline.timelineTimeStamp
                            timeStampHeading="Start of Third Term"
                            timeStampHeadingHint="1 June 2034 to 31 May 2052 (18 years)"
                            >
                            </@fdsTimeline.timelineTimeStamp>
                            <@custom.timelineTimeStampSmall
                            timeStampHeading="Rate"
                            timeStampHeadingHint="1 June 2034 to 31 May 2035 (1 year)"
                            >
                                <@fdsTimeline.timelineEvent>
                                    <p class="govuk-body">
                                        £3900 per km<sup>2</sup>
                                    </p>
                                </@fdsTimeline.timelineEvent>
                            </@custom.timelineTimeStampSmall>
                            <@custom.timelineTimeStampSmall
                            timeStampHeading="Rate"
                            timeStampHeadingHint="1 June 2035 to 31 May 2036 (1 year)"
                            >
                                <@fdsTimeline.timelineEvent>
                                    <p class="govuk-body">
                                        £5100 per km<sup>2</sup>
                                    </p>
                                </@fdsTimeline.timelineEvent>
                            </@custom.timelineTimeStampSmall>
                            <@custom.timelineTimeStampSmall
                            timeStampHeading="Rate"
                            timeStampHeadingHint="1 June 2036 to 31 May 2037 (1 year)"
                            >
                                <@fdsTimeline.timelineEvent>
                                    <p class="govuk-body">
                                        £6300 per km<sup>2</sup>
                                    </p>
                                </@fdsTimeline.timelineEvent>
                            </@custom.timelineTimeStampSmall>
                            <@custom.timelineTimeStampSmall
                            timeStampHeading="Yearly recurring rate"
                            timeStampHeadingHint="1 June 2037 to to 31 May 2052 (15 years)"
                            >
                                <@fdsTimeline.timelineEvent>
                                    <p class="govuk-body">
                                        £7500 per km<sup>2</sup>
                                    </p>
                                </@fdsTimeline.timelineEvent>
                            </@custom.timelineTimeStampSmall>
                            <@fdsTimeline.timelineTimeStamp
                            timeStampHeading="End of Third Term"
                            timeStampHeadingHint="31 May 2052"
                            timeStampClass="fds-timeline__time-stamp--no-border">
                            </@fdsTimeline.timelineTimeStamp>
                        </@fdsTimeline.timelineSection>
                    </@fdsTimeline.timeline>
                </@fdsAccordion.accordionSection>
            </@fdsAccordion.accordion>

        </@fdsSearch.searchPageContent>
    </@fdsSearch.searchPage>

</@defaultPage>