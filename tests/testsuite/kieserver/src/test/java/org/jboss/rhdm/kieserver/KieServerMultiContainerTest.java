package org.jboss.rhdm.kieserver;

import org.arquillian.cube.openshift.api.OpenShiftResource;
import org.arquillian.cube.openshift.api.Template;
import org.arquillian.cube.openshift.api.TemplateParameter;
import org.arquillian.cube.openshift.api.Templates;
import org.arquillian.cube.openshift.impl.enricher.RouteURL;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

@RunWith(Arquillian.class)
@Templates(
        //syncInstantiation = false,
        templates = {
                @Template(url = "https://raw.githubusercontent.com/${template.repository:jboss-container-images}/rhdm-7-openshift-image/${template.branch:rhdm70-dev}/templates/rhdm70-kieserver-s2i.yaml",
                        parameters = {
                                @TemplateParameter(name = "KIE_SERVER_USER", value = "${kie.username:kieserver}"),
                                @TemplateParameter(name = "KIE_SERVER_PWD", value = "${kie.password:Redhat@123}"),
                                @TemplateParameter(name = "APPLICATION_NAME", value = "kie-app"),
                                @TemplateParameter(name = "KIE_SERVER_CONTAINER_DEPLOYMENT",
                                        value="decisionserver-hellorules=org.openshift.quickstarts:decisionserver-hellorules:1.4.0.Final" +
                                        "|" +
                                        "another-decisionserver-hellorules=org.openshift.quickstarts:decisionserver-hellorules:1.4.0.Final")
//                                @TemplateParameter(name = "MAVEN_MIRROR_URL", value = "http://nexus.severinocloud.com/nexus/content/groups/public/")
                        })
        })
@OpenShiftResource("https://raw.githubusercontent.com/${template.repository:jboss-container-images}/rhdm-7-openshift-image/${template.branch:rhdm70-dev}/kieserver-app-secret.yaml")
public class KieServerMultiContainerTest extends KieServerTestBase {

    @RouteURL("kie-app-kieserver")
    private URL routeURL;

    @Test
    @RunAsClient
    @InSequence(1)
    public void testRHDMKieserverContainer() throws MalformedURLException {
        // containerId is comming from here: https://github.com/jboss-container-images/rhdm-7-openshift-image/blob/rhdm70-dev/templates/rhdm70-kieserver-s2i.yaml#L154
        checkKieServerContainer(routeURL, Arrays.asList("decisionserver-hellorules", "another-decisionserver-hellorules"));
    }

    @Test
    @RunAsClient
    @InSequence(2)
    public void testRHDMKieserverFireAllRules() throws MalformedURLException {
        // containerId is comming from here: https://github.com/jboss-container-images/rhdm-7-openshift-image/blob/rhdm70-dev/templates/rhdm70-kieserver-s2i.yaml#L154
        fireAllRulesRest(routeURL, Arrays.asList("decisionserver-hellorules", "another-decisionserver-hellorules"));
    }

}
