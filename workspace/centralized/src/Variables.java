import logist.task.Task;
import logist.simulation.Vehicle;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.*;

public class Variables implements Cloneable{
    public HashMap<PUDTask, PUDTask> nextTaskT = new HashMap<>();
    public HashMap<Vehicle, PUDTask> nextTaskV = new HashMap<>();
    public HashMap<PUDTask, Integer> time = new HashMap<>();
    public HashMap<PUDTask, Vehicle> vehicle = new HashMap<>();
    private ArrayList<PUDTask> PUDTaskSet = new ArrayList<>();
    public Double BestCost;

    private final double p = 0.9;

    public Variables(){}

    public Variables(List<Vehicle> vehicles, TaskSet tasks){
        initPUDTaskSet(tasks);
        initNextTask(vehicles);
        initVehicle();
        initTime();
    }

    private void initPUDTaskSet(TaskSet tasks){
        for(Task t:tasks){
            this.PUDTaskSet.add(new PUDTask(t, "pick"));
            this.PUDTaskSet.add(new PUDTask(t, "deliver"));
        }

    }

    private void initNextTask(List<Vehicle> vehicles){
        for(PUDTask t: this.PUDTaskSet){
            nextTaskT.put(t, null);
        }

        for(Vehicle v: vehicles){
            nextTaskV.put(v, null);
        }
    }

    private void initVehicle(){
        for(PUDTask t: this.PUDTaskSet){
            vehicle.put(t, null);
        }
    }

    private void initTime(){
        for(PUDTask t: this.PUDTaskSet){
            time.put(t, null);
        }
    }

    public void selectInitialSolution(List<Vehicle> vehicle_list){
        Iterator<PUDTask> taskIterator = this.PUDTaskSet.iterator();
        //find vehicle with biggest capacity
        Vehicle biggestVehicle = vehicle_list.get(0);
        for(Vehicle v: vehicle_list){
            if(v.capacity() > biggestVehicle.capacity()){
                biggestVehicle = v;
            }
        }

        //set variables
        if(taskIterator.hasNext()){
            this.nextTaskV.put(biggestVehicle, taskIterator.next()); //set nextTask(vk)}
        }
        int i = 0;
        for(PUDTask t: this.PUDTaskSet){
            if(t.task.weight > biggestVehicle.capacity()) {throw new AssertionError
                    ("Problem unsolvable!");}

            //set nextTask(ti)
            if(taskIterator.hasNext()) {
                this.nextTaskT.put(t, taskIterator.next());
            }

            //set vehicle
            this.vehicle.put(t, biggestVehicle);

            //set time
            this.time.put(t, (i+1));
            i++;
        }

        this.BestCost = costFunction(this, vehicle_list);
    }

    public List<Variables> chooseNeighbour(List<Vehicle> vehicle_list) {
        Random rand = new Random();
        Variables oldA = this;
        Variables A;
        List<Variables> N = new ArrayList<>();
        PUDTask t = null;

        //select changing vehicule
        Vehicle v_i = vehicle_list.get(rand.nextInt(vehicle_list.size()));

        while(oldA.nextTaskV.get(v_i) == null) {
            v_i = vehicle_list.get(rand.nextInt(vehicle_list.size()));
        }


        //CHANGE vehicle
        for (Vehicle v_j : vehicle_list) {

            if (v_j.equals(v_i)) {
                continue;
            }
            t = oldA.nextTaskV.get(v_i);
            //if(t == null){continue;} //never happens

            while(t!=null) {



                if(t.type.equals("deliver")){
                    t = oldA.nextTaskT.get(t);
                    continue;
                }

                if (t.task.weight <= v_j.capacity()) {
                    A = changingVehicle(oldA, v_i, v_j, t);

                    N.add(A);
                }

                t = oldA.nextTaskT.get(t);
            }


        }

        //CHANGE task order of randomly chosen vehicle
        int length = 0;
        t = oldA.nextTaskV.get(v_i);
        //if(t == null){continue;}
        while (t != null) {
            t = oldA.nextTaskT.get(t);
            length++;
        }
        if (length >= 2) {

            for (int tIdx1 = 1; tIdx1 < length; tIdx1++) { //idx start at 1
                for (int tIdx2 = tIdx1 + 1; tIdx2 < length + 1; tIdx2++) {
                    A = changingTaskOrder(oldA, v_i, tIdx1, tIdx2);
                    N.add(A);
                }
            }
        }


        return N;
    }



