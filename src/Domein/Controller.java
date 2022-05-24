package Domein;

import Util.Utils;

import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.core.ContainerID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.FIPANames;
import jade.domain.JADEAgentManagement.CreateAgent;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;
import jade.wrapper.ContainerController;

public class Controller extends Agent {

    private Utils utils = new Utils();
    public static final int nbDevAgents = 4;
    public static final String DevAgentName = "Dev agent";
    public static final String ProductOwnerAgent = "Product owner";
    public static final String ScrumBoardAgent = "Scrum Board";
    public static final String ScrumMasterAgent = "Scrum master";

    protected void setup() {
        super.setup();
        utils.pintAgentIntel(this);

        OneShotBehaviour create = new OneShotBehaviour(this){

            @Override
            public void action() {
//          Agent intel

//          Set up
                //Register the SL content language
                getContentManager().registerLanguage(new SLCodec(), FIPANames.ContentLanguage.FIPA_SL);
                //Register the mobility ontology
                getContentManager().registerOntology(JADEManagementOntology.getInstance());
                ContainerController container = this.getAgent().getContainerController();

                //Scrum board
                String nameSB = ScrumBoardAgent;
                String classNameSB = "Domein.ScrumBoardAgent";
                utils.createOneAgent(container, nameSB, classNameSB);

                //Scrum master Agent
                String nameSM = ScrumMasterAgent;
                String classNameSM = "Domein.ScrumMasterAgent";
                utils.createOneAgent(container,  nameSM, classNameSM);

                //Product Owner
                String namePO = ProductOwnerAgent;
                String classNamePO = "Domein.ProductOwnerAgent";
                utils.createOneAgent(container, namePO, classNamePO);

//          Start Agents
                for (int i = 1; i < nbDevAgents + 1; i++) {
                    String name = DevAgentName + i;
                    String className = "Domein.DeveloperAgent";
                    utils.createOneAgent(container, name, className);
                }
            }
        };
//      Behaviour set
        this.addBehaviour(create);
    }
}

