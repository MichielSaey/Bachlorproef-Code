package Util;

import Items.*;
import jade.core.Agent;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;

import java.util.ArrayList;
import java.util.Random;

public final class Utils {
    public Task taskGen(String naam){

        int randSize = new Random().nextInt(1, 21);

        Task t = new Task();
        t.setSize(randSize);
        t.setNaam(naam);

        return  t;
    }

    public void createOneAgent(ContainerController container, String agentName, String className, Object[]... agentOptionnalParameters) {
        try {
            AgentController ag = container.createNewAgent(agentName,className,agentOptionnalParameters);
            ag.start();
            try {
                System.out.println(agentName+" launched on "+container.getContainerName());
            } catch (ControllerException e) {
                e.printStackTrace();
            }
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }

    public void pintAgentIntel(Agent agent){
        System.out.println("------");
        System.out.println("Local name = " + agent.getAID().getLocalName());
        System.out.println("GUID = " + agent.getAID().getName());
        System.out.println(agent.getAID().getLocalName() + " set up has begun");
        System.out.println("------");
    }

    public ArrayList<Story> findAndReplace(Story story, ArrayList<Story> stories) {
        stories.set( stories.indexOf(story.getName()) , story);
        return stories;
    }
}
