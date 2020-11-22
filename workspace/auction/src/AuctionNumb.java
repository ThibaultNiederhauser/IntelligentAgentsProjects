//the list of imports
import logist.LogistPlatform;
import logist.LogistSettings;
import logist.agent.Agent;
import logist.behavior.AuctionBehavior;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 *
 */
@SuppressWarnings("unused")
public class AuctionNumb implements AuctionBehavior {

    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private Random random;
    private List<Vehicle> vehicleList;

    private City currentCity;
    private long currentCost;
    private long marginalCost;
    private double timeoutBid;
    private double timeoutPlanLook;
    private double timeoutPlanDig;
    private double prob;
    private int lookIter;
    private Variables currentVariables;
    private Variables winVar;

    @Override
    public void setup(Topology topology, TaskDistribution distribution,
                      Agent agent) {

        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;
        this.vehicleList = agent.vehicles();
        this.currentCity = vehicleList.get(0).homeCity();

        this.timeoutBid = LogistPlatform.getSettings().get(LogistSettings.TimeoutKey.BID);
        this.timeoutPlanLook = LogistPlatform.getSettings().get(LogistSettings.TimeoutKey.PLAN)*0.95;
        this.timeoutPlanDig =  LogistPlatform.getSettings().get(LogistSettings.TimeoutKey.PLAN)*0.05;

        long seed = -9019554669489983951L * currentCity.hashCode() * agent.id();
        this.random = new Random(seed);
        this.currentCost = 0;
        this.currentVariables = null;
        this.prob =  Double.parseDouble(this.agent.readProperty("prob", String.class, "1"));
        this.lookIter =  Integer.parseInt(this.agent.readProperty("lookIter", String.class, "10000"));
    }

    @Override
    public void auctionResult(Task previous, int winner, Long[] bids) {
        if (winner == agent.id()) {
           this.currentVariables = winVar;
           this.currentCost += this.marginalCost;
        }
    }

    @Override
    public Long askPrice(Task task) {
        this.marginalCost = 0;
        boolean tooHeavy = true;
        long margin = 10;
        Variables extendedVar = null;

        //Check that task can be carried
        for(Vehicle v : this.vehicleList){
            if(v.capacity() >= task.weight){
                tooHeavy = false;
            }
        }
        if(tooHeavy){return null;}

        //compute marginal cost
        if(currentVariables == null){
            ArrayList<Task> receivedTasks = new ArrayList<>();
            receivedTasks.add(task);
            this.winVar = new Variables(this.vehicleList, receivedTasks);
            winVar.selectInitialSolution();

            this.marginalCost = winVar.costFunction();
        }
        else{

            extendedVar = this.currentVariables.copy();

            extendedVar.addTask(task);

            extendedVar.selectInitialSolution();

            long time_start = System.currentTimeMillis();

            extendedVar = SLS(extendedVar, this.prob, this.lookIter,
                    time_start, this.timeoutPlanLook, Double.POSITIVE_INFINITY);
            this.marginalCost = extendedVar.costFunction() - this.currentCost; //TODO store cost int var -> no need to recall costFunction()

            this.winVar = extendedVar;
        }
        long a = marginalCost + margin;

        return marginalCost + margin;
    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long time_start = System.currentTimeMillis();
        boolean demo = false;
        System.out.println("return plan");

        //Remap plan with the new taskSet
        for(Task newT:tasks){
            for(PUDTask oldT:this.currentVariables.PUDTaskSet){
                if(newT.id == oldT.task.id){
                    oldT.task = newT;
                }
            }
        }

        return createPlan(this.currentVariables, vehicles, tasks);
    }

    //********** AUX FUNCTIONS ***********//

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
            //System.out.println("Proposed " + N.size() + " neighbors");
            var = var.LocalChoice(N, prob);
            if (var.BestCost >= absoluteBestCost) {
                if (var.localChoiceBool) {
                    NoImprovement++;
                }
              //  System.out.println("NO IMPROVEMENT: " + NoImprovement);
            } else {
                absoluteBestCost = var.BestCost;
                NoImprovement = 0;
                BestChoice = var;
              //  System.out.println("IMPROVEMENT: ");
            }
            //System.out.println("BEST COST " + var.BestCost);
            //System.out.println("ABSOLUTE BEST COST " + absoluteBestCost);

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
            if(A == null){
                multiVPlan.add(plan);
                continue;
            }

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
