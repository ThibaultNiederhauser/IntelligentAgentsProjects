//the list of imports

import logist.LogistSettings;
import logist.agent.Agent;
import logist.behavior.CentralizedBehavior;
import logist.config.Parsers;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 *
 */
@SuppressWarnings("unused")
public class Centralized implements CentralizedBehavior {

    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private long timeout_setup;
    private long timeout_plan;
    private final double p = 0.4;


    @Override
    public void setup(Topology topology, TaskDistribution distribution,
                      Agent agent) {

        // this code is used to get the timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config" + File.separator + "settings_default.xml");
        }
        catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }

        // the setup method cannot last more than timeout_setup milliseconds
        timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
        // the plan method cannot execute more than timeout_plan milliseconds
        timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);

        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;
    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long time_start = System.currentTimeMillis();

        //initialize the variable as null lists
        int nT = tasks.size();
        int nV = vehicles.size();
        List<Variables> N;
        Variables BestChoice = new Variables();
        Variables var = new Variables(vehicles, tasks);
        double AbsoluteBestCost = Double.POSITIVE_INFINITY;
        int NoImprovement = 0;
        int NoLocalImp = 0;
        int i = 0;

        var.selectInitialSolution(vehicles);
        while(NoImprovement < 10000) {
            //System.out.println("Choose neighbours");
            N = var.chooseNeighbour();
            System.out.println("Neighbours chosen " + i);
            var = var.LocalChoice(N, AbsoluteBestCost, p);
            if(var.BestCost >= AbsoluteBestCost){
                if(var.localChoiceBool){
                    NoImprovement++;
                }
                System.out.println("NO IMPROVMENT: " + NoImprovement);
            }
            else{
                AbsoluteBestCost = var.BestCost;
                NoImprovement = 0;
                BestChoice = var;
                System.out.println("IMPROVMENT: ");

            }
            System.out.println("BEST COST " + var.BestCost);
            System.out.println("Absolute COST " + AbsoluteBestCost);
            //if(NoImprovement > 1000){ //Go back to best choice
            //    var = BestChoice;
             //   NoImprovement = 0;
              //  N = var.chooseNeighbour();
               // var = var.LocalChoice(N, AbsoluteBestCost, 1);
               // i++;
           // }
        }
        //Dig best choice
        var = BestChoice;
        NoImprovement = 0;
        while(NoImprovement < 10) {
            //System.out.println("Choose neighbours");
            N = var.chooseNeighbour();
            System.out.println("Neighbours chosen " + i);
            var = var.LocalChoice(N, AbsoluteBestCost, 1);
            if(var.BestCost >= AbsoluteBestCost){
                if(var.localChoiceBool){
                    NoImprovement++;
                }
                System.out.println("NO IMPROVMENT: " + NoImprovement);
            }
            else{
                AbsoluteBestCost = var.BestCost;
                NoImprovement = 0;
                BestChoice = var;
                System.out.println("IMPROVMENT: ");
            }

            System.out.println("BEST COST " + var.BestCost);
            System.out.println("Absolute COST " + AbsoluteBestCost);
            //if(NoImprovement > 1000){ //Go back to best choice
            //    var = BestChoice;
            //   NoImprovement = 0;
            //  N = var.chooseNeighbour();
            // var = var.LocalChoice(N, AbsoluteBestCost, 1);
            // i++;
            // }
        }
        var = BestChoice;


        System.out.println("Loop over");

        List<Plan> SLSPlan = createPlan(var, vehicles, tasks);

        return SLSPlan;
    }


    private List<Plan> createPlan(Variables A, List<Vehicle> vehicles, TaskSet tasks){
        ArrayList<Plan> multiVPlan = new ArrayList<>();
        PUDTask t;
        City current;
        Plan plan;

        for(Vehicle v:vehicles){
            current = v.getCurrentCity();
            plan = new Plan(current);

            t = A.nextTaskV.get(v);
            while(t != null){
                // move: current city => pickup location
                if(t.type.equals("pick")){
                    for (City city : current.pathTo(t.task.pickupCity)) {
                        plan.appendMove(city);
                    }
                    plan.appendPickup(t.task);
                    current = t.task.pickupCity;
                }

                if(t.type.equals("deliver")){
                    for (City city : current.pathTo(t.task.deliveryCity)) {
                        plan.appendMove(city);
                    }
                    plan.appendDelivery(t.task);
                    current = t.task.deliveryCity;
                }

                // set current city
                t = A.nextTaskT.get(t);
            }

            multiVPlan.add(plan);

        }
        return multiVPlan;
    }

    /*private Plan SLSPlan(Vehicle vehicle, TaskSet tasks, Variables var) {
        //TODO

    }*/

}