package Behaviour;

import jade.content.ContentManager;
import jade.content.onto.Ontology;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class RequestUserStory extends OneShotBehaviour {

    private ContentManager manager;
    private MessageTemplate template;
    private Ontology onto;
    private AID receiver;

    public RequestUserStory(Agent a, MessageTemplate template, AID receiver, Ontology onto) {
        super(a);
        manager = myAgent.getContentManager();
        this.onto = onto;
        this.template = template;
        this.receiver = receiver;
    }

    @Override
    public void action() {
        try {
            // Prepare the message
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.setOntology(onto.getName());
            msg.addReceiver(receiver);

            // Fill the content
            //manager.fillContent(msg, );

            myAgent.send(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
