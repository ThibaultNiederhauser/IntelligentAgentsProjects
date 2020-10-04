
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.IntStream;
import java.util.*;


import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class ReactiveAgent implements ReactiveBehavior {

	private static final int LEAVE = 0;
	private static final int TAKE = 1;
	private static final double tol = 0.99;

	private int numActions;
	private Agent myAgent;

	List<State> statesList = new ArrayList<State>();
	private HashMap<State, Double> v_values = new HashMap<State, Double>();
	private HashMap<State, double[]> q_values = new HashMap<State, double[]>();

	double task_taken = 0;
	double task_not_taken = 0;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);
		for(City city : topology)
			statesList.add(new State(city));

		List<City> tasks = new ArrayList<Topology.City>(topology.cities());

		int[] actions = {LEAVE, TAKE};

		tasks.add(null); // The "no task" state

		this.numActions = 0;
		this.myAgent = agent;
		init_vals(topology);

		// training
		boolean done = false;
		int iter_count = 0;
		float km_cost = agent.vehicles().get(0).costPerKm();

		while (!done){
			done = true;
			System.out.println("Iter_count: " + iter_count++);
			for (State from : statesList) {  // for all cities
				for (int act : actions) {   // action of leave and action of take
					double q = 0;
					City currentCity = from.getCity();

					if (act == LEAVE) {
						double max_val = 0;
						for(City neighbor : currentCity.neighbors()){
							// mean neighbors policy
							// q += discount*v_values.get(city2State(neighbor))/(currentCity.neighbors().size());
							// max policy
							double val = v_values.get(city2State(neighbor));
							double cost =  neighbor.distanceTo(currentCity)*km_cost;

							if ( discount*val - cost > max_val)
								max_val = discount*val - cost;
							// TODO: soft max policy
						}
						q += max_val;
					} else {
						for (City to : tasks) {  // for all the cities
							if (to == null || to.equals(from.getCity()))	continue;
							q += td.probability(from.getCity(),to)
									*(td.reward(from.getCity(), to) - to.distanceTo(currentCity)*km_cost
									+ discount*v_values.get(city2State(to)));
							// topology size is wrong
						}
					}
					update_q_value(from, act, q);
					}

				// was above but better here
				Double r = update_v_value(from);
				if (r < tol){
					done = false;

				}
			}
		}
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;

		Double discount = (double)1;

		if (availableTask == null) {
			City currentCity = vehicle.getCurrentCity();
			City to = get_best_neighbors(currentCity, vehicle.costPerKm());
			action = new Move(to);
		} else {
			City currentCity = vehicle.getCurrentCity();
			City to_yes = availableTask.deliveryCity;
			City to_not = get_best_neighbors(currentCity, vehicle.costPerKm());

			double valPickup = availableTask.reward
					// - currentCity.distanceTo(to_yes)*vehicle.costPerKm()
					+ discount*v_values.get(city2State(to_yes));

			double valDont = discount*v_values.get(city2State(to_not));
					// - currentCity.distanceTo(to_not)*vehicle.costPerKm();

			if (valPickup > valDont){
				action = new Pickup(availableTask);
				task_taken++;
			}
			else{
				action = new Move(to_not);
				task_not_taken++;
			}
		}
		
		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
			double tot = task_taken+task_not_taken;
			double r = task_taken/tot;
			System.out.println("Ratio task taken: " + r + " Total tasks: " + tot);
		}
		numActions++;
		
		return action;
	}

	private void init_vals(Topology topology){
		for (State s : statesList){
			v_values.put(s, (double) 0);
			q_values.put(s, new double[2]);
		}
	}

	private void update_q_value(State c, int act, double q){
		double[] new_q = q_values.get(c);
		new_q[act] = q;
		q_values.put(c, new_q);
	}
	private Double update_v_value(State c){
		Double max_q = Arrays.stream(q_values.get(c)).max().getAsDouble();
		double r1 = (v_values.get(c) + 0.01) / (max_q+0.01);
		double r2 =  (max_q+0.01)/(v_values.get(c) + 0.01);
		v_values.put(c, max_q);
		return Math.min(r1, r2);
	}

	private State city2State(City c){
		for (State state : statesList){
			if (state.getCity() == c){
				return state;
			}
		}
		return null;
	}

	private City get_best_neighbors(City currentCity, int cost){
		double best = 0;
		City best_city = null;
		for (City neigh : currentCity.neighbors()){
			double v = v_values.get(city2State(neigh)); //- neigh.distanceTo(currentCity)*cost;
			if (v>best){
				best = v;
				best_city = neigh;
			}
		}
		return best_city;
	}
}
