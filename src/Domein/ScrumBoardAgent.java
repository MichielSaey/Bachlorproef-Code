package Domein;

import Behaviour.MessageReceiver;
import Enums.State;
import Items.*;

import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.OntologyUtils;
import jade.core.AID;
import jade.core.Agent;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;

import Util.Utils;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class ScrumBoardAgent extends Agent {

    private final Utils utils = new Utils();
    private static final int nbStories = 10;
    private static final int nbTasks = 5;
    private int nbStoriesSent = 0;
    private int nbStoriesRecived = 0;
    private ArrayList<Story> stories = new ArrayList<>();

    private Ontology ontology = ScrumOntology.getInstance();
    private Codec codec = new SLCodec();

    public ScrumBoardAgent() throws BeanOntologyException {
    }


    protected void setup() {
//      Agent intel
        utils.pintAgentIntel(this);

        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(Controller.ScrumBoardAgent);
        sd.setName(getLocalName());
        dfd.addServices(sd);

//		Data
        listFillere();
        ScrumBoardStatus();

        //Repeater
        CyclicBehaviour running = new CyclicBehaviour(this) {
            @Override
            public void action() {

                int workLeft = 0;

                for (Story story:  stories) {
                    if (story.getState() == State.Product){
                        workLeft = workLeft + 1;
                    }
                }
                if (!(workLeft == nbStories)){
                    System.out.println("stories left: " + workLeft);
                }

                if (workLeft != 0) {
                    //Main
                    SequentialBehaviour behaviour = new SequentialBehaviour(myAgent);

                    //Sub
                    SimpleBehaviour msgR = new MessageReceiver();
                    SimpleBehaviour sbmh = new ScrumBoardMessageHandler(myAgent);

                    //DataStore
                    msgR.setDataStore(behaviour.getDataStore());
                    sbmh.setDataStore(behaviour.getDataStore());

                    //Add To Main
                    behaviour.addSubBehaviour(msgR);
                    behaviour.addSubBehaviour(sbmh);

                    //Run
                    addBehaviour(behaviour);
                } else {
                    SimpleBehaviour endSprint = new EndSprint(myAgent);
                    addBehaviour(endSprint);
                    System.out.println("Sprint finished");
                    this.block();
                }

            }
        };

        //Run
        addBehaviour(running);
    }

    public class ScrumBoardMessageHandler extends SimpleBehaviour {

        private ScrumBoardAgent mySBAgent;

        ScrumBoardMessageHandler(Agent agent) {
            super(agent);
            this.mySBAgent = (ScrumBoardAgent) agent;

        }


        @Override
        public void action() {
            ACLMessage msg = (ACLMessage) getDataStore().get(MessageReceiver.RECV_MSG);

            Story story;
            WorkOrder wo;

            switch (msg.getPerformative()) {
                case ACLMessage.REQUEST:
                    story = getFirstFreeStory(msg.getSender());

                    ACLMessage rmsg = msg.createReply();
                    rmsg.setPerformative(ACLMessage.AGREE);
                    rmsg.addReceiver(msg.getSender());
                    rmsg.setSender(myAgent.getAID());
                    rmsg.setLanguage(codec.getName());
                    rmsg.setOntology(ontology.getName());

                    if (story != null){
                        wo = new WorkOrder();
                        wo.setStory(story);
                        wo.setOwner(msg.getSender());
                        try {
                            getContentManager().fillContent(rmsg, wo);
                            System.out.println(rmsg);
                            send(rmsg);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        rmsg.setPerformative(ACLMessage.FAILURE);
                        send(rmsg);
                    }
                    break;
                case ACLMessage.CONFIRM:
                    try {
                        wo = (WorkOrder) getContentManager().extractContent(msg);
                        story = wo.getStory();
                        stories = utils.findAndReplace(story, stories);

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    break;
                default:
                    block();
                    break;
            }
        }

        @Override
        public boolean done() {
            return false;
        }
    }

    public class EndSprint extends SimpleBehaviour {

        public EndSprint(Agent a) {
            super(a);
        }

        @Override
        public void action() {
            ACLMessage msg = new ACLMessage(ACLMessage.CANCEL);

            for (int i = 1; i < Controller.nbDevAgents + 1; i++) {
                msg.addReceiver(new AID(Controller.DevAgentName + i, AID.ISLOCALNAME));
            }

            msg.setSender(myAgent.getAID());
            msg.setLanguage(codec.getName());
            msg.setOntology(ontology.getName());

        }

        @Override
        public boolean done() {
            return false;
        }
    }

    private void listFillere() {
        for (int i = 1; i < nbStories + 1; i++) {
            ArrayList<Task> tasks = new ArrayList<>();
            int rand = new Random().nextInt(1, nbTasks + 1);
            int randPri = new Random().nextInt(1, 6);
            for (int y = 1; y < rand + 1; y++) {
                tasks.add(utils.taskGen("Task nr. " + y));
            }
            Story story = new Story();

            story.setName("Story nr. " + i);
            story.setPriority(randPri);
            story.setTasks(tasks);
            story.setState(State.Product);


            stories.add(story);
            System.out.println(story);
        }
        System.out.println();
    }

    public int getTotalSize(State state) {
        int sum = 0;
        for (Story story : stories) {
            if (story.getState() == state) {
                sum = sum + story.getTotalSize();
            }
        }
        return sum;
    }

    public void ScrumBoardStatus() {
        State[] states = State.values();
        for (State state : states) {
            System.out.println("Total size of " + state + " is " + getTotalSize(state));
        }
    }

    public Story getFirstFreeStory(AID name) {
        for (Story s : stories) {
            if (s.getState() == State.Product) {
                if (s.getWorkingagent() == null){
                    s.setState(State.Doing);
                    s.setWorkingagent(name);
                    return s;
                }

            }
        }
        return null;
    }
}
