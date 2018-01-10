/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other
 * contributors as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.rhdm.kieserver;

import org.drools.core.command.assertion.AssertEquals;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.kie.api.command.BatchExecutionCommand;
import org.kie.api.command.Command;
import org.kie.api.runtime.ExecutionResults;
import org.kie.api.runtime.rule.QueryResults;
import org.kie.api.runtime.rule.QueryResultsRow;
import org.kie.internal.command.CommandFactory;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieContainerStatus;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.kie.server.client.RuleServicesClient;
import org.openshift.quickstarts.decisionserver.hellorules.Greeting;
import org.openshift.quickstarts.decisionserver.hellorules.Person;


import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

public class KieServerTestBase {

    private Logger log = Logger.getLogger(MethodHandles.lookup().getClass().getName());

    /**
     * Sort the kieContainers list in alphabetical order
     * To sort the list just add the following in the desired method:
     * Collections.sort(KieContainersList, ALPHABETICAL_ORDER);
     */
    public static final Comparator<KieContainerResource> ALPHABETICAL_ORDER =
            Comparator.comparing(KieContainerResource::getContainerId);

    protected void prepareClientInvocation() {
        // do nothing in basic, TODO https templates
    }

    protected static WebArchive getDeploymentInternal() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "run-in-pod.war");
        war.setWebXML("web.xml");
        war.addClass(KieServerTestBase.class);
        //war.addClass(DecisionServerTestBase.class);
        war.addPackage(Person.class.getPackage());
        //war.addAsLibraries(Libraries.transitive("org.kie.server", "kie-server-client"));
//        Files.PropertiesHandle handle = Files.createPropertiesHandle(FILENAME);
//        handle.addProperty("kie.username", KIE_USERNAME);
//        handle.addProperty("kie.password", KIE_PASSWORD);
//        handle.addProperty("mq.username", MQ_USERNAME);
//        handle.addProperty("mq.password", MQ_PASSWORD);
//        handle.store(war);

        return war;
    }


    public void checkKieServerCapabilities(URL serverAddress, List<String> capabilities) throws MalformedURLException {
        log.info("Running test checkKieServerCapabilities");
        // for untrusted connections
        prepareClientInvocation();
        List<String> serverCapabilities = getKieServicesClient(serverAddress).getServerInfo().getResult().getCapabilities();
        Collections.sort(serverCapabilities);
        Collections.sort(capabilities);
        Assert.assertEquals(capabilities, serverCapabilities);
    }

    public void checkKieServerContainer(URL serverAddress, List<String> containersId) throws MalformedURLException {
        log.info("Running test checkKieServerContainer");

        List<KieContainerResource> containers = getKieServicesClient(serverAddress).listContainers().getResult().getContainers();
        Collections.sort(containers, ALPHABETICAL_ORDER);
        Collections.sort(containersId);

        Assert.assertEquals(containersId.size(), containers.size());
        for (int i = 0; i < containers.size(); i++) {
            // The position for the arrays should be the same after sorted.
            Assert.assertTrue(containersId.get(i).equals(containers.get(i).getContainerId()));
            Assert.assertEquals(KieContainerStatus.STARTED, containers.get(i).getStatus());
        }
    }

    public void fireAllRulesRest(URL serverAddress, List<String> containersId) throws MalformedURLException {
        log.info("Running test fireAllRulesRest");
        // for untrusted connections
        prepareClientInvocation();
        String personName = "Filippe Spolti";
        RuleServicesClient client = getKieServicesClient(serverAddress).getServicesClient(RuleServicesClient.class);
        List<Command<?>> commands = new ArrayList<>();
        commands.add((Command<?>) CommandFactory.newInsert(new Person(personName)));
        commands.add((Command<?>) CommandFactory.newFireAllRules());
        commands.add((Command<?>) CommandFactory.newQuery("greetings", "get greeting"));
        BatchExecutionCommand command = CommandFactory.newBatchExecution(commands, "HelloRulesSession");

        containersId.stream().forEach(container -> {

            ServiceResponse<ExecutionResults> response = client.executeCommandsWithResults(container,  command);

            Assert.assertEquals(ServiceResponse.ResponseType.SUCCESS, response.getType());
            Assert.assertEquals("Container " + container + " successfully called.", response.getMsg());

            QueryResults queryResults = (QueryResults) response.getResult().getValue("greetings");
            Greeting greeting = new Greeting();
            for (QueryResultsRow queryResult : queryResults) {
                greeting = (Greeting) queryResult.get("greeting");
                System.out.println("Result: " + greeting.getSalutation());
            }

            Assert.assertEquals("Hello " + personName + "!", greeting.getSalutation());
        });


    }

    private KieServicesClient getKieServicesClient(URL serverAddress) throws MalformedURLException {
            KieServicesConfiguration kieServicesCnf = KieServicesFactory.newRestConfiguration(new URL(serverAddress,
                    "services/rest/server").toString(), "kieserver", "Redhat@123");
            kieServicesCnf.setMarshallingFormat(MarshallingFormat.XSTREAM);
            return KieServicesFactory.newKieServicesClient(kieServicesCnf);
    }

//    private Object getServiceClient(Class clientType, KieServicesClient kieServicesClient) {
//        return kieServicesClient.getServicesClient(clientType);
//    }
}