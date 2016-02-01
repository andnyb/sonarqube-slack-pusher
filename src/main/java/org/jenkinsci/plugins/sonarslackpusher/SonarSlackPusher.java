package org.jenkinsci.plugins.sonarslackpusher;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Notifies a configured Slack channel of Sonar quality gate checks
 * through the Sonar API.
 */
public class SonarSlackPusher extends Notifier {

   private String hook;
   private String sonarUrl;
   private String jobName;
   private String branchName;
   private String resolvedBranchName;
   private String additionalChannel;

   private PrintStream logger = null;

   // Notification contents
   private String branch = null;
   private String id;
   private Attachment attachment = null;

   @DataBoundConstructor
   public SonarSlackPusher(String hook, String sonarUrl, String jobName, String branchName, String additionalChannel) {
      this.hook = hook.trim();
      String url = sonarUrl.trim();
      this.sonarUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
      this.jobName = jobName.trim();
      this.branchName = branchName.trim();
      this.additionalChannel = additionalChannel.trim();
   }

   public String getHook() {
      return hook;
   }

   public String getSonarUrl() {
      return sonarUrl;
   }

   public String getJobName() {
      return jobName;
   }

   public String getBranchName() {
      return branchName;
   }

   public String getAdditionalChannel() {
      return additionalChannel;
   }

