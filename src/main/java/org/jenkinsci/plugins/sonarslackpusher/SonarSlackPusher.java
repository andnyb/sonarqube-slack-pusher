package org.jenkinsci.plugins.sonarslackpusher;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
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
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.PrintStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class SonarSlackPusher extends Notifier {

    private String hook;
    private String sonarUrl;
    private String jobName;

    private PrintStream logger = null;

    // Notification contents
    public String branch = null;
    public String id;
    public List<Attachment> attachments = new ArrayList<Attachment>();

    @DataBoundConstructor
    public SonarSlackPusher(String hook, String sonarUrl, String jobName) {
        this.hook = hook;
        this.sonarUrl = sonarUrl.endsWith("/") ? sonarUrl.substring(0, sonarUrl.length()-1) : sonarUrl;
        this.jobName = jobName;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build,Launcher launcher,BuildListener listener) {
        logger = listener.getLogger();
        try {
            getAllNotifications(getSonarData());
            pushNotification();
        } catch (Exception e) {
            logger.println("[ssp] we have issues "+e.getMessage());
        }
        return true;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public String getDisplayName() {
            return "Sonar Slack pusher";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    private String getSonarData() throws Exception {
        HttpGet get = new HttpGet(sonarUrl + "/api/resources?metrics=qi-quality-index,coverage,test_success_density,blocker_violations,critical_violations&includealerts=true");
        HttpClient client = HttpClientBuilder.create().build();
        HttpResponse res;
        try {
            res = client.execute(get);
            if (res.getStatusLine().getStatusCode() != 200) {
                logger.println("[ssp] could not get Sonar results...");
            }
            return EntityUtils.toString(res.getEntity());
        } catch (Exception e) {
            logger.println("[ssp] could not get Sonar results...");
            throw e;
        }
    }

    public void getAllNotifications(String data) throws Exception {
        JSONParser jsonParser = new JSONParser();
        JSONArray jobs = (JSONArray)jsonParser.parse(data);
        for (Object job : jobs) {
            if (((JSONObject)job).get("name").toString().equals(jobName)) {
                id = ((JSONObject)job).get("id").toString();
                if (((JSONObject)job).get("branch")!=null) {
                    branch = ((JSONObject)job).get("branch").toString();
                }
                JSONArray msrs = (JSONArray)((JSONObject)job).get("msr");
                for (Object msr : msrs) {
                    if (((JSONObject)msr).get("alert")!=null) {
                        String alert = ((JSONObject)msr).get("alert").toString();
                        if (alert.equalsIgnoreCase("ERROR") || alert.equalsIgnoreCase("WARN")) {
                            Attachment a = new Attachment();
                            a.setAlert(alert);
                            a.setReason(((JSONObject)msr).get("alert_text").toString());
                            a.setRule(((JSONObject)msr).get("key").toString());
                            a.setValue(((JSONObject)msr).get("frmt_val").toString());
                            attachments.add(a);
                        }
                    }
                }
            }
        }
    }

    public void pushNotification() throws Exception {
        String linkUrl = new URI(sonarUrl+"/dashboard/index/"+id).normalize().toString();
        String message =
                "{\"text\":\"<"+linkUrl+"|*Sonar job*>\\n"+
                "*Job:* "+jobName;
        if (branch!=null) {
            message += "\\n*Branch:* "+branch;
        }
        message += "\",\"attachments\":[";
        for (int i=0; i<attachments.size(); i++) {
            message += attachments.get(i).getAttachment();
            if (i<(attachments.size()-1)) {
                message += ",";
            }
        }
        message += "]}";
        HttpPost post = new HttpPost(hook);
        HttpEntity entity = new StringEntity(message, "UTF-8");
        post.addHeader("Content-Type", "application/json");
        post.setEntity(entity);
        HttpClient client = HttpClientBuilder.create().build();
        HttpResponse res = client.execute(post);
        if (res.getStatusLine().getStatusCode() != 200) {
            logger.println("[ssp] could not push to slack...");
        }
    }
}

