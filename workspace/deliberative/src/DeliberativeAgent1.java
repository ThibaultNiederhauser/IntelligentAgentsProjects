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
	}
	
	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan = null;

		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			try {
				long startTime = System.currentTimeMillis();
				plan = aStarPlan(vehicle, tasks);
				long elapsedTime = System.currentTimeMillis() - startTime;
				System.out.println("Elapsed time: " + elapsedTime);
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		case BFS:
			try {
				long startTime = System.currentTimeMillis();
				plan = bfsPlan(vehicle, tasks);
				long elapsedTime = System.currentTimeMillis() - startTime;
				System.out.println("Elapsed time: " + elapsedTime);

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

		int DEBUG = 0;
		if (DEBUG == 1) {
			try {
				Plan plan1 = aStarPlan(vehicle, tasks);
				Plan plan2 = bfsPlan(vehicle, tasks);
				System.out.println("plan comparison");
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
		/** Compute optimal plan using A-star**/

		int QUEUESIZE = 1000;
		ArrayList<State> C = new ArrayList<>();
		//Declare Q sorted by heuristic function f(n):
		Comparator<State> compareByPriority = Comparator.comparing(State::getCost);
		PriorityQueue<State> Q = new PriorityQueue(QUEUESIZE, compareByPriority);

		//A-star algorithm
		System.out.println("Astar starting");
		Q.add(new State(vehicle, tasks));

		while (true) {
			if (Q.isEmpty()) { throw new AssertionError("Solution not found"); }

			State n = Q.poll();

			if (n.isFinal()) {
				System.out.println("A-star over");
				return n.getPlan(vehicle);
			}

			//consider n only if it was never seen
			// or if it has a lower cost than the already seen identical state
			if(checkNovelty(n, C)){
				C.add(n);
				List<State> S = n.getAccessibles();
				Q.addAll(S); //merge S and Q. Q is sorted wrt the heuristic fct
			}
		}
	}
	
		
	private Plan bfsPlan(Vehicle vehicle, TaskSet tasks) throws Exception {
		/** Compute optimal plan using Breadth First Search**/

		State start = new State(vehicle, tasks);
		Queue<State> Q = new LinkedList<>();
		List<State> visitedList = new ArrayList<>();

		//BFS Algorithm
		System.out.println("BFS strarting");
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
		//Checks that state s was never seen or that it has a lower cost than the already
		//seen identical state
		for (State v : visitedStates){
			if (!v.isNovelty(s))
				return false;
		}
		return true;
	}
}
