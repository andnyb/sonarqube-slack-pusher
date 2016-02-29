package org.jenkinsci.plugins.sonarslackpusher.it;

import static org.junit.Assert.assertTrue;

import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.sonarslackpusher.SonarSlackPusher;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class SonarSlackPusherIT {

   @Rule
   public MockServerRule mockServerRule = new MockServerRule(this, 9999);

   private MockServerClient mockServerClient;

   @Test
   @Ignore
   public void testSonarHook() throws Exception {
      String body = FileUtils.readFileToString(new File(this.getClass().getResource("/single-project-with-failed-gates.json").getFile()));
      mockServerClient
         .when(HttpRequest.request("/api/resources"))
         .respond(
            HttpResponse.response("")
               .withStatusCode(200)
               .withBody(body)
         );

      SonarSlackPusher ssp = new SonarSlackPusher("", "http://localhost:9999", "fun-service", "", "", "bullen", "passwd");

      // Disable Jenkins provided logging
      Field logger = ssp.getClass().getDeclaredField("logger");
      logger.setAccessible(true);
      logger.set(ssp, new PrintStream(System.out));

      Method getSonarData = ssp.getClass().getDeclaredMethod("getSonarData");
      getSonarData.setAccessible(true);
      String s = (String)getSonarData.invoke(ssp);
      assertTrue("Looking for fun project:", s.contains("fun-service"));

      // Part 2
      Method getAllNotifications = ssp.getClass().getDeclaredMethod("getAllNotifications", String.class);
      getAllNotifications.setAccessible(true);
      getAllNotifications.invoke(ssp, s);

      Field attachment = ssp.getClass().getDeclaredField("attachment");
      attachment.setAccessible(true);
      assertTrue("Checking the attachment", attachment.get(ssp)!=null);
   }
}