    private Variables copy(){
        Variables A_copy = new Variables();
        A_copy.time = (HashMap<PUDTask, Integer>) this.time.clone();
        A_copy.vehicle = (HashMap<PUDTask, Vehicle>) this.vehicle.clone();
        A_copy.nextTaskV = (HashMap<Vehicle, PUDTask>) this.nextTaskV.clone();
        A_copy.nextTaskT = (HashMap<PUDTask, PUDTask>) this.nextTaskT.clone();
        A_copy.PUDTaskSet = this.PUDTaskSet;
        A_copy.BestCost = this.BestCost;

        return A_copy;
    }

    private Variables changingVehicle(Variables A, Vehicle v1, Vehicle v2, PUDTask t){
        Variables A1 = A.copy();
        int id = 1;

        PUDTask tDeliver = A.getPUDTask(t.task, "deliver");
        PUDTask tPre = null;
        PUDTask tPreDeliver = null;
        PUDTask t_i = A1.nextTaskV.get(v1);
        PUDTask t_j = A1.nextTaskV.get(v1);

        //Get pick up task
        while(!t_i.equals(t)) {
            tPre = t_i;
            t_i = A1.nextTaskT.get(tPre);
            id++;
        }

        PUDTask tPost = A1.nextTaskT.get(t);

        //Get delivery task
        while(!t_j.equals(tDeliver)) {
            tPreDeliver = t_j;
            t_j = A1.nextTaskT.get(tPreDeliver);
        }

        PUDTask tPostDeliver = A1.nextTaskT.get(tDeliver);


        //Task t = A.nextTaskV.get(v1);

        //remove task t from v1:

        A1.nextTaskT.put(tPreDeliver, tPostDeliver);
        if(tPre == null) { //if the first Task is removed from v1
            A1.nextTaskV.put(v1, tPost);
        }
        else{
            A1.nextTaskT.put(tPre, tPost);
        }
        if(tPost.equals(tDeliver)){ //in this case, remove tPost
            if(tPre == null){
                A1.nextTaskV.put(v1, A1.nextTaskT.get(tPost));
            }
            else{
                A1.nextTaskT.put(tPre, A1.nextTaskT.get(tPost));
            }
        }

        //add t at beginning of v2

        A1.nextTaskT.put(tDeliver, A1.nextTaskV.get(v2));
        A1.nextTaskT.put(t, tDeliver);
        A1.nextTaskV.put(v2, t);
        A1.updateTime(v1);
        A1.updateTime(v2); //TODO check if A1 is modified correctly
        A1.vehicle.put(t, v2);
        A1.vehicle.put(tDeliver, v2);

        return A1;
    }


