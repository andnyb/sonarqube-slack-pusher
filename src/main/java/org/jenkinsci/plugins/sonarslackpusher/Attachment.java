package org.jenkinsci.plugins.sonarslackpusher;

public class Attachment {

    private String alert;
    private String color;
    private String reason;
    private String rule;
    private String value;

    public String getAttachment() {
        return "{\"text\":\"*"+alert+"*\\n*Quality gate:* "+rule+"\\n*Reason:* "+reason+"\\n*Value:* "+value+"\",\"color\":\""+color+"\",\"mrkdwn_in\": [\"text\"]}";
    }

    public void setAlert(String alert) {
        this.alert = alert;
        this.color = alert.equalsIgnoreCase("ERROR") ? "danger" : "warning";
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
