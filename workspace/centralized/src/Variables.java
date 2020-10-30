import logist.task.Task;
import logist.simulation.Vehicle;
import logist.task.TaskSet;

import java.util.*;

public class Variables implements Cloneable{
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

    public List<Variables> chooseNeighbour(List<Vehicle> vehicle_list) {
        Random rand = new Random();
        Variables oldA = this;
        Variables A;
        List<Variables> N = new ArrayList<>();
        Task t = null;

        //select changing vehicule
        Vehicle v_i = vehicle_list.get(rand.nextInt(vehicle_list.size()));
        while (oldA.nextTaskV.get(v_i) == null) {
            v_i = vehicle_list.get(rand.nextInt(vehicle_list.size()));
        }

        //CHANGE vehicle
        for (Vehicle v_j : vehicle_list) {
            if (v_j.equals(v_i)) {
                continue;
            }
            t = oldA.nextTaskV.get(v_i);
            if(t == null){continue;}

            if (t.weight <= v_j.capacity()) {
               A = changingVehicle(oldA, v_i, v_j);
               N.add(A);
            }
        }

        //CHANGE task
        int length = 0;
        t = oldA.nextTaskV.get(v_i);
        while(t != null){
            t = oldA.nextTaskT.get(t);
            length++;
        }
        if(length>=2){
            for(int tIdx1 = 1; tIdx1 < length; tIdx1++){
                for(int tIdx2 = tIdx1; tIdx2 < tIdx1 + length; tIdx2++){
                    A = changingTaskOrder(oldA, v_i, tIdx1, tIdx2);
                    N.add(A);
                }
            }
        }

        return N;
    }

    private Variables copy(){
        Variables A_copy = new Variables();
        A_copy.time = this.time;
        A_copy.vehicle = this.vehicle;
        A_copy.nextTaskV = this.nextTaskV;
        A_copy.nextTaskT = this.nextTaskT;
        return A_copy;
    }

    private Variables changingVehicle(Variables A, Vehicle v1, Vehicle v2) {
        Variables A1 = A.copy();
        //TODO check cloning

        Task t = A.nextTaskV.get(v1);
        A1.nextTaskV.put(v1, A1.nextTaskT.get(t));
        A1.nextTaskT.put(t, A1.nextTaskV.get(v2));
        A1.nextTaskV.put(v2, t);
        updateTime(A1, v1);
        updateTime(A1, v2); //TODO check if A1 is modified correctly
        A1.vehicle.put(t, v2);
        return A1;
    }


    private void updateTime(Variables A, Vehicle v){
        Task t = A.nextTaskV.get(v);
        Task t_j;
        if(t != null){
            A.time.put(t, 1);
            do {
                t_j = A.nextTaskT.get(t);
                if (t_j != null) {
                    A.time.put(t_j, A.time.get(t) + 1);
                    t = t_j;
                }
            } while (t_j != null);
        }
    }

    private Variables changingTaskOrder(Variables A, Vehicle v, int tIdx1, int tIdx2){
        Variables A1 = A.copy();
        Task t1 = A1.nextTaskV.get(v);
        Task tPre1 = t1; //TODO this is WRONG

        int count = 1;

        while(count < tIdx1){
            tPre1 = t1;
            t1 = A1.nextTaskT.get(t1);
            count++;
        }

        Task tPost1 = A1.nextTaskT.get(t1);
        Task tPre2 = t1;
        Task t2 = A1.nextTaskT.get(tPre2);
        count++;

        while(count < tIdx2){ //TODO function for the while loop (comes twice)
            tPre2 = t2;
            t2 = A1.nextTaskT.get(t2);
            count++;

        }
        Task tPost2 = A1.nextTaskT.get(t2);

        //EXCHANGING 2 Tasks
        if(tPost1 == t2){ //TODO check equality works
           A1.nextTaskT.put(tPre1, t2);
           A1.nextTaskT.put(t2, t1);
           A1.nextTaskT.put(t1, tPost2);
        }
        else{
            A1.nextTaskT.put(tPre1, t2);
            A1.nextTaskT.put(tPre2, t1);
            A1.nextTaskT.put(t2, tPost1);
            A1.nextTaskT.put(t1, tPost2);
        }
        updateTime(A1, v);
        return A1;
    }

}
