package Behaviour;

import jade.content.ContentElement;
import jade.content.ContentManager;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class Request extends OneShotBehaviour {

    private ContentManager manager;
    private Ontology onto;
    private AID receiver;
    private Object content;


    public Request(Agent sender, AID receiver, Ontology onto, Object content) {
        super(sender);
        manager = myAgent.getContentManager();
        this.onto = onto;
        this.receiver = receiver;
        this.content = content;
    }

    @Override
    public void action() {
        try {
            // Prepare the message
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.setOntology(onto.getName());
            msg.addReceiver(receiver);
            msg.setLanguage(new SLCodec().getName());

            // Fill the content
            if (content instanceof String) {
                this.content = (String) content;
            } else {
                myAgent.getContentManager().fillContent(msg, (ContentElement) this.content);
            }
            //System.out.println(myAgent.getAID() + " sending message " + msg);
            myAgent.send(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
