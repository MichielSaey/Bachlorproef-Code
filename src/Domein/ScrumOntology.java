package Domein;

import jade.content.onto.BeanOntology;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;

public class ScrumOntology extends BeanOntology {

    public static final String ONTOLOGY_NAME = "Scrum-ontology";
    private static Ontology INSTANCE;

    static {
        try {
            INSTANCE = new ScrumOntology();
        } catch (BeanOntologyException e) {
            throw new RuntimeException(e);
        }
    }

    public ScrumOntology() throws BeanOntologyException {
        super(ONTOLOGY_NAME);
        add("Items");
    }

    public synchronized final static Ontology getInstance() throws BeanOntologyException {
        return INSTANCE;
    }
}