    private void updateTime(Vehicle v){

        PUDTask t = this.nextTaskV.get(v);
        PUDTask t_j;
        if(t != null){
            this.time.put(t, 1);

            do {
                t_j = this.nextTaskT.get(t);
                if (t_j != null) {
                    this.time.put(t_j, this.time.get(t) + 1);
                    t = t_j;
                }

            } while (t_j != null);

        }
    }
    private PUDTask getPUDTask(Task t, String s){
        for(PUDTask PUDt:this.PUDTaskSet){
            if(PUDt.task.equals(t) && PUDt.type.equals(s)){
                return PUDt;
            }
        }
        throw new AssertionError
                ("The task you are looking for does not exist!");
    }
    private Variables changingTaskOrder(Variables A, Vehicle v, int tIdx1, int tIdx2){
        Variables A1 = A.copy();
        PUDTask tPre1 = null; //A1.nextTaskV.get(v); //TODO check if correct
        PUDTask t1 = A1.nextTaskV.get(v);

        int count = 1;

        while(count < tIdx1){
            tPre1 = t1;
            t1 = A1.nextTaskT.get(t1);
            count++;
        }

        PUDTask tPost1 = A1.nextTaskT.get(t1);
        PUDTask tPre2 = t1;
        PUDTask t2 = A1.nextTaskT.get(tPre2);
        count++;

        while(count < tIdx2){ //TODO function for the while loop (comes twice), optional
            tPre2 = t2;
            t2 = A1.nextTaskT.get(t2);
            count++;
        }

        PUDTask tPost2 = A1.nextTaskT.get(t2);


        //CHECK if inversion possible

        PUDTask pickT = getPUDTask(t2.task, "pick");
        if(t2.type.equals("deliver")
                && A.time.get(pickT) > A.time.get(t1)){
            return A;
        }

        PUDTask deliverT = getPUDTask(t1.task, "deliver");
        if(t1.type.equals("pick")
                && A.time.get(deliverT) < A.time.get(t2)){
            return A;
        }

        //EXCHANGE 2 Tasks
        if(tPost1 != null && tPost1.equals(t2)){
            if(tPre1 != null){
                A1.nextTaskT.put(tPre1, t2);
            }
            else{
                A1.nextTaskV.put(v, t2); //TODO take out of if
            }
           A1.nextTaskT.put(t2, t1);
           A1.nextTaskT.put(t1, tPost2);
        }
        else{
            if(tPre1 != null) {
                A1.nextTaskT.put(tPre1, t2);
            }
            else{
                A1.nextTaskV.put(v, t2); //if t2 needs to be moved if first pos
            }

            A1.nextTaskT.put(tPre2, t1);
            A1.nextTaskT.put(t2, tPost1);
            A1.nextTaskT.put(t1, tPost2);
        }
        A1.updateTime(v);

        //CHECK weight ok
        if(A1.check_weight(v)){return A;}


        return A1;
    }

    private boolean check_weight(Vehicle v){
        int cur_weight = 0;
        PUDTask t = this.nextTaskV.get(v);
        cur_weight += t.task.weight;

        while(t!=null){
            if(t.type.equals("pick")){
                cur_weight += t.task.weight;
            }
            else{
                cur_weight -= t.task.weight;
            }

            if(cur_weight > v.capacity()){
                return false;
            }
            t = this.nextTaskT.get(t);
        }
        return true;

    }
    public Variables LocalChoice(List<Variables> N, TaskSet tasks, List<Vehicle> vehicle_list){
        Variables bestN = this;
        double bestCost = this.BestCost;
        double currentCost = Double.POSITIVE_INFINITY;
        SplittableRandom random = new SplittableRandom();

        for(Variables var: N){
            currentCost = costFunction(var, vehicle_list);
            if(currentCost <= bestCost){
                bestN = var.copy();
                bestCost = currentCost;
            }
        }

        boolean val = random.nextInt(1, 101) < p*100; //val is true with proba p
        if(val){
            bestN.BestCost = bestCost;
            return  bestN;
        }
        else{
            return this;
        }
    }

    private Double costFunction(Variables var, List<Vehicle> vehicles_list){
        double c = 0;
        //double dist_btw;
        double dist;
        //double task_length;
        double costPerK;
        double total_dist = 0;
        City current_city;

        /*for(PUDTask t : this.PUDTaskSet){
            if(var.nextTaskT.get(t) == null){continue;}
            if(t.type.equals("pick")){
                dist =
            }



            dist_btw = t.deliveryCity.distanceTo(var.nextTaskT.get(t).pickupCity);
            task_length = var.nextTaskT.get(t).pickupCity
                    .distanceTo(var.nextTaskT.get(t).deliveryCity);
            costPerK = var.vehicle.get(t).costPerKm();
            total_dist = total_dist + dist_btw + task_length;
            c = c + (dist_btw + task_length) * costPerK;
        }*/

        for(Vehicle v : vehicles_list){
            dist = 0;
            PUDTask t = var.nextTaskV.get(v);
            if(t == null){continue;}
            dist += t.task.pickupCity.distanceTo(v.homeCity()); //from home to first pick-up
            current_city = t.task.pickupCity;
            t = var.nextTaskT.get(t);

            while(t != null){
                if(t.type.equals("pick")){
                    dist += current_city.distanceUnitsTo(t.task.pickupCity);
                    current_city = t.task.pickupCity;
                }
                else{
                    dist += current_city.distanceUnitsTo(t.task.deliveryCity);
                    current_city = t.task.deliveryCity;
                }
                t = var.nextTaskT.get(t);

            }

            c += dist * v.costPerKm();
        }

        return c;
    }
}
