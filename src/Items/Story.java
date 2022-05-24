package Items;

import Enums.State;
import jade.content.Concept;
import jade.content.onto.annotations.AggregateSlot;
import jade.content.onto.annotations.Slot;
import jade.core.AID;


import java.util.ArrayList;
import java.util.Objects;

public class Story implements Concept {
    private static final long serialVersionUID = 1L;

    private String name;
    public ArrayList<Task> tasks = new ArrayList<>();
    //	between 1 and 5
    private int priority;
    private State state;
    private AID workingagent;

/*    public Story(String name, int priority*//*, ArrayList<Task> tasks*//*){
        this.name = name;
        //this.tasks = tasks;
        this.priority = priority;
        //this.state = State.Product;
    }*/

    @Override
    public String toString() {
        return "Story{" +
                "name='" + name + '\'' +
                ", prioroty=" + priority +
                ", state=" + state +
                ", Total Size=" + getTotalSize() +
                ", tasks=" + tasks +
                '}';
    }

    public int getTotalSize() {
        int sum = 0;
        for (Task t : tasks) {
            sum = sum + t.getSize();
        }
        return sum;
    }

    @Slot(mandatory=true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Slot(mandatory=true)
    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public AID getWorkingagent() {return workingagent;}

    public void setWorkingagent(AID workingagent) {state = State.Doing;this.workingagent = workingagent;}

    public ArrayList<Task> getTasks() {
        return tasks;
    }

    public void setTasks(ArrayList<Task> tasks) {
        this.tasks = tasks;
    }
}
