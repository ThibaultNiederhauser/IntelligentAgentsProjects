/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author 
 */

import uchicago.src.sim.space.Object2DGrid;

public class RabbitsGrassSimulationSpace {
    private Object2DGrid rabbitGardenSpace;
    private int size_;

    public RabbitsGrassSimulationSpace(int size){
        rabbitGardenSpace = new Object2DGrid(size, size);
        size_ = size;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                rabbitGardenSpace.putObjectAt(i,j, 0);
            }
        }
    }


    public void spreadFood(int quantity){
        // Randomly place money in moneySpace
        for(int i = 0; i < quantity; i++){

            int x = (int)(Math.random()*size_);
            int y = (int)(Math.random()*size_);

            while (rabbitGardenSpace.getObjectAt(x,y)!= null) {
                // be sure to find an empty coordinate
                x = (int) (Math.random() * size_);
                y = (int) (Math.random() * size_);
            }

            rabbitGardenSpace.putObjectAt(x,y, 1);
        }
    }
    public Object2DGrid getCurrentFoodSpace(){
        return rabbitGardenSpace;
    }
}
