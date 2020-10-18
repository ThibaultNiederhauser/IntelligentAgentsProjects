import logist.plan.Action;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.List;

public class StateDel {
    private City location;
    private TaskSet pick_tasks;
    private TaskSet deliv_tasks;
    private double total_cost;
    public List<Action> actionTrace;

    public StateDel(TaskSet pick_tasks, TaskSet deliv_tasks, City city, double total_cost,
                    List<Action> actionTrace){
        this.location = city;
        this.pick_tasks = pick_tasks;
        this.deliv_tasks = deliv_tasks;
        this.total_cost = total_cost;
        this.actionTrace = actionTrace;
    }

    public StateDel(Vehicle vehicle, TaskSet pick_tasks){
        this.location = vehicle.getlocation();
        this.pick_tasks = pick_tasks;
        this.deliv_tasks = vehicle.getCurrentTasks();
        this.total_cost = 0;
        this.actionTrace = new ArrayList<>();
    }

    public TaskSet getpick_tasks(){return this.pick_tasks;}
    public TaskSet getdeliv_tasks(){return this.deliv_tasks;}
    public City getlocation(){return this.location;}
    public Double gettotal_cost(){return this.total_cost;}

    //public City getCity(){return this.city;}
    //public void setCity(City city){this.city = city;}


    public boolean isInList(List<StateDel> stateList) {
        boolean bool = false;

        for(StateDel s: stateList){
            if(s.getlocation() == this.location &&
                    s.getdeliv_tasks().equals(this.deliv_tasks) &&
                    s.getpick_tasks().equals(this.pick_tasks)
            ){bool = true;}

        }

        return bool;
    }

    public List<StateDel> getSuccessors(Vehicle vehicle){
        List<StateDel> successors = new ArrayList<StateDel>();
        int TotalWeight = 0;

        for(Task task:this.deliv_tasks){
            TotalWeight += task.weight;
        }

        //CHOSE TO PICK UP A TASK
        for(Task task : this.pick_tasks) {
            if (vehicle.capacity() - TotalWeight - task.weight >= 0) { //check that vehicle has enough capacity

                TaskSet sucpick_tasks = this.pick_tasks.clone();
                sucpick_tasks.remove(task);

                TaskSet sucdeliv_tasks = this.deliv_tasks.clone();
                sucdeliv_tasks.add(task);

                City sucCity = task.pickupCity;
                double suctotal_cost = this.total_cost + this.location.distanceTo(sucCity) * vehicle.total_costPerKm();

                //Update trace
                List<Action> sucActionTrace = new ArrayList<>(this.actionTrace);

                for (City city : this.location.pathTo(sucCity)) {
                    Action move = new Action.Move(city);
                    sucActionTrace.add(move);
                }

                Action pickUp = new Action.Pickup(task);
                sucActionTrace.add(pickUp);


                StateDel sucState = new StateDel(sucpick_tasks, sucdeliv_tasks, sucCity, suctotal_cost, sucActionTrace);
                successors.add(sucState);
            }
        }
        //CHOSE TO DELIVER A TASK
        for(Task task : this.deliv_tasks){
            TaskSet sucpick_tasks = this.pick_tasks.clone();

            TaskSet sucdeliv_tasks = this.deliv_tasks.clone();
            sucdeliv_tasks.remove(task);

            City sucCity = task.deliveryCity;

            double suctotal_cost = this.total_cost + this.location.distanceTo(sucCity);

            //Update trace
            List<Action> sucActionTrace = new ArrayList<>(this.actionTrace);

            for (City city : this.location.pathTo(sucCity)) {
                Action move = new Action.Move(city);
                sucActionTrace.add(move);
            }
            Action deliver = new Action.Delivery(task);
            sucActionTrace.add(deliver);

            StateDel sucState = new StateDel(sucpick_tasks, sucdeliv_tasks, sucCity, suctotal_cost,
                    sucActionTrace);
            successors.add(sucState);
        }

       return successors;
    }

}
