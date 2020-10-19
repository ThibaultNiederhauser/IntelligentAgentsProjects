import logist.task.Task;
import logist.topology.Topology.City;
import logist.task.TaskSet;
import logist.simulation.Vehicle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import logist.plan.Action;

public class StateDel {
    private City currentCity;
    private TaskSet availableTasks;
    private TaskSet vehicleTasks;
    private double cost;
    public List<Action> actionTrace;

    public StateDel(TaskSet availableTasks, TaskSet vehicleTasks, City city, double cost,
                    List<Action> actionTrace){
        this.currentCity = city;
        this.availableTasks = availableTasks;
        this.vehicleTasks = vehicleTasks;
        this.cost = cost;
        this.actionTrace = actionTrace;
    }

    public StateDel(Vehicle vehicle, TaskSet availableTasks){
        this.currentCity = vehicle.getCurrentCity();
        this.availableTasks = availableTasks;
        this.vehicleTasks = vehicle.getCurrentTasks();
        this.cost = 0;
        this.actionTrace = new ArrayList<>();
    }

    public TaskSet getAvailableTasks(){return this.availableTasks;}
    public TaskSet getVehicleTasks(){return this.vehicleTasks;}
    public City getCurrentCity(){return this.currentCity;}
    public Double getCost(){return this.cost;}

    //public City getCity(){return this.city;}
    //public void setCity(City city){this.city = city;}


    public boolean isInList(List<StateDel> stateList) {
        boolean bool = false;

        for(StateDel s: stateList){
            if(s.getCurrentCity() == this.currentCity &&
                    s.getVehicleTasks().equals(this.vehicleTasks) &&
                    s.getAvailableTasks().equals(this.availableTasks)
            ){bool = true;}

        }

        return bool;
    }

    public List<StateDel> getSuccessors(Vehicle vehicle){
        List<StateDel> successors = new ArrayList<StateDel>();
        int TotalWeight = 0;

        for(Task task:this.vehicleTasks){
            TotalWeight += task.weight;
        }

        //CHOSE TO PICK UP A TASK
        for(Task task : this.availableTasks) {
            if (vehicle.capacity() - TotalWeight - task.weight >= 0) { //check that vehicle has enough capacity

                TaskSet sucAvailableTasks = this.availableTasks.clone();
                sucAvailableTasks.remove(task);

                TaskSet sucVehicleTasks = this.vehicleTasks.clone();
                sucVehicleTasks.add(task);

                City sucCity = task.pickupCity;
                double sucCost = this.cost + this.currentCity.distanceTo(sucCity) * vehicle.costPerKm();

                //Update trace
                List<Action> sucActionTrace = new ArrayList<>(this.actionTrace);

                for (City city : this.currentCity.pathTo(sucCity)) {
                    Action move = new Action.Move(city);
                    sucActionTrace.add(move);
                }

                Action pickUp = new Action.Pickup(task);
                sucActionTrace.add(pickUp);


                StateDel sucState = new StateDel(sucAvailableTasks, sucVehicleTasks, sucCity, sucCost, sucActionTrace);
                successors.add(sucState);
            }
        }
        //CHOSE TO DELIVER A TASK
        for(Task task : this.vehicleTasks){
            TaskSet sucAvailableTasks = this.availableTasks.clone();

            TaskSet sucVehicleTasks = this.vehicleTasks.clone();
            sucVehicleTasks.remove(task);

            City sucCity = task.deliveryCity;

            double sucCost = this.cost + this.currentCity.distanceTo(sucCity);

            //Update trace
            List<Action> sucActionTrace = new ArrayList<>(this.actionTrace);

            for (City city : this.currentCity.pathTo(sucCity)) {
                Action move = new Action.Move(city);
                sucActionTrace.add(move);
            }
            Action deliver = new Action.Delivery(task);
            sucActionTrace.add(deliver);

            StateDel sucState = new StateDel(sucAvailableTasks, sucVehicleTasks, sucCity, sucCost,
                    sucActionTrace);
            successors.add(sucState);
        }

       return successors;
    }

}
