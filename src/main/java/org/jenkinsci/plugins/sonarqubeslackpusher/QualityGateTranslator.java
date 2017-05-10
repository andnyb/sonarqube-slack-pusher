package org.jenkinsci.plugins.sonarqubeslackpusher;

import org.json.simple.JSONObject;

public class QualityGateTranslator {

   private static final QualityGateTranslator instance = new QualityGateTranslator();

   private QualityGateTranslator() {}

   public static QualityGateTranslator getInstance() {
      return instance;
   }

   /**
    * Creates an error message based on the condition contents
    * @param condition JSON formatted string
    * @return formatted message
    */
   public String translate(JSONObject condition) {
      return createAlertText(condition);
   }

   /**
    * Creates an error message based on the condition contents
    * @param condition JSON formatted string
    * @return formatted message
    */
   public String translate(JSONObject condition, JSONObject timePeriod) {
      String alertText = createAlertText(condition);
      alertText += " over ";
      alertText += "'"+timePeriod.get("parameter")+"' ";
      alertText += timePeriod.get("mode");
      // possibly add run date from 'date'
      return alertText;
   }

   private String createAlertText(JSONObject condition) {
      String alertText = "";
      alertText += condition.get("metricKey");
      alertText += " '"+condition.get("actualValue")+"' ";
      alertText += (condition.get("comparator")).equals("GT") ? "> " : "< ";

      if (condition.get("status").equals("ERROR")) {
         alertText += "'" + condition.get("errorThreshold") + "'";
      } else {
         alertText += "'" + condition.get("warningThreshold") + "'";
      }
      return alertText;
   }
}
