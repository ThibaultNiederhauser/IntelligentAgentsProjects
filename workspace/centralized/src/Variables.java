import logist.task.Task;
import logist.simulation.Vehicle;
import logist.task.TaskSet;



import java.util.ArrayList;
import java.util.List;


public class Variables {
    private ArrayList<Task> nextTask = new ArrayList<>();
    private ArrayList<Integer> time = new ArrayList<>();
    private ArrayList<Vehicle> vehicle = new ArrayList<>();
    private int numTasks;
    private int numVehicles;

    public void setNextTask(TaskSet set){
        int i = 0;
        for(Task t : set){
            nextTask.set(i, t);
            i++;
        }
    }

    public void setTime(List<Integer> time_input){
        int i = 0;
        for(int t: time_input){
            time.set(i, t);
            i++;
        }
    }

    public void setVehicle(List<Vehicle> vehicle_input){
        int i = 0;
        for(Vehicle v: vehicle_input) {
            vehicle.set(i, v);
            i++;
        }
    }

    private void initNextTask(int nT, int nV){
        for(int i = 0; i<(nT+nV); i++ ){
            nextTask.add(null);
        }
    }

    private void initVehicle(int nT){
        for(int i = 0; i<(nT); i++ ){
            vehicle.add(null);
        }
    }

    private void initTime(int nT){
        for(int i = 0; i<(nT); i++ ){
            time.add(null);
        }
    }

    public void initVariables(int nT, int nV){
        initNextTask(nT, nV);
        initVehicle(nT);
        initTime(nT);

        numTasks = nT;
        numVehicles = nV;
    }

    public void selectInitialSolution(List<Vehicle> vehicle_list, TaskSet tasks){
        //find vehicle with biggest capacity
        Vehicle biggestVehicle = vehicle_list.get(0);
        for(Vehicle v: vehicle_list){
            if(v.capacity() > biggestVehicle.capacity()){
                biggestVehicle = v;
            }
        }

        //set variables
        int i = 0;
        for(Task t: tasks){
            if(t.id == 0){ //TODO maybe use a list of tasks instead of set, to use get()
                nextTask.set(biggestVehicle.id() + numTasks, t); //set nextTask(vk)
            }


            if(t.weight > biggestVehicle.capacity()) {throw new AssertionError
                    ("Problem unsolvable!");}

            //set nextTask(ti)
            nextTask.set(i, t);

            //set vehicle
            vehicle.set(i, biggestVehicle);

            //set time
            time.set(i,(i+1));
            i++;
        }
    }

}
