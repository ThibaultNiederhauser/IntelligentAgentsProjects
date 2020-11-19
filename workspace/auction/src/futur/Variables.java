package AuctionFutur;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

import java.util.*;

public class Variables implements Cloneable {
    public HashMap<PUDTask, PUDTask> nextTaskT = new HashMap<>();
    public HashMap<Vehicle, PUDTask> nextTaskV = new HashMap<>();
    public HashMap<PUDTask, Integer> time = new HashMap<>();
    public HashMap<PUDTask, Vehicle> vehicle = new HashMap<>();
    public ArrayList<PUDTask> PUDTaskSet = new ArrayList<>();
    private List<Vehicle> vehicleList;
    public long BestCost;
    public boolean localChoiceBool;


    public Variables() {
    }

    public Variables(List<Vehicle> vehicles, TaskSet tasks) {
        initPUDTaskSet(tasks);
        initNextTask(vehicles);
        initVehicle();
        initTime();
        this.vehicleList = vehicles;
    }

    public Variables(List<Vehicle> vehicles, ArrayList<Task> tasks) {
        initPUDTaskSet(tasks);
        initNextTask(vehicles);
        initVehicle();
        initTime();
        this.vehicleList = vehicles;
    }

    private void reinit(){
        initNextTask(this.vehicleList);
        initVehicle();
        initTime();
    }

    private void initPUDTaskSet(ArrayList<Task> tasks) {
        for (Task t : tasks) {
            this.PUDTaskSet.add(new PUDTask(t, "pick"));
            this.PUDTaskSet.add(new PUDTask(t, "deliver"));
        }

    }

    private void initPUDTaskSet(TaskSet tasks) {
        for (Task t : tasks) {
            this.PUDTaskSet.add(new PUDTask(t, "pick"));
            this.PUDTaskSet.add(new PUDTask(t, "deliver"));
        }

    }

    private void initNextTask(List<Vehicle> vehicles) {
        for (PUDTask t : this.PUDTaskSet) {
            nextTaskT.put(t, null);
        }

        for (Vehicle v : vehicles) {
            nextTaskV.put(v, null);
        }
    }

    private void initVehicle() {
        for (PUDTask t : this.PUDTaskSet) {
            vehicle.put(t, null);
        }
    }

    private void initTime() {
        for (PUDTask t : this.PUDTaskSet) {
            time.put(t, null);
        }
    }

    public void selectInitialSolution() {
        double dist;
        double shortestDist;
        PUDTask firstTask;
        PUDTask tDeliver;
        Vehicle candidate;
        //int i = 0;
        for (PUDTask t : this.PUDTaskSet) {
            if (t.type == "deliver") {
                continue;
            }
            shortestDist = Double.POSITIVE_INFINITY;
            candidate = null;

            for (Vehicle v : this.vehicleList) {
                dist = v.homeCity().distanceTo(t.task.pickupCity);
                if (dist < shortestDist && t.task.weight < v.capacity()) {
                    shortestDist = dist;
                    candidate = v;
                }
            }

            if (candidate == null) {
                throw new AssertionError("Problem unsolvable!");
            }

            firstTask = this.nextTaskV.get(candidate);
            tDeliver = getPUDTask(t.task, "deliver");
            this.nextTaskV.put(candidate, t);
            this.nextTaskT.put(t, tDeliver);
            this.nextTaskT.put(tDeliver, firstTask);

            //set vehicle
            this.vehicle.put(t, candidate);
            this.vehicle.put(tDeliver, candidate);

        }

        for (Vehicle v : this.vehicleList) {
            this.updateTime(v);
        }

        this.BestCost = this.costFunction();
    }

