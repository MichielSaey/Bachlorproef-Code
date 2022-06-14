package Behaviour;

import Domein.ScrumBoardAgent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;

public class MessageReceiver extends CyclicBehaviour {
    public static final String RECV_MSG = "received-message";
    private boolean finished = false;
    public void action() {
        ACLMessage msg = myAgent.receive();
        if (msg!= null) {
            //System.out.println(myAgent.getAID() + " recived message " + msg);
            getDataStore().put(RECV_MSG, msg);
            finished = true;
        } else {
            block();
        }
    }
}
