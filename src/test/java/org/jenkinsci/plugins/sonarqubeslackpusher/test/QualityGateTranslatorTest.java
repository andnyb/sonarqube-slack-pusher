package org.jenkinsci.plugins.sonarqubeslackpusher.test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.json.simple.parser.JSONParser;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.sonarqubeslackpusher.QualityGateTranslator;
import org.json.simple.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

public class QualityGateTranslatorTest {

   private static JSONObject errorCondition;
   private static JSONObject warnCondition;
   private static JSONObject sinceTimeCondition;
   private static JSONObject timePeriod;

   private QualityGateTranslator qgt = QualityGateTranslator.getInstance();

   @BeforeClass
   public static void readFiles() throws Exception {
      JSONParser parser = new JSONParser();
      errorCondition =
         (JSONObject) parser.parse(FileUtils.readFileToString(
            new File(QualityGateTranslatorTest.class.getResource("/error-condition-gate.json").getFile())));
      warnCondition =
         (JSONObject) parser.parse(FileUtils.readFileToString(
            new File(QualityGateTranslatorTest.class.getResource("/warn-condition-gate.json").getFile())));
      sinceTimeCondition =
         (JSONObject) parser.parse(FileUtils.readFileToString(
            new File(QualityGateTranslatorTest.class.getResource("/since-time-condition-gate.json").getFile())));
      timePeriod =
         (JSONObject) parser.parse(FileUtils.readFileToString(
            new File(QualityGateTranslatorTest.class.getResource("/time-period-condition-gate.json").getFile())));
   }

   @Test
   public void nonTimedErrorAlert() {
      assertThat(qgt.translate(errorCondition), is("critical_violations '9' > '0'"));
   }

   @Test
   public void nonTimedWarnAlert() {
      assertThat(qgt.translate(warnCondition), is("skipped_tests '2' < '0'"));
   }

   @Test
   public void timedErrorAlert() {
      assertThat(qgt.translate(sinceTimeCondition, timePeriod), is("new_major_violations '3' > '0' over '10' days"));
   }
}
