/*
 *   Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.carbon.identity.agent.outbound.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.agent.outbound.server.dao.AgentMgtDao;
import org.wso2.carbon.identity.agent.outbound.server.messaging.JMSMessageReceiver;
import org.wso2.msf4j.MicroservicesRunner;
import org.wso2.msf4j.websocket.exception.WebSocketEndpointAnnotationException;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Application main class which initialize listening JMS message and deploy websocket endpoint.
 */
public class Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);
    private Thread shutdownHook;

    public static void main(String[] args) throws WebSocketEndpointAnnotationException, UnknownHostException {
        LOGGER.info("Starting socket server.");
        Application application = new Application();
        application.startApplication();
    }

    private void startApplication() throws UnknownHostException {
        String serverNode = InetAddress.getLocalHost().getHostAddress();
        SessionHandler serverHandler = new SessionHandler();
        JMSMessageReceiver receiver = new JMSMessageReceiver(serverHandler);
        receiver.start();
        addShutdownHook(serverNode);
        new MicroservicesRunner().deployWebSocketEndpoint(new UserStoreServerEndpoint(serverHandler, serverNode))
                .start();
    }

    private void addShutdownHook(String serverNode) {
        if (shutdownHook != null) {
            return;
        }
        shutdownHook = new Thread() {

            public void run() {
                LOGGER.info("Shutdown hook triggered.");
                shutdownGracefully(serverNode);
            }
        };
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    private void shutdownGracefully(String serverNode) {
        AgentMgtDao agentMgtDao = new AgentMgtDao();
        agentMgtDao.closeAllConnection(serverNode);
        LOGGER.info("Shutting down server node " + serverNode);
    }
}
