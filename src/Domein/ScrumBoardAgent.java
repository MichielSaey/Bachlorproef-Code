package Domein;

import Behaviour.Request;
import Enums.State;
import Items.*;

import static Domein.Controller.*;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;

import jade.content.onto.OntologyException;
import jade.content.onto.OntologyUtils;
import jade.core.AID;
import jade.core.Agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;

import Util.Utils;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class ScrumBoardAgent extends Agent {

    private final Utils utils = new Utils();
    //adress book
    private int nbStoriesSent = 0;
    private int nbStoriesRecived = 0;
    private Boolean finished = false;
    private boolean projectFinished = false;
    private boolean sprintFinished = false;
    private boolean receivedSpringBacklog = false;
    private int sprintNr = 0;
    //Board
    private ArrayList<Story> productBackLog = new ArrayList<>();
    private static final int nbStoriesInSprint = 10;
    private int[] devSkills = new int[nbDevAgents];
    ArrayList<String> devNames = new ArrayList<>();
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
        sd.setType(Controller.scrumBoardAgent);
        sd.setName(getLocalName());
        dfd.addServices(sd);

        try {
            OntologyUtils.exploreOntology(ontology);
        } catch (OntologyException e) {
            throw new RuntimeException(e);
        }

//		Data
/*        listFillere();
        fillSprintQueue();
        ScrumBoardStatus();*/

        //RunningBehaviour
        SequentialBehaviour running = new SequentialBehaviour(this);

        //Request Product backlog
        Behaviour requestProductBacklog = new RequestProductBacklog(this);
        requestProductBacklog.setDataStore(running.getDataStore());

        //Sprint
        Behaviour sprint = new Sprint(this);

        //Add sub behaviour to running behaviour
        running.addSubBehaviour(requestProductBacklog);
        running.addSubBehaviour(sprint);

       /* int workLeft = nbStoriesInSprint - nbStoriesRecived;

        if (workLeft != nbStoriesInSprint){
            System.out.println("stories left: " + workLeft);
        }

        //Main
        SequentialBehaviour behaviour = new SequentialBehaviour(myAgent);

        //Request Product Backlog


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
        }*/

        //Run
        addBehaviour(running);

        System.out.println(productBackLog);
    }

    public class ScrumBoardMessageHandler extends SimpleBehaviour {
        private ScrumBoardAgent mySBAgent;


        ScrumBoardMessageHandler(Agent agent) {
            super(agent);
            this.mySBAgent = (ScrumBoardAgent) agent;
        }

        @Override
        public void action() {
            block(1000);
            ACLMessage msg = receive();
            //build list for dev agent names. if a skill is received, add it to the list on the dev number - 1 position
            Story story;
            PreWorkOrder wo;
            if (msg != null) {

                ACLMessage rmsg = msg.createReply();

                System.out.println(mySBAgent.getAID().getLocalName() + " recived message " + msg);
                switch (msg.getPerformative()) {
                    case ACLMessage.AGREE:
                        switch (msg.getSender().getLocalName()) {
                            case productOwnerAgent:
                                try {
                                    PreBackLog preBackLog = (PreBackLog) getContentManager().extractContent(msg);
                                    productBackLog = preBackLog.getStoryArrayList();
                                    System.out.println("Product Backlog recived");
                                    System.out.println(productBackLog);
                                    finished = true;
                                } catch (Exception e) {
                                    System.out.println("Error: " + e);
                                }
                                break;
                            case scrumMasterAgent:
                                try {
                                    PreBackLog preBackLog = (PreBackLog) getContentManager().extractContent(msg);
                                    for (Story story1 : preBackLog.getStoryArrayList()) {
                                        sprintQueue.add(story1);
                                        if (productBackLog.contains(story1)) {
                                            productBackLog.remove(story1);
                                        }
                                    }
                                    System.out.println("Sprint Backlog recived");
                                    System.out.println(sprintQueue);
                                    receivedSpringBacklog = true;
                                    finished = true;
                                } catch (Exception e) {
                                    System.out.println("Error: " + e);
                                }
                                break;
                            default:
                                if (msg.getSender().getLocalName().contains(devAgentName)) {

                                    if (!(devNames.contains(msg.getSender().getLocalName()))) {
                                        devNames.add(msg.getSender().getLocalName());
                                        int devNumber = Integer.parseInt(msg.getSender().getLocalName().substring(devAgentName.length()));
                                        devSkills[devNumber - 1] = Integer.parseInt(msg.getContent());
                                        System.out.println("Dev " + devNumber + " recived skill: " + devSkills[devNumber - 1]);

                                    }
                                    System.out.println(devNames);
                                    if (devNames.size() == nbDevAgents) {
                                        System.out.println("received skills from all devs");
                                        for (int i = 0; i < devSkills.length; i++) {
                                            System.out.println("Dev " + (i + 1) + " skill: " + devSkills[i]);
                                        }
                                        finished = true;
                                    }
                                }
                                break;
                        }
                        break;
                    case ACLMessage.REQUEST:
                        if (sprintQueue.size() != 0) {
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
                                wo = new PreWorkOrder();
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
                            finished = true;
                            rmsg.setPerformative(ACLMessage.FAILURE);
                            send(rmsg);
                        }
                        break;
                    case ACLMessage.CONFIRM:
                        try {
                            String storyName = msg.getContent();
                            sprintQueue.forEach(e -> {
                                if (e.getName().equals(storyName)) {
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
        }

        @Override
        public boolean done() {
            return finished;
        }
    }

    public class RequestProductBacklog extends Behaviour {
        private ScrumBoardAgent mySBAgent;

        RequestProductBacklog(Agent agent) {
            super(agent);
            this.mySBAgent = (ScrumBoardAgent) agent;
        }

        @Override
        public void action() {
            //Request Product Backlog
            finished = false;
            System.out.println("Requesting Product Backlog");
            SequentialBehaviour productBacklog = new SequentialBehaviour(mySBAgent);

            Behaviour requestProductBacklog = new Request(mySBAgent, productOwnerAID, ontology, "story");
            //CyclicBehaviour messageReceiver = new MessageReceiver();
            SimpleBehaviour messageHandler = new ScrumBoardMessageHandler(mySBAgent);

            requestProductBacklog.setDataStore(productBacklog.getDataStore());
            //messageReceiver.setDataStore(productBacklog.getDataStore());
            messageHandler.setDataStore(productBacklog.getDataStore());

            productBacklog.addSubBehaviour(requestProductBacklog);
            //productBacklog.addSubBehaviour(messageReceiver);
            productBacklog.addSubBehaviour(messageHandler);

            addBehaviour(productBacklog);
        }

        @Override
        public boolean done() {
            if (productBackLog.size() == 0) {
                return false;
            } else {
                return true;
            }
        }
    }

    public class Sprint extends Behaviour {
        private ScrumBoardAgent mySBAgent;
        private Boolean finished = false;

        Sprint(Agent agent) {
            super(agent);
            this.mySBAgent = (ScrumBoardAgent) agent;
        }

        @Override
        public void action() {
            block(2000);
            sprintNr++;
            System.out.println("Sprint nr " + sprintNr + " being prepaired");
            SequentialBehaviour sprintBehaviour = new SequentialBehaviour(myAgent);

            //preperations to start sprint
            //Behaviour requestSkill = new RequestSkill(myAgent);
            Behaviour requestSprintBacklog = new RequestSprintBacklog(myAgent);

            //sprintBehaviour.addSubBehaviour(requestProductBacklog);

            if (receivedSpringBacklog == false) {
                sprintBehaviour.addSubBehaviour(requestSprintBacklog);
            }
            //start sprint
            Behaviour startSprint = new StartSprint(myAgent);
            sprintBehaviour.addSubBehaviour(startSprint);

            block(1000);
            if (productBackLog.size() == 0) {
                projectFinished = true;
            }
            if (sprintQueue.size() == 0) {
                receivedSpringBacklog = false;
                sprintFinished = true;
                finished = true;
            }
            if (sprintFinished == false/* && productBackLog.size() != 0*/) {
                finished = false;
                ScrumBoardMessageHandler messageHandler = new ScrumBoardMessageHandler(myAgent);
                sprintBehaviour.addSubBehaviour(messageHandler);
            }

            addBehaviour(sprintBehaviour);
        }

        @Override
        public boolean done() {
            if (projectFinished== true) {
                System.out.println("Project Finished");
                block(1000);
                return true;
            } else {
                return finished;
            }
        }
    }

    public class RequestSkill extends Behaviour {
        private ScrumBoardAgent mySBAgent;

        RequestSkill(Agent agent) {
            super(agent);
            this.mySBAgent = (ScrumBoardAgent) agent;
        }

        @Override
        public void action() {
            //Request Skill
            block(3000);
            System.out.println("Requesting Skill");
            finished = false;

            SequentialBehaviour skill = new SequentialBehaviour(mySBAgent);

            for (AID aid : Controller.getDevAgents()) {
                Behaviour requestSkill = new Request(mySBAgent, aid, ontology, "story");
                requestSkill.setDataStore(skill.getDataStore());
                skill.addSubBehaviour(requestSkill);
            }

            SimpleBehaviour messageHandler = new ScrumBoardMessageHandler(mySBAgent);
            messageHandler.setDataStore(skill.getDataStore());

            skill.addSubBehaviour(messageHandler);

            addBehaviour(skill);


            for (int i = 0; i < devSkills.length; i++) {
                System.out.println("Dev " + (i + 1) + " skill: " + devSkills[i]);
            }

        }

        @Override
        public boolean done() {
            return true;
        }
    }

    public class RequestSprintBacklog extends Behaviour {
        private ScrumBoardAgent mySBAgent;

        RequestSprintBacklog(Agent agent) {
            super(agent);
            this.mySBAgent = (ScrumBoardAgent) agent;
            finished = false;
        }

        @Override
        public void action() {

            System.out.println("Requesting Sprint Backlog");
            SequentialBehaviour sprintBacklog = new SequentialBehaviour(mySBAgent);

            PreRequestSprintBacklog preRequestSprintBacklog = new PreRequestSprintBacklog();
            int sum = 0;
            //Loop through the array to calculate sum of elements
            for (int i = 0; i < devSkills.length; i++) {
                sum = sum + devSkills[i];
            }
            preRequestSprintBacklog.setSkill(Arrays.stream(devSkills).sum());
            preRequestSprintBacklog.setStoryArrayList(productBackLog);
            preRequestSprintBacklog.setNbStoriesInSprint(nbStoriesInSprint);

            Request request = new Request(mySBAgent, scrumMasterAID, ontology, preRequestSprintBacklog);
            ScrumBoardMessageHandler messageHandler = new ScrumBoardMessageHandler(mySBAgent);

            sprintBacklog.addSubBehaviour(request);
            sprintBacklog.addSubBehaviour(messageHandler);

            addBehaviour(sprintBacklog);
            block(2000);
        }

        @Override
        public boolean done() {
            return finished;
        }
    }

    public class StartSprint extends Behaviour {
        private ScrumBoardAgent mySBAgent;

        StartSprint(Agent agent) {
            super(agent);
            this.mySBAgent = (ScrumBoardAgent) agent;
        }

        @Override
        public void action() {
            //Request Skill
            block(2000);
            System.out.println("Starting Sprint " + sprintNr);
            finished = false;


            for (AID aid : Controller.getDevAgents()) {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(aid);
                msg.setLanguage(codec.getName());
                msg.setOntology(ontology.getName());
                msg.setContent("startSprint");
                msg.setSender(mySBAgent.getAID());

                mySBAgent.send(msg);
            }
        }

        @Override
        public boolean done() {
            return true;
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
                msg.addReceiver(new AID(Controller.devAgentName + i, AID.ISLOCALNAME));
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


    public int getTotalSize(State state) {
        int sum = 0;
        for (Story story : sprintQueue) {
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
        for (Story s : sprintQueue) {
            if (s.getState() == State.Product) {
                if (s.getWorkingagent() == null) {
                    s.setState(State.Doing);
                    s.setWorkingagent(name);
                    return s;
                }
            }
        }
        return null;
    }
}
