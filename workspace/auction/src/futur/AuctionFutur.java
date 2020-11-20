package futur;
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
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 *
 */
@SuppressWarnings("unused")
public class AuctionFutur implements AuctionBehavior {

    private final double futurProba = 0.6;

    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private Random random;
    private List<Vehicle> vehicleList;

    private City currentCity;
    private double marginalCost;
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
        this.currentVariables = null;

        this.prob =  Double.parseDouble(this.agent.readProperty("prob", String.class, "1"));
        this.lookIter =  Integer.parseInt(this.agent.readProperty("lookIter", String.class, "10000"));
    }

    @Override
    public void auctionResult(Task previous, int winner, Long[] bids) {
        if (winner == agent.id()) {
           this.currentVariables = winVar;
        }
        System.out.println("auction results");
    }

    @Override
    public Long askPrice(Task task) {
        double bid = 0;
        Task predictedTask;
        City deliverCity = task.deliveryCity;
        double predictedMarginalCost;
        double assocProba;
        int id = 100; //doesn't really matter
        Variables predictedVar = null;
        double TrueMarginal = 0;
        ArrayList<Double> probas = new ArrayList<>();
        ArrayList<Double> marginals = new ArrayList<>();
        int i = 0;


        boolean tooHeavy = true;
        int margin = 10;

        if(this.currentVariables != null) {
            //TrueMarginal = computeMarginalCost(task, this.currentVariables);
        }


        //Check that task can be carried
        for(Vehicle v : this.vehicleList){
            if(v.capacity() >= task.weight){
                tooHeavy = false;
            }
        }
        if(tooHeavy){return null;}

        //COMPUTE BEST PLAN IF Win
        if(this.currentVariables == null){
            ArrayList<Task> receivedTasks = new ArrayList<>();
            receivedTasks.add(task);
            this.winVar = new Variables(this.vehicleList, receivedTasks);
        }
        else{
            this.winVar = this.currentVariables.copy();
            this.winVar.addTask(task);
            long time_start = System.currentTimeMillis();
            this.winVar.selectInitialSolution();
            this.winVar = SLS(this.winVar, this.prob, this.lookIter, time_start, this.timeoutPlanLook,
                    Double.POSITIVE_INFINITY); //TODO timeOUt not correct
            //TODO take this into account for bid computing or put in PLan fct
        }

        //COMPUTE BID
        for(City from:topology) {
            for (City to : topology) {
                if (from == to) {
                    continue;
                }

                predictedTask = new Task(id, from, to, 0, distribution.weight(from, to));
                if (this.currentVariables != null) {
                    predictedVar = this.currentVariables.copy();
                    predictedVar.addTask(predictedTask);
                } else {
                    ArrayList<Task> receivedTasks = new ArrayList<>();
                    receivedTasks.add(predictedTask);
                    predictedVar = new Variables(this.vehicleList, receivedTasks);
                }

                //.println("predicting for task nb: " + id);
                predictedVar.selectInitialSolution();
                predictedMarginalCost = computeMarginalCost(task, predictedVar);
                assocProba = this.distribution.probability(from, to)/topology.size();

                if(probas.size() <= 0){
                    probas.add(assocProba);
                    marginals.add(predictedMarginalCost);
                }
                else{
                    //find value
                    i = 0;
                    while(i<marginals.size()){
                        if(predictedMarginalCost <= marginals.get(i)){
                            break;
                        }
                        i++;
                    }
                    // shift second half
                    marginals.add(marginals.get(marginals.size()-1));
                    probas.add(probas.get(probas.size()-1));

                    for(int j = marginals.size() - 2; j>=i; j--) {
                        marginals.set(j + 1, marginals.get(j));
                        probas.set(j+1, probas.get(j));
                    }

                    //insert new value
                    marginals.add(i, predictedMarginalCost);
                    probas.add(i, assocProba);
                }



                //bid += assocProba * predictedMarginalCost;

                id += 1;
            }
        }

        double cumulProba = 0;
        for(int k = marginals.size()-1; k >= 0; k--){
            cumulProba += probas.get(k);
            if(cumulProba > (1- this.futurProba)){
                bid = marginals.get(k);
                break;
            }
        }


        System.out.println("bid futur: " + (long) Math.round(bid + margin));

        return (long) Math.round(bid + margin);
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


    private Double computeMarginalCost(Task task, Variables var){
        double marginalCost = 0;
        Variables extendedVar = null;
        //compute marginal cost
        /**if(var == null){
            ArrayList<Task> receivedTasks = new ArrayList<>();
            receivedTasks.add(task);
            extendedVar = new Variables(this.vehicleList, receivedTasks);
            extendedVar.selectInitialSolution();

            marginalCost = extendedVar.costFunction();
        }**/
        //else {


        extendedVar = var.copy();
        extendedVar.addTask(task);

        long time_start = System.currentTimeMillis();
        var = SLS(var, this.prob, this.lookIter,
                time_start, this.timeoutPlanLook, Double.POSITIVE_INFINITY);

        extendedVar.selectInitialSolution();
        time_start = System.currentTimeMillis();
        extendedVar = SLS(extendedVar, this.prob, this.lookIter,
                time_start, this.timeoutPlanLook, Double.POSITIVE_INFINITY);
        marginalCost = extendedVar.costFunction() - var.costFunction(); //TODO store cost int var -> no need to recall costFunction()
       //}

        return marginalCost;
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
        Plan plan ;

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
