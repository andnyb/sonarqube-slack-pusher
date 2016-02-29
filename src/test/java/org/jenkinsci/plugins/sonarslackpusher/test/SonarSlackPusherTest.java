package org.jenkinsci.plugins.sonarslackpusher.test;

import org.jenkinsci.plugins.sonarslackpusher.Attachment;
import org.jenkinsci.plugins.sonarslackpusher.SonarSlackPusher;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SonarSlackPusherTest {

   @Test
   public void testReplaceWithParameters() throws Exception {
      SonarSlackPusher ssp = new SonarSlackPusher("", "", "http://sonar.company.org:9000", "", "", "", "");
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
}

