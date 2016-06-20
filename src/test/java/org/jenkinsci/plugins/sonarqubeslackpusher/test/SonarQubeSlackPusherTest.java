package org.jenkinsci.plugins.sonarqubeslackpusher.test;

import org.jenkinsci.plugins.sonarqubeslackpusher.Attachment;
import org.jenkinsci.plugins.sonarqubeslackpusher.SonarQubeSlackPusher;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SonarQubeSlackPusherTest {

   @Test
   public void testReplaceWithParameters() throws Exception {
      SonarQubeSlackPusher ssp = new SonarQubeSlackPusher("", "", "http://sonar.company.org:9000", "", "", "", "");
      Method replace = ssp.getClass().getDeclaredMethod("getParams", String.class);
      replace.setAccessible(true);

      String s = "abc${efg}hij";
      assertEquals(1, ((List<String>) (replace.invoke(ssp, s))).size());
      assertEquals("efg", ((ArrayList<String>) (replace.invoke(ssp, s))).get(0));

      s = "123${köadsfökjadsf";
      assertEquals(0, ((List<String>) (replace.invoke(ssp, s))).size());

      s = "123${456${789}0";
      assertEquals(1, ((List<String>) (replace.invoke(ssp, s))).size());
      assertEquals("789", ((ArrayList<String>) (replace.invoke(ssp, s))).get(0));

      s = "${}";
      assertEquals(0, ((List<String>) (replace.invoke(ssp, s))).size());

      s = "${456${789}0";
      assertEquals(1, ((List<String>) (replace.invoke(ssp, s))).size());
      assertEquals("789", ((ArrayList<String>) (replace.invoke(ssp, s))).get(0));

      s = "${456${789}";
      assertEquals(1, ((List<String>) (replace.invoke(ssp, s))).size());
      assertEquals("789", ((ArrayList<String>) (replace.invoke(ssp, s))).get(0));

      s = "${456}${789}";
      assertEquals(2, ((List<String>) (replace.invoke(ssp, s))).size());
      assertEquals("456", ((ArrayList<String>) (replace.invoke(ssp, s))).get(0));
      assertEquals("789", ((ArrayList<String>) (replace.invoke(ssp, s))).get(1));

      s = "123${456}${789}0";
      assertEquals(2, ((List<String>) (replace.invoke(ssp, s))).size());
      assertEquals("456", ((ArrayList<String>) (replace.invoke(ssp, s))).get(0));
      assertEquals("789", ((ArrayList<String>) (replace.invoke(ssp, s))).get(1));
   }

   @Test
   public void testSplitUpOfAlerts() {
      Attachment a = new Attachment();
      a.setAlertText("Unit tests errors > 0, Unit tests failures > 0, Skipped unit tests > 0, Major issues > 10, Critical issues > 0, Overall coverage < 50");
      assertTrue("We expect the alerts to be splitted up", a.getAttachment().contains("- Unit tests errors > 0"));
      assertTrue("We expect the alerts to be splitted up", a.getAttachment().contains("- Skipped unit tests > 0"));
   }

   @Test
   public void testResolveJobNameNoParameterization() throws Exception {
      SonarQubeSlackPusher ssp = new SonarQubeSlackPusher("", "http://sonar.company.org:9000", "J o b", "B r a n c h", "ac", "un", "pw");
      Method resolve = ssp.getClass().getDeclaredMethod("resolveJobName");
      resolve.setAccessible(true);

      assertEquals("Job name and branch name", "J o b B r a n c h", resolve.invoke(ssp));
   }

   @Test
   public void testResolveJobNameBranchBlank() throws Exception {
      SonarQubeSlackPusher ssp = new SonarQubeSlackPusher("", "http://sonar.company.org:9000", "J o b", "", "ac", "un", "pw");
      Method resolve = ssp.getClass().getDeclaredMethod("resolveJobName");
      resolve.setAccessible(true);

      assertEquals("Job name and branch name", "J o b", resolve.invoke(ssp));
   }

   @Test
   public void testResolveJobNameJobParameterizedBranchEmpty() throws Exception {
      SonarQubeSlackPusher ssp = new SonarQubeSlackPusher("", "http://sonar.company.org:9000", "ToBeResolved", "", "ac", "un", "pw");
      Method resolve = ssp.getClass().getDeclaredMethod("resolveJobName");
      resolve.setAccessible(true);

      Field resolvedJob = ssp.getClass().getDeclaredField("resolvedJobName");
      resolvedJob.setAccessible(true);
      resolvedJob.set(ssp, "J o b");

      assertEquals("Job name and branch name", "J o b", resolve.invoke(ssp));
   }

   @Test
   public void testResolveJobNameJobParameterized() throws Exception {
      SonarQubeSlackPusher ssp = new SonarQubeSlackPusher("", "http://sonar.company.org:9000", "ToBeResolved", "B r a n c h", "ac", "un", "pw");
      Method resolve = ssp.getClass().getDeclaredMethod("resolveJobName");
      resolve.setAccessible(true);

      Field resolvedJob = ssp.getClass().getDeclaredField("resolvedJobName");
      resolvedJob.setAccessible(true);
      resolvedJob.set(ssp, "J o b");

      assertEquals("Job name and branch name", "J o b B r a n c h", resolve.invoke(ssp));
   }

   @Test
   public void testResolveJobNameBranchParameterized() throws Exception {
      SonarQubeSlackPusher ssp = new SonarQubeSlackPusher("", "http://sonar.company.org:9000", "J o b", "ToBeResolved", "ac", "un", "pw");
      Method resolve = ssp.getClass().getDeclaredMethod("resolveJobName");
      resolve.setAccessible(true);

      Field resolvedBranch = ssp.getClass().getDeclaredField("resolvedBranchName");
      resolvedBranch.setAccessible(true);
      resolvedBranch.set(ssp, "B r a n c h");

      assertEquals("Job name and branch name", "J o b B r a n c h", resolve.invoke(ssp));
   }

   @Test
   public void testResolveJobNameJobAndBranchParameterized() throws Exception {
      SonarQubeSlackPusher ssp = new SonarQubeSlackPusher("", "http://sonar.company.org:9000", "ToBeResolvedJob", "ToBeResolvedBranch", "ac", "un", "pw");
      Method resolve = ssp.getClass().getDeclaredMethod("resolveJobName");
      resolve.setAccessible(true);

      Field resolvedJob = ssp.getClass().getDeclaredField("resolvedJobName");
      resolvedJob.setAccessible(true);
      resolvedJob.set(ssp, "J o b");
      Field resolvedBranch = ssp.getClass().getDeclaredField("resolvedBranchName");
      resolvedBranch.setAccessible(true);
      resolvedBranch.set(ssp, "B r a n c h");
      resolvedBranch.set(ssp, "B r a n c h");

      assertEquals("Job name and branch name", "J o b B r a n c h", resolve.invoke(ssp));
   }
}

