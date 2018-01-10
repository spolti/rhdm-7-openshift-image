package org.jboss.app;

import org.drools.core.runtime.rule.impl.FlatQueryResults;
import org.kie.api.command.BatchExecutionCommand;
import org.kie.api.command.Command;
import org.kie.api.runtime.ExecutionResults;
import org.kie.api.runtime.rule.QueryResults;
import org.kie.api.runtime.rule.QueryResultsRow;
import org.kie.internal.command.CommandFactory;
import org.kie.server.api.marshalling.Marshaller;
import org.kie.server.api.marshalling.MarshallerFactory;
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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class App {

    private static HashMap<String, KieServicesClient> client = new HashMap<>();

    /**
     * Sort the kieContainers list in alphabetical order
     * To sort the list just add the following in the desired method:
     * Collections.sort(KieContainersList, ALPHABETICAL_ORDER);
     */
    public static final Comparator<KieContainerResource> ALPHABETICAL_ORDER =
            Comparator.comparing(KieContainerResource::getContainerId);

    public static void main(String[] args) throws MalformedURLException {

        KieServicesConfiguration kieServicesCnf = KieServicesFactory
                .newRestConfiguration(new URL("http://myapp-kieserver-rhdm.severinocloud.com/services/rest/server")
                        .toString(), "executionUser", "MnoCda1!");
        kieServicesCnf.setMarshallingFormat(MarshallingFormat.XSTREAM);
        KieServicesClient kieServicesClient = KieServicesFactory.newKieServicesClient(kieServicesCnf);

        System.out.println("capabilities " + kieServicesClient.getServerInfo().getResult().getCapabilities());


        List<String> containersId = Arrays.asList("decisionserver-hellorules");

        List<KieContainerResource> containers = kieServicesClient.listContainers().getResult().getContainers();
        Collections.sort(containers, ALPHABETICAL_ORDER);



        System.out.println("SIZE: " + containers.size());
        for (int i = 0; i < containers.size(); i++) {
            // The position for the arrays should be the same after sorted.
            System.out.println(containersId.get(i) + " - " + (containers.get(i).getContainerId()));
            System.out.println(KieContainerStatus.STARTED + " - " + containers.get(i).getStatus());
        }




        Set<Class<?>> classes = new HashSet<>();
        classes.add(Person.class);
        classes.add(Greeting.class);
        Marshaller marshaller = MarshallerFactory.getMarshaller(
                new HashSet<Class<?>>(classes), MarshallingFormat.XSTREAM, Person.class.getClassLoader());




        RuleServicesClient client = kieServicesClient.getServicesClient(RuleServicesClient.class);

        Person person = new Person();
        person.setName("Filippe Spolti");
        List<Command<?>> commands = new ArrayList<>();
        commands.add((Command<?>) CommandFactory.newInsert(person));
        commands.add((Command<?>) CommandFactory.newFireAllRules());
        commands.add((Command<?>) CommandFactory.newQuery("greetings", "get greeting"));
        BatchExecutionCommand command = CommandFactory.newBatchExecution(commands, "HelloRulesSession");

        String marshalledCommands = marshaller.marshall(commands);


        ServiceResponse<ExecutionResults> response = client.executeCommandsWithResults("decisionserver-hellorules",   command);

        ExecutionResults results = response.getResult();
        System.out.println(response.toString());

        System.out.println(response.getMsg());
        System.out.println(response.getType());
        System.out.println(response.getResult());


        FlatQueryResults queryResults = (FlatQueryResults) response.getResult().getValue("greetings");



        Greeting greeting = new Greeting();
        for (QueryResultsRow queryResult : queryResults) {
            greeting = (Greeting) queryResult.get("greeting");
            System.out.println("Result: " + greeting.getSalutation());
        }




    }

    private static KieServicesClient getKieServicesClient(URL serverAddress) {
        try {

            for (Map.Entry<String, KieServicesClient> entry : client.entrySet()) {
                System.out.println("KEY: " + entry.getKey() + " VaLUE: " + entry.getValue());
            }

            System.out.println("client :" + client.containsKey(serverAddress.getHost()) + " size " + client.size());
            if (client.containsKey(serverAddress.getHost())) {
                System.out.println("conf for " + serverAddress + " already exist.");
                return client.get(serverAddress);
            } else {
                System.out.println("kieServices empty creating a new one " + serverAddress);
                KieServicesConfiguration kieServicesCnf = KieServicesFactory.newRestConfiguration(new URL(serverAddress,
                        "services/rest/server").toString(), "executionUser", "MnoCda1!");
                kieServicesCnf.setMarshallingFormat(MarshallingFormat.XSTREAM);
                client.put(serverAddress.getHost(), KieServicesFactory.newKieServicesClient(kieServicesCnf));
                return client.get(serverAddress.getHost());
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