    public List<Variables> chooseNeighbour() {
        Random rand = new Random();
        Variables oldA = this;
        Variables A;
        List<Variables> N = new ArrayList<>();
        PUDTask t = null;

        //select changing vehicle
        Vehicle v_i = this.vehicleList.get(rand.nextInt(this.vehicleList.size()));

        while (oldA.nextTaskV.get(v_i) == null) {
            v_i = this.vehicleList.get(rand.nextInt(this.vehicleList.size()));
        }


        //CHANGE vehicle
        for (Vehicle v_j : this.vehicleList) {
            if (v_j.equals(v_i)) {
                continue;
            }
            t = oldA.nextTaskV.get(v_i);

            if (t.type.equals("deliver")) {
                continue;
            }

            if (t.task.weight <= v_j.capacity()) { //TODO can remove this if
                A = changingVehicle(oldA, v_i, v_j, t);
                N.add(A);
            }

        }

        //CHANGE task order of randomly chosen vehicle
        //v_i = this.vehicleList.get(rand.nextInt(this.vehicleList.size()));

        int length = 0;
        t = oldA.nextTaskV.get(v_i);

        while (t != null) {
            t = oldA.nextTaskT.get(t);
            length++;
        }
        if (length >= 2) {
            for (int tIdx1 = 1; tIdx1 < length; tIdx1++) { //idx start at 1
                for (int tIdx2 = tIdx1 + 1; tIdx2 < length + 1; tIdx2++) {
                    A = changingTaskOrder(oldA, v_i, tIdx1, tIdx2);
                    if (A != null) {
                        N.add(A);
                    }
                }
            }
        }

        return N;
    }


    public Variables copy() {
        Variables A_copy = new Variables();
        A_copy.time = (HashMap<PUDTask, Integer>) this.time.clone();
        A_copy.vehicle = (HashMap<PUDTask, Vehicle>) this.vehicle.clone();
        A_copy.nextTaskV = (HashMap<Vehicle, PUDTask>) this.nextTaskV.clone();
        A_copy.nextTaskT = (HashMap<PUDTask, PUDTask>) this.nextTaskT.clone();
        A_copy.PUDTaskSet = this.PUDTaskSet;
        A_copy.BestCost = this.BestCost;
        A_copy.vehicleList = this.vehicleList;

        return A_copy;
    }

    private Variables changingVehicle(Variables A, Vehicle v1, Vehicle v2, PUDTask t) {
        Variables A1 = A.copy();
        int id = 1;

        PUDTask tDeliver = A.getPUDTask(t.task, "deliver");
        PUDTask tPre = null;
        PUDTask tPreDeliver = null;
        PUDTask t_i = A1.nextTaskV.get(v1);
        PUDTask t_j = A1.nextTaskV.get(v1);

        //Get pick up task
        while (!t_i.equals(t)) {
            tPre = t_i;
            t_i = A1.nextTaskT.get(tPre);
            id++;
        }

        PUDTask tPost = A1.nextTaskT.get(t);

        //Get delivery task
        while (!t_j.equals(tDeliver)) {
            tPreDeliver = t_j;
            t_j = A1.nextTaskT.get(tPreDeliver);
        }

        PUDTask tPostDeliver = A1.nextTaskT.get(tDeliver);

        //remove task t from v1:

        A1.nextTaskT.put(tPreDeliver, tPostDeliver);
        if (tPre == null) { //if the first Task is removed from v1
            A1.nextTaskV.put(v1, tPost);
        } else {
            A1.nextTaskT.put(tPre, tPost);
        }
        if (tPost.equals(tDeliver)) { //in this case, remove tPost
            if (tPre == null) {
                A1.nextTaskV.put(v1, A1.nextTaskT.get(tPost));
            } else {
                A1.nextTaskT.put(tPre, A1.nextTaskT.get(tPost));
            }
        }

        //add t at beginning of v2
        A1.nextTaskT.put(tDeliver, A1.nextTaskV.get(v2));
        A1.nextTaskT.put(t, tDeliver);
        A1.nextTaskV.put(v2, t);
        A1.updateTime(v1);
        A1.updateTime(v2);
        A1.vehicle.put(t, v2);
        A1.vehicle.put(tDeliver, v2);

        return A1;
    }