   @Override
   public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
      // Clean up
      attachment = null;
      logger = listener.getLogger();
      resolvedBranchName = parameterReplacement(branchName, build, listener);
      try {
         getAllNotifications(getSonarData());
      } catch (Exception e) {
         return false;
      }
      pushNotification();
      return true;
   }

   private String parameterReplacement(String str, AbstractBuild<?, ?> build, BuildListener listener) {
      try {
         EnvVars env = build.getEnvironment(listener);
         env.overrideAll(build.getBuildVariables());
         ArrayList<String> params = getParams(str);
         for (String param : params) {
            if (env.containsKey(param)) {
               str = env.get(param);
            } else if (build.getBuildVariables().containsKey(param)) {
               str = build.getBuildVariables().get(param);
            }
         }
      } catch (InterruptedException ie) {
      } catch (IOException ioe) {
      } finally {
         return str;
      }
   }

   private ArrayList<String> getParams(String str) {
      ArrayList<String> params = new ArrayList<String>();
      final String start = java.util.regex.Pattern.quote("${");
      final String end = "}";

      String[] rawParams = str.split(start);
      for (int i = 1; i < rawParams.length; i++) {
         if (rawParams[i].contains(end)) {
            String[] raw = rawParams[i].split(java.util.regex.Pattern.quote(end));
            if (raw.length > 0) {
               System.out.println("Adding: " + raw[0]);
               params.add(raw[0]);
            }
         }
      }
      return params;
   }

   @Extension
   public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

      public DescriptorImpl() {
         load();
      }

      @Override
      public String getDisplayName() {
         return "Sonar Slack pusher";
      }

      @Override
      public boolean isApplicable(Class<? extends AbstractProject> jobType) {
         return true;
      }

      public FormValidation doCheckHook(@QueryParameter String value)
         throws IOException, ServletException {
         String url = value;
         if ((url == null) || url.equals("")) {
            return FormValidation.error("Please specify a valid URL");
         } else {
            try {
               new URL(url);
               return FormValidation.ok();
            } catch (Exception e) {
               return FormValidation.error("Please specify a valid URL.");
            }
         }
      }

      public FormValidation doCheckSonarUrl(@QueryParameter String value)
         throws IOException, ServletException {
         String url = value;
         if ((url == null) || url.equals("")) {
            return FormValidation.error("Please specify a valid URL");
         } else {
            try {
               new URL(url);
               return FormValidation.ok();
            } catch (Exception e) {
               return FormValidation.error("Please specify a valid URL.");
            }
         }
      }

      public FormValidation doCheckJobName(@QueryParameter String value)
         throws IOException, ServletException {
         String name = value;
         if ((name == null) || name.equals("")) {
            return FormValidation.error("Please enter a Sonar job name.");
         }
         return FormValidation.ok();
      }
   }

   public BuildStepMonitor getRequiredMonitorService() {
      return BuildStepMonitor.NONE;
   }

   private String getSonarData() throws Exception {
      HttpGet get = new HttpGet(sonarUrl + "/api/resources?metrics=alert_status,quality_gate_details&includealerts=true");
      HttpClient client = HttpClientBuilder.create().build();
      HttpResponse res;
      try {
         logger.println("[ssp] calling SonarQube on: " + sonarUrl + "/api/resources?metrics=alert_status,quality_gate_details&includealerts=true");
         res = client.execute(get);
         if (res.getStatusLine().getStatusCode() != 200) {
            logger.println("[ssp] got a non 200 response from Sonar at URL: '" + sonarUrl + "/api/resources?metrics=qi-quality-index,coverage,test_success_density,blocker_violations,critical_violations&includealerts=true'");
         }
         return EntityUtils.toString(res.getEntity());
      } catch (Exception e) {
         logger.println("[ssp] could not get Sonar results, exception: '" + e.getMessage() + "'");
         throw e;
      }
   }

   private void getAllNotifications(String data) {
      JSONParser jsonParser = new JSONParser();
      JSONArray jobs = null;
      try {
         jobs = (JSONArray) jsonParser.parse(data);
      } catch (ParseException pe) {
         logger.println("[ssp] could not parse the response from Sonar '" + data + "'");
      }
      for (Object job : jobs) {
         String name = jobName;
         if (resolvedBranchName != null && !resolvedBranchName.equals("")) {
            name += " " + resolvedBranchName;
         }
         if (((JSONObject) job).get("name").toString().equals(name)) {
            id = ((JSONObject) job).get("id").toString();
            if (((JSONObject) job).get("branch") != null) {
               branch = ((JSONObject) job).get("branch").toString();
            }
            JSONArray msrs = (JSONArray) ((JSONObject) job).get("msr");
            for (Object msr : msrs) {
               if (((JSONObject) msr).get("key").equals("alert_status")) {
                  if (((JSONObject) msr).get("alert") != null) {
                     String alert = ((JSONObject) msr).get("alert").toString();
                     if (alert.equalsIgnoreCase("ERROR") || alert.equalsIgnoreCase("WARN")) {
                        attachment = new Attachment();
                        attachment.setAlert(alert);
                        attachment.setAlertText(((JSONObject) msr).get("alert_text").toString());
                     }
                  }
               }
            }
         }
      }
   }

   private void pushNotification() {
      if (attachment == null) {
         logger.println("[ssp] no failed quality checks for project '" + jobName + " " + branch + "' nothing to report to the Slack channel.");
         return;
      }
      String linkUrl = null;
      try {
         linkUrl = new URI(sonarUrl + "/dashboard/index/" + id).normalize().toString();
      } catch (URISyntaxException use) {
         logger.println("[ssp] could not create link to Sonar job with the following content'" + sonarUrl + "/dashboard/index/" + id + "'");
      }
      String message = "{";
      if (additionalChannel != null) {
         message += "\"channel\":\"" + additionalChannel + "\",";
      }
      message += "\"username\":\"Sonar Slack Pusher\",";
      message += "\"text\":\"<" + linkUrl + "|*Sonar job*>\\n" +
         "*Job:* " + jobName;
      if (branch != null) {
         message += "\\n*Branch:* " + branch;
      }
      message += "\",\"attachments\":[";
      message += attachment.getAttachment();
      message += "]}";
      HttpPost post = new HttpPost(hook);
      HttpEntity entity = new StringEntity(message, "UTF-8");
      post.addHeader("Content-Type", "application/json");
      post.setEntity(entity);
      HttpClient client = HttpClientBuilder.create().build();
      logger.println("[ssp] pushing notification(s) to the Slack channel.");
      try {
         HttpResponse res = client.execute(post);
         if (res.getStatusLine().getStatusCode() != 200) {
            logger.println("[ssp] could not push to Slack... got a non 200 response. Post body: '" + message + "'");
         }
      } catch (IOException ioe) {
         logger.println("[ssp] could not push to slack... got an exception: '" + ioe.getMessage() + "'");
      }
   }
}
