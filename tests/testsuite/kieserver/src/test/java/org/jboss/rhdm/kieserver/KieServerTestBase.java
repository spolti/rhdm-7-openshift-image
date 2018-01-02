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

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.openshift.quickstarts.decisionserver.hellorules.Person;

import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.logging.Logger;

public class KieServerTestBase {

    private Logger log = Logger.getLogger(MethodHandles.lookup().getClass().getName());

    protected void prepareClientInvocation() {
        // do nothing in basic
    }

    protected static WebArchive getDeploymentInternal() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "run-in-pod.war");
        war.setWebXML("web.xml");
        war.addClass(KieServerTestBase.class);
        //war.addClass(DecisionServerTestBase.class);
        war.addPackage(Person.class.getPackage());
        //war.addAsLibraries(Libraries.transitive("org.kie.server", "kie-server-client"));
        //war.addAsLibraries(Libraries.transitive("org.arquillian.cube", "arquillian-cube-openshift-httpclient"));
//        Files.PropertiesHandle handle = Files.createPropertiesHandle(FILENAME);
//        handle.addProperty("kie.username", KIE_USERNAME);
//        handle.addProperty("kie.password", KIE_PASSWORD);
//        handle.addProperty("mq.username", MQ_USERNAME);
//        handle.addProperty("mq.password", MQ_PASSWORD);
//        handle.store(war);

        return war;
    }


    public void checkKieServerCapabilities(URL serverAddress, String cap) {
        log.info("Running test checkKieServerCapabilities");
        // for untrusted connections
        prepareClientInvocation();

        KieServicesClient kieServicesClient = getKieServicesClient(serverAddress);

    }

    private KieServicesClient getKieServicesClient(URL serverAddress, Map<String, String> credentials) {
        try {
            KieServicesConfiguration kieServicesCnf = null;
            kieServicesCnf = KieServicesFactory.newRestConfiguration(new URL(serverAddress,
                    "/kie-server/services/rest/server").toString(), KIE_USERNAME, KIE_PASSWORD);
            kieServicesCnf.setMarshallingFormat(MarshallingFormat.XSTREAM);
            return KieServicesFactory.newKieServicesClient(kieServicesCnf);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }


}