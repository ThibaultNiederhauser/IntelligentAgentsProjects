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

import java.util.*;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 */
@SuppressWarnings("unused")
public class AuctionFutur implements AuctionBehavior {

    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private List<Vehicle> vehicleList;

    long time_start;
    private double marginalCost;
    private double timeoutBid;
    private double timeoutPlanLook;
    private double timeoutPlanDig;
    private double prob;
    private double nodeResolution;
    private double nodeTrash;
    private int lookIter;

    private Variables currentVariables;
    private Variables winVar;

    private int auctionNumber = 0;
    private int margin = 150;
    private long revenue;

    private ArrayList<Task> taskHistory = new ArrayList<>();
    private HashMap<Task, Long> OpponentBidHistory = new HashMap<>();
    private HashMap<Task, Double> discounts = new HashMap<>();
    private long bid;
    private ArrayList<Task> OpponentTasks = new ArrayList<>();
    private int oppMaxCapacity = Integer.MAX_VALUE;


    //****PARAMETERS***//
    private final double percentageMargin = 0.1; //
    private final double loseDiscount = 0; //discount factors for bid prediction after opponent won a task
    private final double neighDiscount = 0.5; //discount of neighbouring city for bid pred
    private final double agressivityFactor = 0.6; // btw 0 and 1.


    @Override
    public void setup(Topology topology, TaskDistribution distribution,
                      Agent agent) {

        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;
        this.vehicleList = agent.vehicles();
        City currentCity = vehicleList.get(0).homeCity();

        this.timeoutBid = LogistPlatform.getSettings().get(LogistSettings.TimeoutKey.BID);
        this.timeoutPlanLook = LogistPlatform.getSettings().get(LogistSettings.TimeoutKey.PLAN) * 0.95;
        this.timeoutPlanDig = LogistPlatform.getSettings().get(LogistSettings.TimeoutKey.PLAN) * 0.05;

        long seed = -99983951L * currentCity.hashCode() * agent.id();
        Random random = new Random(seed);
        this.currentVariables = null;
        this.revenue = 0;

        this.prob = Double.parseDouble(this.agent.readProperty("prob", String.class, "1"));
        this.nodeResolution = Double.parseDouble(this.agent.readProperty("nodeResolution", String.class, "0.05"));
        this.nodeTrash = Double.parseDouble(this.agent.readProperty("nodeTrash", String.class, "0.005"));
        this.lookIter = Integer.parseInt(this.agent.readProperty("lookIter", String.class, "300"));
    }

    @Override
    public void auctionResult(Task previous, int winner, Long[] bids) {
        boolean oppBidsNull = true;

        System.out.println("bids"+ Arrays.toString(bids));

        //algo only for 1v1
        if(bids.length > 2){
            System.out.println("WARNING, more than one opponent! " +
                    "Agent not optimized for this configuration!");
        }

        //Update History
        this.discounts.put(previous, (double) 1); //update opponent's bids discount array
        taskHistory.add(previous);
        if(taskHistory.size() >= 5){this.margin = 10;}

        if (winner == agent.id()) {
            this.currentVariables = winVar;
            this.revenue+=bids[agent.id()];
            System.out.println("Vittoriaaa");

        }
        else{
            System.out.println("TASK LOST");
            this.OpponentTasks.add(previous);

            //update discounts
            for(Task t:taskHistory) {
                discounts.put(t, discounts.get(t) * this.loseDiscount);
            }
        }

        ///find opponent bid (considering 1v1)
        for(Long b:bids) {
            if (b!= null && b != this.bid){
                this.OpponentBidHistory.put(previous, b);
                oppBidsNull = false;
                break;
            }
        }

        //Estimate opponent max capacity
        if(oppBidsNull && previous.weight <= this.oppMaxCapacity){
            this.oppMaxCapacity = previous.weight - 1;
        }

        //if wrong assumption about null bid (not because unfeasible)
        if(!oppBidsNull && previous.weight > this.oppMaxCapacity){
            this.oppMaxCapacity = Integer.MAX_VALUE;
        }
        //System.out.println("auction results");
    }

