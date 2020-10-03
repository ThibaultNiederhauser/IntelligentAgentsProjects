import java.util.Random;
import java.util.List;

import logist.config.Parsers;
import logist.simulation.Manager;
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


public class ReinforcementLearning {
    Topology topo;
    private static final int numCities = topo.size();
    private static final double epsilon = 0.1; //stopping criteria


    public static void reinforce(){

        double[][] V = initV(numCities);
        double[][][] Q = new double[numCities][2][numCities-1];

        while(true){
            //TODO change while condition
            for(int i=0; i< numCities; i++ ){
                for(int j=0; j< 2; j++ ){

                    List<City>  = City.neighbors().length;

                    for(int k= 0; k<(numCities-1); k++){
                        Q[i][j][k] =

                    }
                }

            }

        }



    }

    private static double randomFill(){
        Random rand = new Random();
        double randomNum = rand.nextDouble();
        return randomNum;
    }

    private static double[][] initV(int numCities) {
        //INIT V(S) randomly
        double[][] V = new double[numCities][2]; //two-dim for task availability
        for(int i = 0; i<numCities; i++){
            for(int j = 0; j <2; j++){
                V[i][j] = randomFill();
            }
        }

        return V;
    }



}
