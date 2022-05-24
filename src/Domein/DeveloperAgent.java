package Domein;

import Behaviour.MessageReceiver;
import Behaviour.RequestUserStory;
import Items.*;
import Items.WorkOrder;
import Util.Utils;
import jade.content.ContentElement;
import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyUtils;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class DeveloperAgent extends Agent {
    private Utils utils = new Utils();
    private Ontology ontology = ScrumOntology.getInstance();
    private Codec codec = new SLCodec();

    //Agent Tools
    Queue<Story> toDo = new LinkedList<>();
    private int skill = 3; //TODO: make random add learning curve

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
        sd.setType(Controller.DevAgentName);
        sd.setName(getLocalName());
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            throw new RuntimeException(e);
        }

        //Daily
        int tick = 0;
        //Main
        CyclicBehaviour daily = new CyclicBehaviour(this) {
            @Override
            public void action() {

                //TODO: check if work is avalable.
                //Work Avalable? NO

                if (!toDo.isEmpty()){
                    System.out.println("To do list " + myAgent.getLocalName() + toDo);

                }

                int todoSize = 0;
                for (Story story : toDo) {
                    todoSize += story.getTotalSize();
                }

                if (todoSize == 0 && toDo.isEmpty()) {
                    SequentialBehaviour requestUserStoryCom = new SequentialBehaviour(myAgent);

                    //Sub to request work
                    RequestUserStory requestUserStory = new RequestUserStory(myAgent, template, new AID(Controller.ScrumBoardAgent, AID.ISLOCALNAME), ontology);
                    MessageReceiver receiver = new MessageReceiver();
                    DeveloperMessageHandler messageHandler = new DeveloperMessageHandler(myAgent);

                    //DataStore
                    requestUserStory.setDataStore(requestUserStoryCom.getDataStore());
                    receiver.setDataStore(requestUserStoryCom.getDataStore());
                    messageHandler.setDataStore(requestUserStoryCom.getDataStore());

                    //Add To Main
                    requestUserStoryCom.addSubBehaviour(requestUserStory);
                    requestUserStoryCom.addSubBehaviour(receiver);
                    requestUserStoryCom.addSubBehaviour(messageHandler);

                    addBehaviour(requestUserStoryCom);
                }

                //Work Avalable? YES
                if (!toDo.isEmpty()){
                    Work work = new Work(myAgent);

                    addBehaviour(work);
                }
            }
        };

        addBehaviour(daily);

    }

    public class DeveloperMessageHandler extends SimpleBehaviour {

        private DeveloperAgent mySBAgent;

        DeveloperMessageHandler(Agent agent) {
            super(agent);
            this.mySBAgent = (DeveloperAgent) agent;
        }

        @Override
        public void action() {
            ACLMessage msg = (ACLMessage) getDataStore().get(MessageReceiver.RECV_MSG);

            switch (msg.getPerformative()) {
                case ACLMessage.AGREE:
                    Story story = null;
                    try {

                        WorkOrder wo = (WorkOrder) getContentManager().extractContent(msg);
                        story = wo.getStory();
                        System.out.println();

                        if (story != null) {
                            toDo.add(story);
                        } else {
                            block();
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case ACLMessage.CANCEL:
                    System.out.println(myAgent.getLocalName() + "stops working");
                    this.getParent().block();
                    break;
                default:
                    block();
            }
        }

        @Override
        public boolean done() {
            return false;
        }
    }

    public class Work extends SimpleBehaviour {

        public Work(Agent a) {
            super(a);
        }

        @Override
        public void action() {

            int tick = 0;

            Story story = toDo.peek();

            if (!toDo.isEmpty()){
                System.out.println(getLocalName() + " works on: " + story);
                for (Task task : story.getTasks()) {
                    if (!(tick >= 1)) {
                        if (task.getSize() > 0) {
                            System.out.println("Work to do: " + task.getSize());
                            System.out.println("Work done: " + skill);
                            task.setSize(task.getSize() - skill);
                            System.out.println("Work left: " + task.getSize());
                            tick++;
                        }
                    }
                };
            }


            if(story.getTotalSize() <= 0){
                WorkFinished workFinished = new WorkFinished(myAgent, story);
                addBehaviour(workFinished);
            }

        }


        @Override
        public boolean done() {
            return false;
        }
    }

    public class WorkFinished extends SimpleBehaviour {

        Story story;
        public WorkFinished(Agent a, Story story) {
            super(a);
            this.story = story;
        }

        @Override
        public void action() {
            System.out.println(myAgent.getLocalName() + " has finished working on " + story);

            toDo.remove();

            ACLMessage msg = new ACLMessage(ACLMessage.CONFIRM);
            msg.addReceiver(new AID(Controller.ScrumBoardAgent, AID.ISLOCALNAME));
            msg.setSender(myAgent.getAID());
            msg.setLanguage(codec.getName());
            msg.setOntology(ontology.getName());

            WorkOrder wo = new WorkOrder();
            wo.setStory(story);
            wo.setOwner(msg.getSender());

            try {
                getContentManager().fillContent(msg, wo);
                System.out.println(msg);
                send(msg);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }

        @Override
        public boolean done() {
            return false;
        }
    }
}
