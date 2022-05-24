package Domein;

import Util.Utils;
import Behaviour.*;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.lang.acl.ACLMessage;

public class ProductOwnerAgent extends Agent {

    private static final long serialVersionUID = 1L;
    private Utils utils = new Utils();

    protected void setup() {
        //      Agent intel
        utils.pintAgentIntel(this);

        SequentialBehaviour sb = new SequentialBehaviour(this);
        Behaviour b = new MessageReceiver();
        b.setDataStore(sb.getDataStore());
        sb.addSubBehaviour(b);
        b = new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive();

                if (msg != null) {
                    switch (msg.getPerformative()){
                        case ACLMessage.REQUEST:
                            System.out.println();
                    }
                } else {
                    block();
                }
            }
        };

    }

}
