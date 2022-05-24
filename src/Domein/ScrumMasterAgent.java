package Domein;

import Util.Utils;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class ScrumMasterAgent extends Agent {

    private Utils utils = new Utils();

    protected void setup() {
        utils.pintAgentIntel(this);


        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive();

                if (msg != null) {

                } else {

                    block();
                }
            }
        });

    }

}
