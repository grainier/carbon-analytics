/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.analytics.test.osgi;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.ExamFactory;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.testng.listener.PaxExam;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.wso2.carbon.analytics.test.osgi.util.HTTPResponseMessage;
import org.wso2.carbon.analytics.test.osgi.util.TestUtil;
import org.wso2.carbon.container.CarbonContainerFactory;
import org.wso2.carbon.container.options.CarbonDistributionOption;
import org.wso2.carbon.kernel.CarbonServerInfo;

import java.net.URI;
import java.nio.file.Paths;
import javax.inject.Inject;

import static org.ops4j.pax.exam.CoreOptions.systemProperty;

/**
 * SiddhiAsAPI OSGI Tests.
 */

@Listeners(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@ExamFactory(CarbonContainerFactory.class)
public class SiddhiAsAPITestcase {
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SiddhiAsAPITestcase.class);

    private static final String DEFAULT_USER_NAME = "admin";
    private static final String DEFAULT_PASSWORD = "admin";

    @Inject
    private CarbonServerInfo carbonServerInfo;

    @Configuration
    public Option[] createConfiguration() {
        return new Option[0];
    }

    /*
    Siddhi App deployment related test cases
     */

    @Test
    public void testValidSiddhiAPPDeployment() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps";
        String contentType = "text/plain";
        String method = "POST";
        String body = "@App:name('SiddhiApp1')\n" +
                "define stream FooStream (symbol string, price float, volume long);\n" +
                "\n" +
                "@source(type='inMemory', topic='symbol', @map(type='passThrough'))Define stream BarStream " +
                "(symbol string, price float, volume long);\n" +
                "\n" +
                "from FooStream\n" +
                "select symbol, price, volume\n" +
                "insert into BarStream;";

        logger.info("Deploying valid Siddhi App through REST API");
        HTTPResponseMessage httpResponseMessage = TestUtil.sendHRequest(body, baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 201);
        Assert.assertEquals(httpResponseMessage.getContentType(), "application/json");

        Thread.sleep(10000);
    }

    @Test(dependsOnMethods = {"testValidSiddhiAPPDeployment"})
    public void testValidDuplicateSiddhiAPPDeployment() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps";
        String contentType = "text/plain";
        String method = "POST";
        String body = "@App:name('SiddhiApp1')\n" +
                "define stream FooStream (symbol string, price float, volume long);\n" +
                "\n" +
                "@source(type='inMemory', topic='symbol', @map(type='passThrough'))Define stream BarStream " +
                "(symbol string, price float, volume long);\n" +
                "\n" +
                "from FooStream\n" +
                "select symbol, price, volume\n" +
                "insert into BarStream;";

        logger.info("Deploying valid Siddhi App whih is already existing in server through REST API");
        HTTPResponseMessage httpResponseMessage = TestUtil.sendHRequest(body, baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 409);
    }

    @Test(dependsOnMethods = {"testValidDuplicateSiddhiAPPDeployment"})
    public void testInValidSiddhiAPPDeployment() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps";
        String contentType = "text/plain";
        String method = "POST";

        String invalidBody = "@App:name('SiddhiApp2')\n" +
                "define stream FooStream (symbol string, price float, volume long);\n" +
                "\n" +
                "@source(type='inMemory', topic='symbol', @map(type='passThrough'))Define stream BarStream " +
                "(symbol string, price float, volume long);\n" +
                "\n" +
                "from FooStream\n" +
                "select symbol, price, volume\n" +
                "";
        logger.info("Deploying invalid Siddhi App through REST API");
        HTTPResponseMessage httpResponseMessage = TestUtil.sendHRequest(invalidBody, baseURI, path, contentType,
                method, true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 400);

    }

    @Test(dependsOnMethods = {"testInValidSiddhiAPPDeployment"})
    public void testSiddhiAPPDeploymentWithInvalidContentType() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps";
        String contentType = "application/json";
        String method = "POST";

        String invalidBody = "@App:name('SiddhiApp2')\n" +
                "define stream FooStream (symbol string, price float, volume long);\n" +
                "\n" +
                "@source(type='inMemory', topic='symbol', @map(type='passThrough'))Define stream BarStream " +
                "(symbol string, price float, volume long);\n" +
                "\n" +
                "from FooStream\n" +
                "select symbol, price, volume\n" +
                "";

        logger.info("Deploying Siddhi App with invalid content type through REST API");
        HTTPResponseMessage httpResponseMessage = TestUtil.sendHRequest(invalidBody, baseURI, path, contentType,
                method, true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 415);

    }

    @Test(dependsOnMethods = {"testSiddhiAPPDeploymentWithInvalidContentType"})
    public void testSiddhiAPPDeploymentWithNoBody() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps";
        String contentType = "text/plain";
        String method = "POST";

        logger.info("Deploying Siddhi App without request body through REST API");
        HTTPResponseMessage httpResponseMessage = TestUtil.sendHRequest("", baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 400);
    }


    /*
    Siddhi App update related test cases
     */

    @Test(dependsOnMethods = {"testSiddhiAPPDeploymentWithNoBody"})
    public void testValidNonExistSiddhiAPPUpdate() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps";
        String contentType = "text/plain";
        String method = "PUT";
        String body = "@App:name('SiddhiApp3')\n" +
                "define stream FooStream (symbol string, price float, volume long);\n" +
                "\n" +
                "@source(type='inMemory', topic='symbol', @map(type='passThrough'))Define stream BarStream " +
                "(symbol string, price float, volume long);\n" +
                "\n" +
                "from FooStream\n" +
                "select symbol, price, volume\n" +
                "insert into BarStream;";

        logger.info("Deploying valid Siddhi App which does not exists through REST API");
        HTTPResponseMessage httpResponseMessage = TestUtil.sendHRequest(body, baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 201);

        Thread.sleep(10000);
    }

    @Test(dependsOnMethods = {"testValidNonExistSiddhiAPPUpdate"})
    public void testValidAlreadyExistSiddhiAPPUpdate() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps";
        String contentType = "text/plain";
        String method = "PUT";
        String body = "@App:name('SiddhiApp3')\n" +
                "define stream FooStream (symbol string, price float, volume long);\n" +
                "\n" +
                "@source(type='inMemory', topic='symbol', @map(type='passThrough'))Define stream BarStream " +
                "(symbol string, price float, volume long);\n" +
                "\n" +
                "from FooStream\n" +
                "select symbol, price, volume\n" +
                "insert into BarStream;";

        logger.info("Deploying valid Siddhi App whih is already existing in server through REST API");
        HTTPResponseMessage httpResponseMessage = TestUtil.sendHRequest(body, baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 200);

        Thread.sleep(10000);
    }

    @Test(dependsOnMethods = {"testValidAlreadyExistSiddhiAPPUpdate"})
    public void testInValidSiddhiAPPUpdate() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps";
        String contentType = "text/plain";
        String method = "PUT";

        String invalidBody = "@App:name('SiddhiApp3')\n" +
                "define stream FooStream (symbol string, price float, volume long);\n" +
                "\n" +
                "@source(type='inMemory', topic='symbol', @map(type='passThrough'))Define stream BarStream " +
                "(symbol string, price float, volume long);\n" +
                "\n" +
                "from FooStream\n" +
                "select symbol, price, volume\n" +
                "";
        logger.info("Deploying invalid Siddhi App through REST API");
        HTTPResponseMessage httpResponseMessage = TestUtil.sendHRequest(invalidBody, baseURI, path, contentType,
                method, true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 400);
    }

    @Test(dependsOnMethods = {"testInValidSiddhiAPPUpdate"})
    public void testSiddhiAPPUpdateWithInvalidContentType() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps";
        String contentType = "application/json";
        String method = "PUT";

        String invalidBody = "@App:name('SiddhiApp3')\n" +
                "define stream FooStream (symbol string, price float, volume long);\n" +
                "\n" +
                "@source(type='inMemory', topic='symbol', @map(type='passThrough'))Define stream BarStream " +
                "(symbol string, price float, volume long);\n" +
                "\n" +
                "from FooStream\n" +
                "select symbol, price, volume\n" +
                "";

        logger.info("Deploying Siddhi App with invalid content type through REST API");
        HTTPResponseMessage httpResponseMessage = TestUtil.sendHRequest(invalidBody, baseURI, path, contentType,
                method, true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 415);
    }

    /*
        Siddhi App retrieval (individual) related test cases
     */

    @Test(dependsOnMethods = {"testSiddhiAPPUpdateWithInvalidContentType"})
    public void testValidSiddhiAPPRetrieval() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/SiddhiApp1";
        String method = "GET";
        String contentType = "text/plain";

        logger.info("Retrieving active Siddhi App through REST API");
        HTTPResponseMessage httpResponseMessage = TestUtil.sendHRequest(" ", baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 200);
    }

    @Test(dependsOnMethods = {"testValidSiddhiAPPRetrieval"})
    public void testValidSiddhiAPPRetrievalWithDifferntContentType() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/SiddhiApp1";
        String method = "GET";
        String contentType = "application/json";

        logger.info("Retrieving active Siddhi App through REST API");
        HTTPResponseMessage httpResponseMessage = TestUtil.sendHRequest(" ", baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 200);
    }

    @Test(dependsOnMethods = {"testValidSiddhiAPPRetrievalWithDifferntContentType"})
    public void testNonExistSiddhiAPPRetrieval() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/SiddhiApp33";
        String method = "GET";
        String contentType = "text/plain";

        logger.info("Retrieving non exist Siddhi App through REST API");
        HTTPResponseMessage httpResponseMessage = TestUtil.sendHRequest(" ", baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 404);
    }

    @Test(dependsOnMethods = {"testValidSiddhiAPPRetrieval"})
    public void testInactiveSiddhiAPPRetrieval() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/TestInvalidSiddhiApp";
        String method = "GET";
        String contentType = "text/plain";

        logger.info("Retrieving inactive Siddhi App through REST API");
        HTTPResponseMessage httpResponseMessage = TestUtil.sendHRequest(" ", baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 200);
    }

    /*
        Siddhi App retrieval (collection) related test cases
     */

    @Test(dependsOnMethods = {"testInactiveSiddhiAPPRetrieval"})
    public void testAllSiddhiAPPRetrieval() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps";
        String method = "GET";

        logger.info("Retrieving all Siddhi App names through REST API");
        HTTPResponseMessage httpResponseMessage = TestUtil.sendHRequest(null, baseURI, path, null, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 200);
    }

    @Test(dependsOnMethods = {"testAllSiddhiAPPRetrieval"})
    public void testAllSiddhiAPPRetrievalWithContentType() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps";
        String method = "GET";
        String contentType = "application/json";

        logger.info("Retrieving all Siddhi App names through REST API (different content type)");
        HTTPResponseMessage httpResponseMessage = TestUtil.sendHRequest(null, baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 200);

    }

    /*
        Siddhi App status retrieval related test cases
     */

    @Test(dependsOnMethods = {"testAllSiddhiAPPRetrievalWithContentType"})
    public void testNonExistSiddhiAPPStatusRetrieval() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/SiddhiApp4/status";
        String method = "GET";
        String contentType = "text/plain";

        logger.info("Retrieving the status of a Siddhi App which not exists in server through REST API");
        HTTPResponseMessage httpResponseMessage = TestUtil.sendHRequest(null, baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 404);
    }

    @Test(dependsOnMethods = {"testNonExistSiddhiAPPStatusRetrieval"})
    public void testValidSiddhiAPPStatusRetrieval() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/SiddhiApp1/status";
        String method = "GET";

        logger.info("Retrieving the status of a Siddhi App which exists in server through REST API");
        HTTPResponseMessage httpResponseMessage = TestUtil.sendHRequest(null, baseURI, path, null, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 200);
    }

    @Test(dependsOnMethods = {"testValidSiddhiAPPStatusRetrieval"})
    public void testInactiveSiddhiAPPStatusRetrieval() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/TestInvalidSiddhiApp/status";
        String method = "GET";

        logger.info("Retrieving the status of a Siddhi inactive App which exists in server through REST API");
        HTTPResponseMessage httpResponseMessage = TestUtil.sendHRequest(null, baseURI, path, null, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 200);
    }

    @Test(dependsOnMethods = {"testInactiveSiddhiAPPStatusRetrieval"})
    public void testiddhiAPPStatusRetrievalWithDifferentContentType() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/SiddhiApp1/status";
        String method = "GET";
        String contentType = "application/json";

        logger.info("Retrieving the status of a Siddhi App which exists in server through REST API with different " +
                "content type");
        HTTPResponseMessage httpResponseMessage = TestUtil.sendHRequest(null, baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 200);
    }

    /*
        Siddhi App state backup related test cases
     */

    @Test(dependsOnMethods = {"testiddhiAPPStatusRetrievalWithDifferentContentType"})
    public void testValidSiddhiAPPBackup() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/SiddhiApp1/backup";
        String method = "POST";
        String contentType = "text/plain";

        logger.info("Taking snapshot of a Siddhi App that exists in server through REST API");
        HTTPResponseMessage httpResponseMessage = TestUtil.sendHRequest("", baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 201);

        Thread.sleep(2000);
    }

    @Test(dependsOnMethods = {"testValidSiddhiAPPBackup"})
    public void testNonExistsSiddhiAPPBackup() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/SiddhiApp2/backup";
        String method = "POST";
        String contentType = "text/plain";

        logger.info("Taking snapshot of a Siddhi App that does not exist in server through REST API");
        HTTPResponseMessage httpResponseMessage = TestUtil.sendHRequest("", baseURI, path, null, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 404);
    }

    @Test(dependsOnMethods = {"testNonExistsSiddhiAPPBackup"})
    public void testValidSiddhiAPPBackupTake2() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/SiddhiApp1/backup";
        String method = "POST";
        String contentType = "text/plain";

        logger.info("Taking snapshot again for a Siddhi App that exists in server through REST API");
        HTTPResponseMessage httpResponseMessage = TestUtil.sendHRequest("", baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 201);

        Thread.sleep(2000);
    }

    @Test(dependsOnMethods = {"testValidSiddhiAPPBackupTake2"})
    public void testSiddhiAPPBackupWithInvalidMethod() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/SiddhiApp1/backup";
        String method = "GET";
        String contentType = "text/plain";

        logger.info("Taking snapshot of a Siddhi App that exists in server through REST API by invoking with " +
                "invalid method");
        HTTPResponseMessage httpResponseMessage = TestUtil.sendHRequest("", baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 405);
    }

    /*
        Siddhi App state restore related test cases
     */

    @Test(dependsOnMethods = {"testSiddhiAPPBackupWithInvalidMethod"})
    public void testValidSiddhiAPPRestoreToLastRevision() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/SiddhiApp1/restore";
        String method = "POST";
        String contentType = "text/plain";

        logger.info("Restoring the snapshot (last revision) of a Siddhi App that exists in server through REST API");
        HTTPResponseMessage httpResponseMessage = TestUtil.sendHRequest("", baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 200);
    }

    @Test(dependsOnMethods = {"testValidSiddhiAPPRestoreToLastRevision"})
    public void testNonExistSiddhiAPPRestoreToLastRevision() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/SiddhiApp2/restore";
        String method = "POST";

        logger.info("Restoring the snapshot (last revision) of a Siddhi App that does not exist in " +
                "server through REST API");
        HTTPResponseMessage httpResponseMessage = TestUtil.sendHRequest("", baseURI, path, null, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 404);
    }

    @Test(dependsOnMethods = {"testNonExistSiddhiAPPRestoreToLastRevision"})
    public void testSiddhiAPPRestoreToNonExistRevision() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/SiddhiApp1/restore?revision=445534";
        String method = "POST";

        logger.info("Restoring the snapshot revison that does not exist of a Siddhi App through REST API");
        HTTPResponseMessage httpResponseMessage = TestUtil.sendHRequest("", baseURI, path, null, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 500);

    }

    @Test(dependsOnMethods = {"testSiddhiAPPRestoreToNonExistRevision"})
    public void testSiddhiAPPBackupWithInvalidContentType() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/SiddhiApp1/backup";
        String method = "GET";
        String contentType = "application/json";

        logger.info("Taking snapshot of a Siddhi App that exists in server through REST API by invoking with " +
                "invalid method");
        HTTPResponseMessage httpResponseMessage = TestUtil.sendHRequest("", baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 405);
    }

    /*
        Siddhi App deletion related test cases
     */

    @Test(dependsOnMethods = {"testSiddhiAPPBackupWithInvalidContentType"})
    public void testNonExistSiddhiAPPDeletion() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/SiddhiApp2";
        String method = "DELETE";
        String contentType = "text/plain";

        logger.info("Deleting Siddhi App which not exists in server through REST API");
        HTTPResponseMessage httpResponseMessage = TestUtil.sendHRequest(null, baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 404);
    }

    @Test(dependsOnMethods = {"testNonExistSiddhiAPPDeletion"})
    public void testValidSiddhiAPPDeletion() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/SiddhiApp1";
        String method = "DELETE";

        logger.info("Deleting valid Siddhi App which exists in server through REST API");
        HTTPResponseMessage httpResponseMessage = TestUtil.sendHRequest(null, baseURI, path, null, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 200);
        Thread.sleep(6000);

    }

    @Test(dependsOnMethods = {"testValidSiddhiAPPDeletion"})
    public void testValidSiddhiAPPDeletionWithoutAppName() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps";
        String method = "DELETE";

        logger.info("Deleting Siddhi App which without providing the app name in the url through REST API");
        HTTPResponseMessage httpResponseMessage = TestUtil.sendHRequest(null, baseURI, path, null, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 405);
    }

    @Test(dependsOnMethods = {"testValidSiddhiAPPDeletionWithoutAppName"})
    public void testInactiveSiddhiAPPDeletion() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/TestInvalidSiddhiApp";
        String method = "DELETE";
        String contentType = "application/json";

        logger.info("Deleting inactive Siddhi App which exists in server through REST API");
        HTTPResponseMessage httpResponseMessage = TestUtil.sendHRequest(null, baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 200);
        Thread.sleep(6000);
    }

    /*
        Siddhi App retrieval after deletion related test cases
     */
    @Test(dependsOnMethods = {"testInactiveSiddhiAPPDeletion"})
    public void testSiddhiAPPRetrievalAfterDeletion() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps";
        String method = "GET";
        String contentType = "text/plain";

        logger.info("Retrieving all Siddhi App names through REST API");
        HTTPResponseMessage httpResponseMessage = TestUtil.sendHRequest(null, baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 200);
    }

    /*
        Siddhi App without authentication
     */
    // TODO: 11/1/17 To enable after the Siddhi-apps API is secured
    @Test(enabled = false)
    public void testSiddhiAPPWithoutAuthentication() {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps";
        String method = "GET";
        String contentType = "text/plain";

        logger.info("Retrieving all Siddhi App names through REST API");
        HTTPResponseMessage httpResponseMessage = TestUtil.sendHRequest(null, baseURI, path, contentType, method,
                false, null, null);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 401);
    }

    /*
        Siddhi App with wrong credentials
     */
    @Test(enabled = false)
    public void testSiddhiAPPWrongCredentials() {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps";
        String method = "GET";
        String contentType = "text/plain";

        logger.info("Retrieving all Siddhi App names through REST API");
        HTTPResponseMessage httpResponseMessage = TestUtil.sendHRequest(null, baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, "admin2");
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 401);
    }

}
