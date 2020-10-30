import logist.task.Task;
import logist.simulation.Vehicle;
import logist.task.TaskSet;

import java.util.*;

public class Variables {
    private HashMap<Task, Task> nextTaskT = new HashMap<>();
    private HashMap<Vehicle, Task> nextTaskV = new HashMap<>();
    private HashMap<Task, Integer> time = new HashMap<>();
    private HashMap<Task, Vehicle> vehicle = new HashMap<>();

    /*public void setNextTask(TaskSet set){
        int i = 0;
        for(Task t : set){
            nextTaskT.put(t, t);
            i++;
        }
    }*/

    /*public void setTime(List<Integer> time_input){
        int i = 0;
        for(int t: time_input){
            time.set(i, t);
            i++;
        }
    }*/

    /*public void setVehicle(List<Vehicle> vehicle_input){
        int i = 0;
        for(Vehicle v: vehicle_input) {
            vehicle.set(i, v);
            i++;
        }
    }*/

    private void initNextTask(List<Vehicle> vehicles, TaskSet tasks){
        for(Task t: tasks){
            nextTaskT.put(t, null);
        }

        for(Vehicle v: vehicles){
            nextTaskV.put(v, null);
        }
    }

    private void initVehicle(TaskSet tasks){
        for(Task t: tasks){
            vehicle.put(t, null);
        }
    }

    private void initTime(TaskSet tasks){
        for(Task t: tasks){
            time.put(t, null);
        }
    }

    public void initVariables(List<Vehicle> vehicles, TaskSet tasks){
        initNextTask(vehicles, tasks);
        initVehicle(tasks);
        initTime(tasks);

    }

    public void selectInitialSolution(List<Vehicle> vehicle_list, TaskSet tasks){
        Iterator<Task> taskIterator = tasks.iterator();
        //find vehicle with biggest capacity
        Vehicle biggestVehicle = vehicle_list.get(0);
        for(Vehicle v: vehicle_list){
            if(v.capacity() > biggestVehicle.capacity()){
                biggestVehicle = v;
            }
        }

        //set variables
        if(taskIterator.hasNext()){
            nextTaskV.put(biggestVehicle, taskIterator.next()); //set nextTask(vk)}
        }
        int i = 0;
        for(Task t: tasks){
            if(t.weight > biggestVehicle.capacity()) {throw new AssertionError
                    ("Problem unsolvable!");}

            //set nextTask(ti)
            if(taskIterator.hasNext()) {
                nextTaskT.put(t, taskIterator.next());
            }

            //set vehicle
            vehicle.put(t, biggestVehicle);

            //set time
            time.put(t,(i+1));
            i++;
        }
    }

    /*public void chooseNeighbour(){
        Random rand = new Random();
        Variables oldA = this;
        while(oldA.nextTask(numTasks + ))
        Vehicle v_i = vehicle.get(rand.nextInt(numVehicles))
    }*/

}
