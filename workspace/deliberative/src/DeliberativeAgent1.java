import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.*;

/**
 * An optimal planner for one vehicle.
 */

public class DeliberativeAgent1 implements DeliberativeBehavior {

	enum Algorithm { BFS, ASTAR, NAIVE }
	
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
		String algorithmName = agent.readProperty("algorithm", String.class, "NAIVE");
		
		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());
		
		// ...
	}
	
	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan = null;

		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			try {
				plan = aStarPlan(vehicle, tasks);
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		case BFS:
			try {
				plan = bfsPlan(vehicle, tasks);
			} catch (Exception e) {
				throw new AssertionError("Should not happen.");
			}
			break;
		case NAIVE:
			plan = naivePlan(vehicle, tasks);
			break;
		default:
			throw new AssertionError("Should not happen.");
		}
		int DEBUG = 1;
		if (DEBUG == 1) {
			try {
				Plan plan1 = aStarPlan(vehicle, tasks);
				Plan plan2 = bfsPlan(vehicle, tasks);
				System.out.println("bring bring bring");
			} catch (Exception e) {
				e.printStackTrace();
			}
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

	private Plan aStarPlan(Vehicle vehicle, TaskSet tasks) throws Exception {
		int QUEUESIZE = 1000;
		System.out.println("Astar strarting");

		ArrayList<State> C = new ArrayList<>();

		Comparator<State> compareByCost = Comparator.comparing(State::getCost);
		PriorityQueue<State> Q = new PriorityQueue(QUEUESIZE, compareByCost);

		Q.add(new State(vehicle, tasks));

		while (true) {
			if (Q.isEmpty()) { throw new AssertionError("Solution not found"); }

			State n = Q.poll();

			if (n.isFinal()) { return n.getPlan(vehicle); }

			if(checkNovelty(n, C)){
				C.add(n);
				List<State> S = n.getAccessibles();
				Q.addAll(S);
			}
		}
	}
	
		
	private Plan bfsPlan(Vehicle vehicle, TaskSet tasks) throws Exception {
		System.out.println("BFS strarting");
		State start = new State(vehicle, tasks);

		Queue<State> Q = new LinkedList<>();
		List<State> visitedList = new ArrayList<>();
		Q.add(start);

		double bestCost = Double.POSITIVE_INFINITY;
		State bestState = start;

		while(!Q.isEmpty()){
			State newState = Q.poll();
			List<State> proposal = newState.getAccessibles();
			for (State s : proposal) {
				if (s.isFinal()){
					if (s.getCost() < bestCost){
						bestState = s;
						bestCost = s.getCost();
					}
				}
				else if (checkNovelty(s, visitedList)){
					Q.add(s);
					visitedList.add(s);
				}
			}
		}
		System.out.println("BFS over");
		return bestState.getPlan(vehicle);
	}

	@Override
	public void planCancelled(TaskSet carriedTasks) {
		
		if (!carriedTasks.isEmpty()) {

		}
	}

	public boolean checkNovelty(State s, List<State> visitedStates){
		for (State v : visitedStates){
			if (!v.isNovelty(s))
				return false;
		}
		return true;
	}
}
