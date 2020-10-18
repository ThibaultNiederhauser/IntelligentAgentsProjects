
/* import table */
import logist.plan.Action;
import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.Comparator;

import java.util.Collections;
import java.util.List;

/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class DeliberativeAgent implements DeliberativeBehavior {

    enum Algorithm { BFS, ASTAR }

    /* Environment */
    Topology topology;
    TaskDistribution td;

    /* the properties of the agent */
    Agent agent;
    int capacity;

    /* the planning class */
    Algorithm algorithm;

    @Override
    public void setup(Topology topology, TaskDistribution td, Agent agent) {
        this.topology = topology;
        this.td = td;
        this.agent = agent;

        // initialize the planner
        int capacity = agent.vehicles().get(0).capacity();
        String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");

        // Throws IllegalArgumentException if algorithm is unknown
        algorithm = Algorithm.valueOf(algorithmName.toUpperCase());

        // ...
    }

    @Override
    public Plan plan(Vehicle vehicle, TaskSet tasks) {
        Plan plan;

        // Compute the plan with the selected algorithm.
        switch (algorithm) {
            case ASTAR:
                // ...
                plan = aStarPlan(vehicle, tasks);
                break;
            case BFS:
                // ...
                plan = naivePlan(vehicle, tasks);
                break;
            default:
                throw new AssertionError("Should not happen.");
        }
        return plan;
    }

    private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
        City current = vehicle.getCurrentCity();
        Plan plan = new Plan(current);

        for (Task task : tasks) {
            // move: current city => pickup location
            for (City city : current.pathTo(task.pickupCity))
                plan.appendMove(city);

            plan.appendPickup(task);

            // move: pickup location => delivery location
            for (City city : task.path())
                plan.appendMove(city);

            plan.appendDelivery(task);

            // set current city
            current = task.deliveryCity;
        }
        return plan;
    }

    private Plan aStarPlan(Vehicle vehicle, TaskSet tasks) {
        ArrayList<StateDel> Q = new ArrayList<StateDel>();
        StateDel n;
        Plan plan = new Plan(vehicle.homeCity());
        Comparator<StateDel> compareByCost = (StateDel s1, StateDel s2)
                -> s1.getCost().compareTo( s2.getCost());
        ArrayList<StateDel> C = new ArrayList<>();
        List<StateDel> S;


        Q.add(new StateDel(vehicle, tasks));

        while (true) {
            if (Q.isEmpty()) {
                throw new AssertionError("Q is empty");
            }

            n = Q.get(0);
            Q.remove(0);

            if (n.getAvailableTasks().isEmpty() && n.getVehicleTasks().isEmpty()) {
                return retrievePlan(n, vehicle);
            }

            if(!n.isInList(C)){ //TODO add lower cost
                System.out.println("HERE");

                C.add(n);
                S = n.getSuccessors(vehicle);
                S.sort(compareByCost); //TODO compare by heuristic: here h(n) = 0;
                Q.addAll(S);
            }
        }
    }

    @Override
    public void planCancelled(TaskSet carriedTasks) {

        if (!carriedTasks.isEmpty()) {
            // This cannot happen for this simple agent, but typically
            // you will need to consider the carriedTasks when the next
            // plan is computed.
        }
    }

    private Plan retrievePlan(StateDel state, Vehicle vehicle){
        Plan plan = new Plan(vehicle.homeCity());

        for(Action a : state.actionTrace){
            plan.append(a);
        }
        return plan;
    }

}
