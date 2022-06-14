package Domein;

import Util.Utils;

import jade.content.lang.sl.SLCodec;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.FIPANames;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.wrapper.ContainerController;

public class Controller extends Agent {

    private Utils utils = new Utils();
    public static final int nbDevAgents = 4;
    public static final String devAgentName = "Dev agent";
    public static String[] devAgentNames = new String[nbDevAgents];
    public static AID[] devAgents = new AID[nbDevAgents];
    public static final String productOwnerAgent = "Product owner";
    public final static AID productOwnerAID = new AID(productOwnerAgent, AID.ISLOCALNAME);
    public static final String scrumBoardAgent = "Scrum Board";
    public final static AID scrumBoardAID = new AID(scrumBoardAgent, AID.ISLOCALNAME);
    public static final String scrumMasterAgent = "Scrum master";
    public final static AID scrumMasterAID = new AID(scrumMasterAgent, AID.ISLOCALNAME);

    protected void setup() {
        super.setup();
        utils.pintAgentIntel(this);

        OneShotBehaviour create = new OneShotBehaviour(this) {

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
                String nameSB = scrumBoardAgent;
                String classNameSB = "Domein.ScrumBoardAgent";
                utils.createOneAgent(container, nameSB, classNameSB);

                block(3000);

                //Product Owner
                String namePO = productOwnerAgent;
                String classNamePO = "Domein.ProductOwnerAgent";
                utils.createOneAgent(container, namePO, classNamePO);

                block(3000);

                //Scrum master Agent
                String nameSM = scrumMasterAgent;
                String classNameSM = "Domein.ScrumMasterAgent";
                utils.createOneAgent(container, nameSM, classNameSM);

                block(3000);

//          Start Agents
                for (int i = 1; i < nbDevAgents + 1; i++) {
                    String name = devAgentName + i;
                    String className = "Domein.DeveloperAgent";
                    utils.createOneAgent(container, name, className);
                    AID devAgent = new AID(devAgentName + i, AID.ISLOCALNAME);
                    devAgents[i - 1] = devAgent;
                }

                System.out.println("Agents created");
                System.out.println(devAgents);
            }
        };
//      Behaviour set
        this.addBehaviour(create);
    }

    public static AID[] getDevAgents() {
        return devAgents;
    }

    public void setDevAgents(AID[] devAgents) {
        this.devAgents = devAgents;
    }
}

