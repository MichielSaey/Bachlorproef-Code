package Items;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;

import java.util.ArrayList;

public class PreBackLog implements Predicate {
    private static final long serialVersionUID = 1L;

    public ArrayList<Story> getStoryArrayList() {
        return storyArrayList;
    }

    public void setStoryArrayList(ArrayList<Story> storyArrayList) {
        this.storyArrayList = storyArrayList;
    }

    @Slot(mandatory=true)
    private ArrayList<Story> storyArrayList = new ArrayList<>();



}