    private void updateTime(Vehicle v) {

        PUDTask t = this.nextTaskV.get(v);
        PUDTask t_j;
        if (t != null) {
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

    private PUDTask getPUDTask(Task t, String s) {
        for (PUDTask PUDt : this.PUDTaskSet) {
            if (PUDt.task.equals(t) && PUDt.type.equals(s)) {
                return PUDt;
            }
        }
        throw new AssertionError
                ("The task you are looking for does not exist!");
    }

    private Variables changingTaskOrder(Variables A, Vehicle v, int tIdx1, int tIdx2) {
        Variables A1 = A.copy();
        PUDTask tPre1 = null;
        PUDTask t1 = A1.nextTaskV.get(v);

        int count = 1;

        while (count < tIdx1) {
            tPre1 = t1;
            t1 = A1.nextTaskT.get(t1);
            count++;
        }

        PUDTask tPost1 = A1.nextTaskT.get(t1);
        PUDTask tPre2 = t1;
        PUDTask t2 = A1.nextTaskT.get(tPre2);
        count++;

        while (count < tIdx2) {
            tPre2 = t2;
            t2 = A1.nextTaskT.get(t2);
            count++;
        }

        PUDTask tPost2 = A1.nextTaskT.get(t2);


        //CHECK if inversion possible
        PUDTask pickT = getPUDTask(t2.task, "pick");
        if (t2.type.equals("deliver")
                && A.time.get(pickT) >= A.time.get(t1)) {
            return null;
        }

        PUDTask deliverT = getPUDTask(t1.task, "deliver");
        if (t1.type.equals("pick")
                && A.time.get(deliverT) <= A.time.get(t2)) {
            return null;
        }

        //EXCHANGE 2 Tasks
        if (tPre1 != null) {
            A1.nextTaskT.put(tPre1, t2);
        } else {
            A1.nextTaskV.put(v, t2);
        }

        if (tPost1 != null && tPost1.equals(t2)) {

            A1.nextTaskT.put(t2, t1);
            A1.nextTaskT.put(t1, tPost2);
        } else {
            A1.nextTaskT.put(tPre2, t1);
            A1.nextTaskT.put(t2, tPost1);
            A1.nextTaskT.put(t1, tPost2);
        }
        A1.updateTime(v);

        //CHECK weight ok
        if (!A1.check_weight(v)) {
            return null;
        }

        return A1;
    }

    private boolean check_weight(Vehicle v) {
        int cur_weight = 0;
        PUDTask t = this.nextTaskV.get(v);
        cur_weight += t.task.weight;

        while (t != null) {
            if (t.type.equals("pick")) {
                cur_weight += t.task.weight;
            } else {
                cur_weight -= t.task.weight;
            }

            if (cur_weight > v.capacity()) {
                return false;
            }
            t = this.nextTaskT.get(t);
        }
        return true;

    }

    public Variables LocalChoice(List<Variables> N, double p) {
        ArrayList<Variables> bestN = new ArrayList<>();
        bestN.add(this.copy());
        ArrayList<Variables> improvN = new ArrayList<>();
        improvN.add(this.copy());
        Variables choice = null;
        long bestCost = this.BestCost;
        long currentCost = Long.MAX_VALUE;
        SplittableRandom random = new SplittableRandom();

        for (Variables var : N) {
            currentCost = var.costFunction();
            if (currentCost < bestCost) {
                bestN.clear();
                bestN.add(var);
                bestCost = currentCost;
                improvN.add(var);
            }
            if (currentCost == bestCost) {
                bestN.add(var);
                improvN.add(var);
            }
        }

        if (bestN.size() > 1) {
            choice = bestN.get(random.nextInt(bestN.size()));
        } else {
            choice = bestN.get(0);
        }


        boolean val = random.nextInt(1, 101) <= p * 100; //val is true with proba p
        //System.out.println(val);


        if (val) {
            choice.BestCost = bestCost;
            choice.localChoiceBool = true;

            return choice;
        } else {
            if(N.size() > 1){N.remove(choice);}
            int randInd = random.nextInt(N.size());
            Variables RandomChoice = N.get(randInd);
            RandomChoice.localChoiceBool = false;
            RandomChoice.BestCost = RandomChoice.costFunction();
            return RandomChoice;
        }
    }

    public Long costFunction() {
        long c = 0;
        double dist;
        City current_city;


        for (Vehicle v : this.vehicleList) {
            dist = 0;
            PUDTask t = this.nextTaskV.get(v);
            if (t == null) {
                continue;
            }
            dist += t.task.pickupCity.distanceTo(v.homeCity()); //from home to first pick-up
            current_city = t.task.pickupCity;
            t = this.nextTaskT.get(t);

            while (t != null) {
                if (t.type.equals("pick")) {
                    dist += current_city.distanceTo(t.task.pickupCity);
                    current_city = t.task.pickupCity;
                } else {
                    dist += current_city.distanceTo(t.task.deliveryCity);
                    current_city = t.task.deliveryCity;
                }
                t = this.nextTaskT.get(t);

            }

            c += dist * v.costPerKm();
        }

        return c;
    }

    public void addTask(Task task){
        this.PUDTaskSet.add(new PUDTask(task, "pick"));
        this.PUDTaskSet.add(new PUDTask(task, "deliver"));

        this.reinit();
    }
}