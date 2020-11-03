//the list of imports

import logist.LogistSettings;
import logist.agent.Agent;
import logist.behavior.CentralizedBehavior;
import logist.config.Parsers;
import logist.plan.Plan;
import logist.simulation.Vehicle;
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
 */
@SuppressWarnings("unused")
public class Centralized implements CentralizedBehavior {

    private Agent agent;
    private double timeout_planLook;
    private double timeout_planDig;
    private double prob;
    private int lookIter;

    @Override
    public void setup(Topology topology, TaskDistribution distribution,
                      Agent agent) {

        // this code is used to get the timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config" + File.separator + "settings_default.xml");
        } catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }

        // the plan method cannot execute more than timeout_plan milliseconds
        assert ls != null;
        this.timeout_planLook = ls.get(LogistSettings.TimeoutKey.PLAN) * 0.95;
        this.timeout_planDig = ls.get(LogistSettings.TimeoutKey.PLAN) * 0.05;
        this.agent = agent;
        this.prob =  Double.parseDouble(this.agent.readProperty("prob", String.class, "1"));
        this.lookIter =  Integer.parseInt(this.agent.readProperty("lookIter", String.class, "10000"));
    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long time_start = System.currentTimeMillis();
        boolean demo = false;

        // Init task distribution
        Variables var = new Variables(vehicles, tasks);

        // Search solution
        var.selectInitialSolution(vehicles);
        var = SLS(var, this.prob, this.lookIter, time_start, this.timeout_planLook, Double.POSITIVE_INFINITY);

        //Dig more best choice

        long time_dig = System.currentTimeMillis();
        var = SLS(var, 1., 1, time_dig, this.timeout_planDig, var.BestCost);

        // output result and return plan

        if (demo) { verboseOut(var.BestCost, time_start); }
        else { System.out.println("Generating plan"); }

        return createPlan(var, vehicles, tasks);
    }

    private void verboseOut(double bestCost, long time_start){
        System.out.println("----RESULT----");
        System.out.println("Params: \niter:\t"+ this.lookIter + "\np:\t" + this.prob);
        double elapsed_time = (System.currentTimeMillis() - time_start)/1000.;
        System.out.println("Time (s):\t"+ elapsed_time);
        System.out.println("Cost:\t" + bestCost);
        System.exit(0);
    }

    private Variables SLS(Variables var, double prob, int stopIter,
                          long time_start, Double timeout_plan, double absoluteBestCost) {
        List<Variables> N;
        Variables BestChoice = var.copy();
        int NoLocalImp = 0;
        int i = 0;
        int NoImprovement = 0;

        while (NoImprovement < stopIter &&
                checkTimeConstraint(time_start, timeout_plan)) {
            N = var.chooseNeighbour();
            System.out.println("Proposed " + N.size() + " neighbors");
            var = var.LocalChoice(N, prob);
            if (var.BestCost >= absoluteBestCost) {
                if (var.localChoiceBool) {
                    NoImprovement++;
                }
                System.out.println("NO IMPROVEMENT: " + NoImprovement);
            } else {
                absoluteBestCost = var.BestCost;
                NoImprovement = 0;
                BestChoice = var;
                System.out.println("IMPROVEMENT: ");
            }
            System.out.println("BEST COST " + var.BestCost);
        }

        return BestChoice;
    }

    private List<Plan> createPlan(Variables A, List<Vehicle> vehicles, TaskSet tasks) {
        ArrayList<Plan> multiVPlan = new ArrayList<>();
        PUDTask t;
        City current;
        Plan plan;

        for (Vehicle v : vehicles) {
            current = v.getCurrentCity();
            plan = new Plan(current);

            t = A.nextTaskV.get(v);
            while (t != null) {
                // move: current city => pickup location
                if (t.type.equals("pick")) {
                    for (City city : current.pathTo(t.task.pickupCity)) {
                        plan.appendMove(city);
                    }
                    plan.appendPickup(t.task);
                    current = t.task.pickupCity;
                }

                if (t.type.equals("deliver")) {
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

    boolean checkTimeConstraint(long time_start, double timeConstraint) {
        return System.currentTimeMillis() - time_start < timeConstraint;
    }

}