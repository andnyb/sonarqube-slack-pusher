package org.jenkinsci.plugins.sonarqubeslackpusher.it;

import static org.junit.Assert.assertTrue;

import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.sonarqubeslackpusher.SonarQubeSlackPusher;
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

public class SonarQubeSlackPusherIT {

   @Rule
   public MockServerRule mockServerRule = new MockServerRule(this, 9999);

   private MockServerClient mockServerClient;

   @Test
   public void testSonarHook() throws Exception {
      String body = FileUtils.readFileToString(new File(this.getClass().getResource("/single-project-with-failed-gates.json").getFile()));
      mockServerClient
         .when(HttpRequest.request("/api/qualitygates/project_status"))
         .respond(
            HttpResponse.response("")
               .withStatusCode(200)
               .withBody(body)
         );

      SonarQubeSlackPusher ssp = new SonarQubeSlackPusher("", "http://localhost:9999", "fun-service", "", "", "bullen", "passwd");

      // Disable Jenkins provided logging
      Field logger = ssp.getClass().getDeclaredField("logger");
      logger.setAccessible(true);
      logger.set(ssp, new PrintStream(System.out));

      Method getSonarData = ssp.getClass().getDeclaredMethod("getSonarQubeData");
      getSonarData.setAccessible(true);
      String s = (String)getSonarData.invoke(ssp);

      // Part 2
      Method getAllNotifications = ssp.getClass().getDeclaredMethod("getAllNotifications", String.class);
      getAllNotifications.setAccessible(true);
      getAllNotifications.invoke(ssp, s);

      Field attachment = ssp.getClass().getDeclaredField("attachment");
      attachment.setAccessible(true);
      assertTrue("Checking the attachment", attachment.get(ssp)!=null);
   }

   @Test
   public void testSonarHook_example_2() throws Exception {
      String body = FileUtils.readFileToString(new File(this.getClass().getResource("/single-project-example-2.json").getFile()));
      mockServerClient
         .when(HttpRequest.request("/api/qualitygates/project_status"))
         .respond(
            HttpResponse.response("")
               .withStatusCode(200)
               .withBody(body)
         );

      SonarQubeSlackPusher ssp = new SonarQubeSlackPusher("", "http://localhost:9999", "fun-service", "", "", "bullen", "passwd");

      // Disable Jenkins provided logging
      Field logger = ssp.getClass().getDeclaredField("logger");
      logger.setAccessible(true);
      logger.set(ssp, new PrintStream(System.out));

      Method getSonarData = ssp.getClass().getDeclaredMethod("getSonarQubeData");
      getSonarData.setAccessible(true);
      String s = (String)getSonarData.invoke(ssp);

      // Part 2
      Method getAllNotifications = ssp.getClass().getDeclaredMethod("getAllNotifications", String.class);
      getAllNotifications.setAccessible(true);
      getAllNotifications.invoke(ssp, s);

      Field attachment = ssp.getClass().getDeclaredField("attachment");
      attachment.setAccessible(true);
      assertTrue("Checking the attachment", attachment.get(ssp)!=null);
   }
}

