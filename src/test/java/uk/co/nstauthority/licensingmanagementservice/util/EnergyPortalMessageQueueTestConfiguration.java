package uk.co.nstauthority.licensingmanagementservice.util;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import uk.co.fivium.energyportalmessagequeue.sns.SnsService;
import uk.co.fivium.energyportalmessagequeue.sns.SnsTopicArn;
import uk.co.fivium.energyportalmessagequeue.sqs.SqsQueueUrl;
import uk.co.fivium.energyportalmessagequeue.sqs.SqsService;

@TestConfiguration
public class EnergyPortalMessageQueueTestConfiguration {

  @Bean
  @Primary
  SnsService testSnsService() {
    var mockService = mock(SnsService.class);

    var baseNameCaptor = ArgumentCaptor.forClass(String.class);

    when(mockService.getOrCreateTopic(baseNameCaptor.capture()))
        .thenAnswer(invocation -> new SnsTopicArn("%s-arn".formatted(baseNameCaptor.getValue())));

    return mockService;
  }

  @Bean
  @Primary
  SqsService testSqsService() {
    var mockService = mock(SqsService.class);

    var baseNameCaptor = ArgumentCaptor.forClass(String.class);

    when(mockService.getOrCreateQueue(baseNameCaptor.capture()))
        .thenAnswer(invocation -> new SqsQueueUrl("%s-url".formatted(baseNameCaptor.getValue())));

    return mockService;
  }
}
