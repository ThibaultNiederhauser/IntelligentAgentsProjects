import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.List;

public class State implements Cloneable {
    private City location;
    private TaskSet pick_tasks;
    private TaskSet deliv_tasks;
    private int capacity;
    private double costPerKm;
    private List<CustomAction> actionTrace = new ArrayList<CustomAction>();
    private double total_cost;

    public State(Vehicle vehicle, TaskSet pick_tasks){
        //Variables for state description
        this.location = vehicle.getCurrentCity();
        this.pick_tasks = pick_tasks.clone();
        this.deliv_tasks = vehicle.getCurrentTasks().clone();

        //Handy variables
        this.capacity = vehicle.capacity();
        this.costPerKm = vehicle.costPerKm();
        this.total_cost = 0;
        this.actionTrace = new ArrayList<>();
    }

    public Double getCost(){
        return this.total_cost;
    }

    private List<CustomAction> getTrivialActions() {
        // Check instantaneous tasks
        List<CustomAction> actions = new ArrayList<CustomAction>();
        for (Task task : deliv_tasks) {
            if (task.deliveryCity == this.location) {
                actions.add(new CustomAction(CustomAction.Type.DELIVER, task));
                this.capacity += task.weight;
            }
        }
        return actions;
    }

    private List<CustomAction> getPossibleActions() {

        List<CustomAction> actions = new ArrayList<CustomAction>();
        for (Task task : deliv_tasks) {
            if (task.deliveryCity != this.location) {
                actions.add(new CustomAction(CustomAction.Type.DELIVER, task));
            }
        }
        for (Task task : pick_tasks) {
            if (task.weight <= this.capacity) {
                actions.add(new CustomAction(CustomAction.Type.PICKUP, task));
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
        returnState.actionTrace = new ArrayList<CustomAction>(s.actionTrace);
        return returnState;
    }

    public List<State> getAccessibles() throws CloneNotSupportedException {
        List<CustomAction> trivialActions = getTrivialActions();
        List<CustomAction> actions = getPossibleActions();
        List<State> accessibles = new ArrayList<>();

        //Releasing task in current town
        State trivialState = cloneState(this);
        for (CustomAction action : trivialActions) {
            if (action.type == CustomAction.Type.DELIVER) {
                // trivialState.capacity += action.task.weight; this was previously updated @line 36
                trivialState.deliv_tasks.remove(action.task);
                trivialState.actionTrace.add(action);
            }
        }

        //PickUp or Deliver Task in other town
        for (CustomAction action : actions) {
            // creating new tasks
            State newState = cloneState(trivialState);

            //Delivery
            if (action.type == CustomAction.Type.DELIVER) {
                newState.location = action.task.deliveryCity;
                newState.deliv_tasks.remove(action.task);
                newState.capacity += action.task.weight;
                newState.actionTrace.add(action);
                newState.total_cost += this.location.distanceTo(newState.location)*this.costPerKm;
            }
            //Pick Up
            else if (action.type == CustomAction.Type.PICKUP) {
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

    public Plan getPlan(Vehicle v) throws Exception {
        //Returns a plan based on the action trace

        City current = v.getCurrentCity();
        Plan plan = new Plan(current);

        for (CustomAction action : this.actionTrace){
            City movingTo;
            if (action.type == CustomAction.Type.DELIVER) {
                movingTo = action.task.deliveryCity;
            } else if (action.type == CustomAction.Type.PICKUP) {
                movingTo = action.task.pickupCity;
            } else {
                throw new Exception("You shouldn't be there");
            }

            for (City city : current.pathTo(movingTo))
                plan.appendMove(city);

            current = movingTo;

            if (action.type == CustomAction.Type.DELIVER) {
                plan.appendDelivery(action.task);
            } else {
                plan.appendPickup(action.task);
            }
        }
        return plan;
    }

    public Double getPriority(){
        double h = 0; //the heuristic
        double g = this.total_cost; //the cost
        double dist = 0;
        double newDist;

        if(!this.pick_tasks.isEmpty()) {
            for (Task task : this.pick_tasks) {
                newDist = task.pickupCity.distanceTo(this.location);
                newDist += task.deliveryCity.distanceTo(task.pickupCity);
                if(newDist > dist) {
                    dist = newDist;
                }
            }
        }

        if(!this.deliv_tasks.isEmpty()) {
            for (Task task : this.deliv_tasks) {
                newDist = task.deliveryCity.distanceTo(this.location);
                if (newDist > dist) {
                    dist = newDist;
                }
            }
        }

        h = dist*this.costPerKm;
        g = this.total_cost;

        return h+g;
    }
}


