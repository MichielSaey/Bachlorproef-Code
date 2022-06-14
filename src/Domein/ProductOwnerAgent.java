package Domein;

import Enums.State;
import Items.PreBackLog;
import Items.Story;
import Items.Task;
import Util.Utils;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;
import java.util.Random;

public class ProductOwnerAgent extends Agent {

    private static final long serialVersionUID = 1L;
    private Utils utils = new Utils();
    private static final int nbStoriesToGen = 20;
    private static final int nbTasks = 5;
    private ArrayList productBackLog = new ArrayList();
    private boolean backlogSend = false;
    private Ontology ontology = ScrumOntology.getInstance();
    private Codec codec = new SLCodec();

    public ProductOwnerAgent() throws BeanOntologyException {
    }

    protected void setup() {
        //      Agent intel
        utils.pintAgentIntel(this);

        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(Controller.productOwnerAgent);
        sd.setName(getLocalName());
        dfd.addServices(sd);

        Behaviour wait = new Wait(this);

        addBehaviour(wait);
    }

    public class MessageHandler extends SimpleBehaviour {
        @Override
        public void action() {
            ACLMessage msg = receive()/*(ACLMessage) getDataStore().get(MessageReceiver.RECV_MSG)*/;
            if (msg != null) {
                System.out.println(getAID().getLocalName() + " recived message " + msg);
                switch (msg.getPerformative()) {
                    case ACLMessage.REQUEST:
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.AGREE);
                        reply.setLanguage(codec.getName());
                        try {
                            listFiller();
                            PreBackLog backlog = new PreBackLog();
                            backlog.setStoryArrayList(productBackLog);
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
            if (backlogSend) {
                block(5000);
                return true;
            } else {
                return false;
            }
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
            return true;
        }
    }

    private void listFiller() {
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

            productBackLog.add(story);
        }
        System.out.println();
    }

}
