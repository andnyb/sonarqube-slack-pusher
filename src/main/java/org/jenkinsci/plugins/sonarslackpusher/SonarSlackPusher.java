package org.jenkinsci.plugins.sonarslackpusher;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.*;
import org.kohsuke.stapler.DataBoundConstructor;

public class SonarSlackPusher extends Notifier {

    private String hook;
    private String sonarUrl;
    private String jobName;
    private String branch;

    @DataBoundConstructor
    public SonarSlackPusher(String hook, String sonarUrl, String jobName, String branch) {
        this.hook = hook;
        this.sonarUrl = sonarUrl;
        this.jobName = jobName;
        this.branch = branch;
    }

    @Override
    public boolean perform(
            AbstractBuild<?, ?> build,
            Launcher launcher,
            BuildListener listener) {
        // Do the action here...
        return true;
    }
/*
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }
*/
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
}

