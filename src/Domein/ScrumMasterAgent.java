package Domein;

import Enums.State;
import Items.*;
import Util.Utils;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ScrumMasterAgent extends Agent {

    private Utils utils = new Utils();
    private Ontology ontology = ScrumOntology.getInstance();
    private Codec codec = new SLCodec();
    private boolean backlogSend = false;
    public ScrumMasterAgent() throws BeanOntologyException {
    }

    protected void setup() {
        utils.pintAgentIntel(this);

        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(Controller.scrumMasterAgent);
        sd.setName(getLocalName());
        dfd.addServices(sd);

        Behaviour wait = new Wait(this);

        addBehaviour(wait);
    }

    public class MessageHandler extends SimpleBehaviour {
        @Override
        public void action() {

            ACLMessage msg = receive();
            if (msg != null) {
                //System.out.println(getAID().getLocalName() + " recived message " + msg);
                switch (msg.getPerformative()){
                    case ACLMessage.REQUEST:
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.AGREE);
                        reply.setLanguage(codec.getName());
                        try {
                            PreRequestSprintBacklog preRequestSprintBacklog = (PreRequestSprintBacklog) getContentManager().extractContent(msg);
                            PreBackLog backlog = new PreBackLog();
                            backlog.setStoryArrayList(fillSprintQueue(preRequestSprintBacklog.getStoryArrayList(), preRequestSprintBacklog.getNbStoriesInSprint(), preRequestSprintBacklog.getSkill()));
                            getContentManager().fillContent(reply, backlog);
                        } catch (Exception e) {
                            System.out.println("Error: " + e.getMessage());
                        }
                        send(reply);
                        System.out.println(myAgent.getAID() + " sent message " + reply);
                        backlogSend = true;
                        break;
                }
            } else {
                block();
            }
        }
        @Override
        public boolean done() {
            return backlogSend;
        }
    }

    public class Wait extends Behaviour {
        Wait(Agent agent) {
            super(agent);
        }
        @Override
        public void action() {
            SequentialBehaviour running = new SequentialBehaviour(myAgent);

            //Behaviour messageReceiver = new MessageReceiver();
            SimpleBehaviour messageHandler = new MessageHandler();

            //.setDataStore(running.getDataStore());
            messageHandler.setDataStore(running.getDataStore());

            //running.addSubBehaviour(messageReceiver);
            running.addSubBehaviour(messageHandler);

            addBehaviour(running);
        }

        @Override
        public boolean done() {
            return false;
        }
    }

    private ArrayList<Story> fillSprintQueue(ArrayList<Story> stories, int sprintSize, int skill) {
        ArrayList<Story> sprintQueue = new ArrayList<>();
        AtomicInteger count = new AtomicInteger();
        stories.forEach((story) -> {
            if(count.get() < sprintSize){
                story.setState(State.Sprint);
                sprintQueue.add(story);
                count.addAndGet(1);
            }
        });
        return sprintQueue;
    }
}
