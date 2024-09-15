package ust.tad.terraformmpsplugin.analysistaskresponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ust.tad.terraformmpsplugin.analysistask.AnalysisTaskResponseSender;

@SpringBootTest
public class AnalysisTaskResponseSenderTest {

  @Autowired AnalysisTaskResponseSender analysisTaskResponseSender;

  @Test
  public void sendAnalysisTaskResponse() throws JsonProcessingException {
    analysisTaskResponseSender.sendSuccessResponse(UUID.randomUUID());
  }
}
