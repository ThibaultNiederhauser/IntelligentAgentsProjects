import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.List;

public class State implements Cloneable {
    public City location;
    private TaskSet pick_tasks;
    private TaskSet deliv_tasks;
    private int capacity;
    private int tmp_capacity;
    private double costPerKm;
    private List<Action> actionTrace = new ArrayList<Action>();
    private double total_cost;

    public State(Vehicle vehicle, TaskSet pick_tasks){
        this.location = vehicle.getCurrentCity();
        this.pick_tasks = pick_tasks.clone();
        this.deliv_tasks = vehicle.getCurrentTasks().clone();
        this.capacity = vehicle.capacity();
        this.costPerKm = vehicle.costPerKm();
        this.total_cost = 0;
        this.actionTrace = new ArrayList<>();
    }


    public State(TaskSet pick_tasks, TaskSet deliv_tasks, City city, double total_cost,
                    List<Action> actionTrace){
        this.location = city;
        this.pick_tasks = pick_tasks;
        this.deliv_tasks = deliv_tasks;
        this.total_cost = total_cost;
        this.actionTrace = actionTrace;
    }


    private List<Action> getTrivialActions() {
        // Check instantaneous tasks
        tmp_capacity = capacity;
        List<Action> actions = new ArrayList<Action>();
        for (Task task : deliv_tasks) {
            if (task.deliveryCity == this.location) {
                actions.add(new Action(Action.Type.DELIVER, task));
                tmp_capacity += task.weight;
            }
        }
        return actions;
    }

    private List<Action> getPossibleActions() {

        List<Action> actions = new ArrayList<Action>();
        for (Task task : deliv_tasks) {
            if (task.deliveryCity != this.location) {
                actions.add(new Action(Action.Type.DELIVER, task));
            }
        }
        for (Task task : pick_tasks) {
            if (task.weight <= this.tmp_capacity) {
                actions.add(new Action(Action.Type.PICKUP, task));
            }
        }
        return actions;
    }

    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }

    private State cloneState(State s) throws CloneNotSupportedException {
        State returnState = (State) s.clone();
        returnState.deliv_tasks = s.deliv_tasks.clone();
        returnState.pick_tasks = s.pick_tasks.clone();
        returnState.actionTrace = new ArrayList<Action>(s.actionTrace);
        return returnState;
    }

    public List<State> getAccessibles() throws CloneNotSupportedException {
        List<Action> trivialActions = getTrivialActions();
        List<Action> actions = getPossibleActions();
        List<State> accessibles = new ArrayList<>();

        // releasing task in town

        State trivialState = cloneState(this);

        for (Action action : trivialActions) {
            if (action.type == Action.Type.DELIVER) {
                trivialState.capacity += action.task.weight;
                trivialState.deliv_tasks.remove(action.task);
                trivialState.actionTrace.add(action);
            }
        }

        for (Action action : actions) {
            // creating new tasks
            State newState = cloneState(trivialState);

            if (action.type == Action.Type.DELIVER) {
                newState.location = action.task.deliveryCity;
                newState.deliv_tasks.remove(action.task);
                newState.capacity += action.task.weight;
                newState.actionTrace.add(action);
                newState.total_cost += this.location.distanceTo(newState.location)*this.costPerKm;
            } else if (action.type == Action.Type.PICKUP) {
                newState.pick_tasks.remove(action.task);
                newState.deliv_tasks.add(action.task);
                newState.location = action.task.pickupCity;
                newState.capacity -= action.task.weight;
                newState.actionTrace.add(action);
                newState.total_cost += this.location.distanceTo(newState.location)*this.costPerKm;
            }

            accessibles.add(newState);
        }
        return accessibles;
    }

    public boolean isNovelty(State s){
       if (s.location != this.location)
           return true;
       if (!s.pick_tasks.equals(this.pick_tasks))
           return true;
       if (!s.deliv_tasks.equals(this.deliv_tasks))
           return true;
       if (s.total_cost < this.total_cost)
           return true;
       return false;
    }

    public boolean isFinal(){
        return deliv_tasks.isEmpty() && pick_tasks.isEmpty();
    }

    public List<Action>getActions(){
        return this.actionTrace;
    }

    public Plan getPlan(Vehicle v) throws Exception {
        City current = v.getCurrentCity();
        Plan plan = new Plan(current);

        for (Action action : this.actionTrace){
            City movingTo;
            if (action.type == Action.Type.DELIVER) {
                movingTo = action.task.deliveryCity;
            } else if (action.type == Action.Type.PICKUP) {
                movingTo = action.task.pickupCity;
            } else {
                throw new Exception("You shouldn't be there");
            }

            for (City city : current.pathTo(movingTo))
                plan.appendMove(city);

            current = movingTo;

            if (action.type == Action.Type.DELIVER) {
                plan.appendDelivery(action.task);
            } else {
                plan.appendPickup(action.task);
            }
        }
        return plan;
    }

    public Double getCost(){
        return this.total_cost;
    }

}


