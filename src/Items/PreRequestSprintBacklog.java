package Items;

import jade.content.Predicate;

import java.util.ArrayList;

public class PreRequestSprintBacklog implements Predicate {
    private ArrayList<Story> storyArrayList = new ArrayList<>();
    private int skill;

    private int nbStoriesInSprint;

    public ArrayList<Story> getStoryArrayList() {
        return storyArrayList;
    }

    public void setStoryArrayList(ArrayList<Story> storyArrayList) {
        this.storyArrayList = storyArrayList;
    }
    public int getSkill() {
        return skill;
    }
    public void setSkill(int skill) {
        this.skill = skill;
    }
    public int getNbStoriesInSprint() {
        return nbStoriesInSprint;
    }
    public void setNbStoriesInSprint(int nbStoriesInSprint) {
        this.nbStoriesInSprint = nbStoriesInSprint;
    }
}