    @Override
    public Long askPrice(Task task) {
        this.time_start = System.currentTimeMillis();
        this.auctionNumber++;

        City deliverCity = task.deliveryCity;

        //Check that task can be carried
        if (tooHeavy(vehicleList, task.weight)) {
            return null;
        }

        //Peculiar case: we think that the opponent will bid "null"
        if(task.weight > this.oppMaxCapacity){
            this.bid = Long.MAX_VALUE;
            computeWinVar(task);
            return this.bid;
        }


        //compute bid
        long marginal = this.computeSmartBidding(task);

        //compute margin based on opponents bids
        long OppMargin = computeMargin(task);

        long bid_val = Math.max(Math.max(marginal + this.margin, marginal + OppMargin),100);
        System.out.println("bidding: " + bid_val);
        this.bid = bid_val;
        return this.bid;
    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {

        long time_start = System.currentTimeMillis();
        boolean demo = false;
        System.out.println("return plan");

        //Remap plan with the new taskSet
        for (Task newT : tasks) {
            for (PUDTask oldT : this.currentVariables.PUDTaskSet) {
                if (newT.id == oldT.task.id) {
                    oldT.task = newT;
                }
            }
        }
        return createPlan(this.currentVariables, vehicles, tasks);
    }


    private long computeSmartBidding(Task task) {
        this.winVar = computeWinVar(task);
        long costBefore =  (this.isEmpty())? 0 : currentVariables.BestCost;
        Long startCost = this.winVar.costFunction();
        System.out.println(this.auctionNumber + " - start cost " + startCost + " cost without task " + costBefore + " money right now " + this.revenue);
        Node root = new Node(this.winVar, 1.0, 0, startCost);
        Queue<Node> Q = new LinkedList<>();
        Q.add(root);
        List<Node> toRank = new ArrayList<>();  // will have final leaves
        int id = 100;

        while (!Q.isEmpty()) {
            Node current = Q.poll();

            for (City from : topology) {
                for (City to : topology) {
                    if (from == to) {
                        continue;
                    }
                    double next_prob = this.distribution.probability(from, to)/topology.size() * current.prob;
                    if (next_prob < this.nodeTrash) continue;

                    Task next_task = new Task(id++, from, to, 0, distribution.weight(from, to));
                    Variables next_var = current.data.copy();

                    next_var.addTask(next_task);
                    next_var.selectInitialSolution();  // will be faster later
                    next_var = completeSLS(next_var, this.prob, this.lookIter, Double.POSITIVE_INFINITY);

                    Long cost = next_var.BestCost;

                    Node next_node = new Node(next_var, next_prob, current.deep + 1, cost);

                    if (next_node.prob < this.nodeResolution || next_node.deep > Math.max(1,6-this.auctionNumber)) {
                        toRank.add(next_node.light());  // version without variables
                    } else {
                        Q.add(next_node);
                    }
                }
            }
            // check time constraint
            if (System.currentTimeMillis() - this.time_start > this.timeoutBid*0.8){
                while(!Q.isEmpty()){
                    Node next_node = Q.poll();
                    toRank.add(next_node.light());
                }
            }
        }
        long finalMarginal = 0;
        double discoutfct =.75;
        double sumprob = 0;
        for (Node n : toRank) {
            finalMarginal += (double) (n.cost-costBefore) * n.prob / ((double) (1+n.deep) * discoutfct);
            sumprob += n.prob;
        }
        finalMarginal += (double) (startCost-costBefore) * (1-sumprob);
        System.out.println("Solve in time: " + (System.currentTimeMillis() - time_start)/1000   +  " explored "  + (int) (sumprob*100)+ "% probs");
        System.out.println("cost: " + finalMarginal);



        return finalMarginal;
    }

    private long computeMargin(Task task){
        //Computes margin based on opponents' bids

        double predictedBid = 0;
        double deltaBid = 0;
        double denominator = 0;
        double margin = 0;

        for(Task tHist:this.taskHistory){
            if(this.OpponentBidHistory.get(tHist) == null) { continue; }

            //if exact same task already seen
            if(tHist.deliveryCity == task.deliveryCity && tHist.pickupCity == task.pickupCity){
                predictedBid += this.OpponentBidHistory.get(tHist) * this.discounts.get(tHist);
                denominator += this.discounts.get(tHist);
                System.out.println("!!!task known !!!!!!");
            }
            //if task already seen with pickup or delivery city as neighbour
            else if((tHist.deliveryCity == task.deliveryCity &&
                    tHist.pickupCity.hasNeighbor(task.pickupCity)) ||
                    (tHist.deliveryCity.hasNeighbor(task.deliveryCity) &&
                            tHist.pickupCity == task.pickupCity)){

                predictedBid += this.OpponentBidHistory.get(tHist)
                        * this.discounts.get(tHist) * this.neighDiscount;
                denominator += this.discounts.get(tHist) * this.neighDiscount;
                System.out.println("!!!task known 1 N !!!!!!");

            }
            //if task already seen with both pick-up and delivery cities as neighbours
            else if(tHist.pickupCity.hasNeighbor(task.pickupCity) &&
                    tHist.deliveryCity.hasNeighbor(task.deliveryCity)){

                predictedBid += this.OpponentBidHistory.get(tHist)
                        * this.discounts.get(tHist) * this.neighDiscount * this.neighDiscount;
                denominator += this.discounts.get(tHist)*this.neighDiscount * this.neighDiscount;
                System.out.println("!!!task known 2 N!!!!!!");
            }
        }

        //if(formerTask != null){
        //predictedBid = this.OpponentBidHistory.get(formerTask);
        predictedBid = predictedBid/denominator;
        deltaBid = predictedBid - bid;
        if(deltaBid/bid > this.percentageMargin){
            margin = Math.round(deltaBid * this.agressivityFactor);
        }
        System.out.println("!!!new margin!!!!!!: " + margin);
        //}

        return (long) margin;
    }


    //********** AUX FUNCTIONS ***********//


    private void verboseOut(double bestCost, long time_start) {
        System.out.println("----RESULT----");
        System.out.println("Params: \niter:\t" + this.lookIter + "\np:\t" + this.prob);
        double elapsed_time = (System.currentTimeMillis() - time_start) / 1000.;
        System.out.println("Time (s):\t" + elapsed_time);
        System.out.println("Cost:\t" + bestCost);
        System.exit(0);
    }

    Variables computeWinVar(Task task) {
        if (this.isEmpty()) {
            ArrayList<Task> receivedTasks = new ArrayList<>();
            receivedTasks.add(task);
            this.winVar = new Variables(this.vehicleList, receivedTasks);
            this.winVar.selectInitialSolution();

        } else {
            this.winVar = this.currentVariables.copy();
            this.winVar.addTask(task);  // TODO optimize
            this.winVar.selectInitialSolution();
            this.winVar = completeSLS(this.winVar, this.prob, this.lookIter, Double.POSITIVE_INFINITY);
        }
        return this.winVar;
    }

    private Variables completeSLS(Variables var, double prob, int stopIter, double absoluteBestCost) {
        var = SLS(var, this.prob, this.lookIter, 0, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        var = SLS(var, 1, 1, 0, Double.POSITIVE_INFINITY, var.BestCost);
        return var;
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
            var = var.LocalChoice(N, prob);
            if (var.BestCost >= absoluteBestCost) {
                if (var.localChoiceBool) {
                    NoImprovement++;
                }
            } else {
                absoluteBestCost = var.BestCost;
                NoImprovement = 0;
                BestChoice = var;
            }
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
            if (A == null) {
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

    boolean isEmpty() {
        return this.currentVariables == null;
    }

    boolean tooHeavy(List<Vehicle> vehicleList, int weight) {
        for (Vehicle v : vehicleList) {
            if (v.capacity() >= weight) {
                return false;
            }
        }
        return true;
    }

}
