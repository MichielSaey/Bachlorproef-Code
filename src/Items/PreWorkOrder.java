package Items;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;
import jade.core.AID;

public class PreWorkOrder implements Predicate {
    private static final long serialVersionUID = 1L;

    private AID owner;
    private Story item;

    @Slot(mandatory=true)
    public AID getOwner() {
        return owner;
    }
    public void setOwner(AID id) {
        owner = id;
    }

    @Slot(mandatory=true)
    public Story getStory() {
        return item;
    }

    public void setStory(Story i) {
        item = i;
    }
}
