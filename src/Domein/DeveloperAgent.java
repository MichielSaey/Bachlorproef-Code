package Domein;

import Behaviour.MessageReceiver;
import Behaviour.Request;
import Items.*;
import Items.PreWorkOrder;
import Util.Utils;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static Domein.Controller.*;

public class DeveloperAgent extends Agent {
    private Utils utils = new Utils();
    private Ontology ontology = ScrumOntology.getInstance();
    private Codec codec = new SLCodec();

    //Agent Tools
    private boolean finished = false;
    private boolean sprintStarted = false;
    private ArrayBlockingQueue<Story> toDo = new ArrayBlockingQueue<Story>(10, true);
    private ArrayList<String> workLog = new ArrayList<>();
    Random random = new Random();
    private int skill = random.nextInt((10 - 1) + 1) + 1; //TODO: make random add learning curve

    private MessageTemplate template = MessageTemplate.and(
            MessageTemplate.MatchSender(this.getAID()),
            MessageTemplate.MatchLanguage(codec.getName())
    );

    public DeveloperAgent() throws BeanOntologyException {
    }

    protected void setup() {
//      Agent intel
        utils.pintAgentIntel(this);

        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(Controller.devAgentName);
        sd.setName(getLocalName());
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            throw new RuntimeException(e);
        }

        Behaviour wait = new Wait(this);

        addBehaviour(wait);

    }

    public class DeveloperMessageHandler extends SimpleBehaviour {
        private DeveloperAgent myDEAgent;

        DeveloperMessageHandler(Agent agent) {
            super(agent);
            this.myDEAgent = (DeveloperAgent) agent;
        }

        @Override
        public void action() {
            ACLMessage msg = receive();

            if (msg != null) {
                System.out.println(myDEAgent.getAID().getLocalName() + " recived message " + msg);
                switch (msg.getPerformative()) {
                    case ACLMessage.AGREE:
                        Story story = null;
                        try {
                            PreWorkOrder wo = (PreWorkOrder) getContentManager().extractContent(msg);
                            story = wo.getStory();
                            if (story != null) {
                                if (!workLog.contains(story.getName())) {
                                    AtomicBoolean own = new AtomicBoolean(false);
                                    Story finalStory = story;
                                    toDo.forEach(e -> {
                                        if (e.getName().equals(finalStory.getName())) {
                                            own.set(true);
                                        }
                                    });
                                    if (own.get() == false) {
                                        toDo.add(story);
                                        workLog.add(story.getName());
                                    }
                                } else {
                                    block();
                                }
                            } else {
                                block();
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    case ACLMessage.REQUEST:
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.AGREE);
                        reply.setLanguage(codec.getName());
                        reply.setContent(String.valueOf(myDEAgent.skill));
                        send(reply);
                        block(3000);
                        finished = true;
                        break;
                    case ACLMessage.INFORM:
                        sprintStarted = true;
                        finished = true;
                    /*case ACLMessage.CANCEL:
                        System.out.println(myAgent.getLocalName() + "stops working");
                        block();
                        break;*/
                    default:
                        block();
                }
            }

        }

        @Override
        public boolean done() {
            return finished;
        }
    }
    public class Wait extends Behaviour {
        Wait(Agent agent) {
            super(agent);
        }
        @Override
        public void action() {
            SequentialBehaviour running = new SequentialBehaviour();

            SimpleBehaviour messageHandler = new DeveloperMessageHandler(myAgent);
            messageHandler.setDataStore(running.getDataStore());
            running.addSubBehaviour(messageHandler);

            if (sprintStarted) {
                sprintStarted = false;
                Daily daily = new Daily(myAgent);
                running.addSubBehaviour(daily);
            }

            addBehaviour(running);
        }

        @Override
        public boolean done() {
            return false;
        }
    }
    public class Daily extends Behaviour {
        //work 2 times a day. If work is not avalable request it. when it is requested receive it.
        private int tick = 1;

        public Daily(Agent a) {
            super(a);
            finished = false;
        }

        @Override
        public void action() {
            block(1000);

            if (!toDo.isEmpty()) {
                System.out.println("To do list " + myAgent.getLocalName() + toDo);
            }

            int todoSize = 0;
            for (Story story : toDo) {
                todoSize += story.getTotalSize();
            }

            if (todoSize == 0 && toDo.isEmpty()) {
                SequentialBehaviour requestUserStoryCom = new SequentialBehaviour(myAgent);

                //Sub to request work
                Request requestUserStory = new Request(myAgent, scrumBoardAID, ontology, "story");
                DeveloperMessageHandler messageHandler = new DeveloperMessageHandler(myAgent);

                //DataStore
                requestUserStory.setDataStore(requestUserStoryCom.getDataStore());
                messageHandler.setDataStore(requestUserStoryCom.getDataStore());

                //Add To Main
                requestUserStoryCom.addSubBehaviour(requestUserStory);
                requestUserStoryCom.addSubBehaviour(messageHandler);

                addBehaviour(requestUserStoryCom);
            }

            tick++;

            //Work Avalable? YES
            if (!toDo.isEmpty()) {
                Work work = new Work(myAgent);

                addBehaviour(work);
            }

        }

        @Override
        public boolean done() {
            /*if (tick == 10) {
                tick = 0;
                return true;
            }*/
            return false;
        }
    }
    public class Work extends SimpleBehaviour {

        public Work(Agent a) {
            super(a);
        }
        int tick = 0;
        @Override
        public void action() {
            block(100);
            if (!toDo.isEmpty()) {
                Story story = null;

                story = toDo.peek();

                if (story.getTotalSize() != 0) {
                    System.out.println(getLocalName() + " works on: " + story.getName() + " with a work size of " + story.getTotalSize());
                    for (Task task : story.getTasks()) {
                        if (!(tick >= 1)) {
                            if (task.getSize() > 0) {
                                //System.out.println("Work to do: " + task.getSize());
                                //System.out.println("Work done: " + skill);
                                task.setSize(task.getSize() - skill);
                                //System.out.println("Work left: " + task.getSize());
                                tick++;
                            }
                        }
                    }
                    ;
                    if (story.getTotalSize() <= 0) {
                        workFinished(myAgent, story);
                        toDo.remove(story);
                        System.out.println(getLocalName() + " finished work on: " + story.getName());
                    }
                }

            }
        }


        @Override
        public boolean done() {
            if (tick >= 2) {
                return true;
            } else {
                return false;
            }
        }
    }
    private void workFinished(Agent myAgent, Story story) {
        if (!toDo.isEmpty()) {
            toDo.forEach((a) -> {
                if (a.getName().equals(story.getName())) {
                    toDo.remove(a);

                    ACLMessage msg = new ACLMessage(ACLMessage.CONFIRM);
                    msg.addReceiver(new AID(Controller.scrumBoardAgent, AID.ISLOCALNAME));
                    msg.setSender(myAgent.getAID());
                    msg.setLanguage(codec.getName());
                    msg.setContent(story.getName());

                    try {
                        toDo.size();
                        send(msg);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
    }
}

