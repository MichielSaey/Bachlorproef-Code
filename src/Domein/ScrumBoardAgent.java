package Domein;

import Behaviour.MessageReceiver;
import Enums.State;
import Items.*;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;
import jade.core.AID;
import jade.core.Agent;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import Util.Utils;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class ScrumBoardAgent extends Agent {

    private final Utils utils = new Utils();
    private static final int nbStoriesToGen = 20;
    private static final int nbStoriesInSprint = 10;
    private static final int nbTasks = 5;
    private int nbStoriesSent = 0;
    private int nbStoriesRecived = 0;
    private ArrayList<Story> stories = new ArrayList<>();
    private ArrayBlockingQueue<Story> sprintQueue = new ArrayBlockingQueue<Story>(nbStoriesInSprint, true);

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
        fillSprintQueue();
        ScrumBoardStatus();

        //Repeater
        CyclicBehaviour running = new CyclicBehaviour(this) {
            @Override
            public void action() {

                int workLeft = nbStoriesInSprint - nbStoriesRecived;

                if (workLeft != nbStoriesInSprint){
                    System.out.println("stories left: " + workLeft);
                }

                    //Main
                    SequentialBehaviour behaviour = new SequentialBehaviour(myAgent);

                    //Sub
                    SimpleBehaviour msgR = new MessageReceiver();
                    SimpleBehaviour sbmh = new ScrumBoardMessageHandler(myAgent);
                    //SimpleBehaviour workComplete = new checkSprintComplete(myAgent);

                    //DataStore
                    msgR.setDataStore(behaviour.getDataStore());
                    sbmh.setDataStore(behaviour.getDataStore());

                    //Add To Main
                    behaviour.addSubBehaviour(msgR);
                    behaviour.addSubBehaviour(sbmh);

                    //Run
                    addBehaviour(behaviour);
                if (workLeft == 0) {
                    SimpleBehaviour endSprint = new EndSprint(myAgent);
                    addBehaviour(endSprint);
                    System.out.println("Sprint finished");
                    this.getParent().block();
                }

            }
        };

        //Run
        addBehaviour(running);
    }

    public class checkSprintComplete extends SimpleBehaviour{
        public checkSprintComplete(Agent a) {
            super(a);
        }

        @Override
        public void action() {

        }

        @Override
        public boolean done() {
            return false;
        }
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
            ACLMessage rmsg = msg.createReply();

            Story story;
            WorkOrder wo;

            switch (msg.getPerformative()) {
                case ACLMessage.REQUEST:
                    if(sprintQueue.size() != 0) {
                        //sprintQueue.
                        try {
                            story = sprintQueue.take();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                        rmsg.setPerformative(ACLMessage.AGREE);
                        rmsg.addReceiver(msg.getSender());
                        rmsg.setSender(myAgent.getAID());
                        rmsg.setLanguage(codec.getName());
                        rmsg.setOntology(ontology.getName());

                        if (story != null) {
                            wo = new WorkOrder();
                            wo.setStory(story);
                            wo.setOwner(msg.getSender());
                            try {
                                getContentManager().fillContent(rmsg, wo);
                                //System.out.println(rmsg);
                                send(rmsg);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    } else {
                        rmsg.setPerformative(ACLMessage.FAILURE);
                        send(rmsg);
                    }
                    break;
                case ACLMessage.CONFIRM:
                    try {
                        String storyName =  msg.getContent();
                        stories.forEach(e -> {
                            if (e.getName().equals(storyName)){
                                if (e.getState() != State.Done) {
                                    e.setState(State.Done);
                                    System.out.println(e.getName() + " is finished by " + msg.getSender().getLocalName());
                                }
                            }
                        });
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
        for (int i = 1; i < nbStoriesToGen + 1; i++) {
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
        }
        System.out.println();
    }

    private void fillSprintQueue() {
        AtomicInteger count = new AtomicInteger();
        stories.forEach((story) -> {
            if(count.get() < nbStoriesInSprint){
                story.setState(State.Sprint);
                sprintQueue.add(story);
                count.addAndGet(1);
            }
        });
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
