package Main;

import Domein.Controller;
import Util.Utils;
import jade.content.onto.Ontology;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.*;

public class Start {

    private static final String PLATFORM_IP = "127.0.0.1";
    private static final int PLATFORM_PORT=8888;
    private static final String PLATFORM_ID="ScrumMAS";
    private static Utils utils = new Utils();

    public static void main(String[] args) {
        Runtime rt = Runtime.instance();

        //Main container
        Profile pMain = new ProfileImpl(PLATFORM_IP, PLATFORM_PORT, PLATFORM_ID);
        System.out.println("Launching a main-container..."+pMain);
        AgentContainer mainContainerRef = rt.createMainContainer(pMain);

        //Own container
        String containerName;
        ProfileImpl pContainer;

        containerName = "ScrumMAS";
        pContainer = new ProfileImpl(PLATFORM_IP, PLATFORM_PORT, PLATFORM_ID);
        pContainer.setParameter(Profile.CONTAINER_NAME, containerName);
        System.out.println("Launching container " + containerName);
        AgentContainer containerRef = rt.createAgentContainer(pContainer);

        //Monitoring Agent
        createMonitoringAgents(mainContainerRef);

        System.out.println("platform OK");

        //Controller Agent
        String controllerName = "controller";
        String className = "Domein.Controller";

        utils.createOneAgent(containerRef, controllerName, className);
    }

    private static void createMonitoringAgents(ContainerController mc) {

        System.out.println("Launching the rma agent on the main container ...");
        AgentController rma;

        try {
            rma = mc.createNewAgent("rma", "jade.tools.rma.rma", new Object[0]);
            rma.start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
            System.out.println("Launching of rma agent failed");
        }

        System.out.println("Launching  Sniffer agent on the main container...");
        AgentController snif=null;

        try {
            snif= mc.createNewAgent("sniffeur", "jade.tools.sniffer.Sniffer",new Object[0]);
            snif.start();

        } catch (StaleProxyException e) {
            e.printStackTrace();
            System.out.println("launching of sniffer agent failed");

        }
    }



}
