/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author 
 */

import uchicago.src.sim.space.Object2DGrid;

public class RabbitsGrassSimulationSpace {
    private Object2DGrid rabbitFoodSpace;
    private Object2DGrid rabbitAgentSpace;
    private int size_;

    public RabbitsGrassSimulationSpace(int size){
        rabbitFoodSpace = new Object2DGrid(size, size);
        rabbitAgentSpace = new Object2DGrid(size, size);
        
        size_ = size;

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                rabbitFoodSpace.putObjectAt(i,j, 0);
            }
        }
    }


    public void spreadFood(int quantity){
        // Randomly place money in moneySpace
        for(int i = 0; i < quantity; i++){

            int x = (int)(Math.random()*size_);
            int y = (int)(Math.random()*size_);

            while ((Integer) rabbitFoodSpace.getObjectAt(x,y) != 0) {
                // be sure to find an empty coordinate
                x = (int) (Math.random() * size_);
                y = (int) (Math.random() * size_);
            }

            rabbitFoodSpace.putObjectAt(x,y, 1);
        }
    }

    public boolean addAgent(RabbitsGrassSimulationAgent agent){
        boolean retVal = false;
        int count = 0;
        int countLimit = 10 * size_ * size_;

        while((!retVal) && (count < countLimit)){
            int x = (int)(Math.random()*(rabbitAgentSpace.getSizeX()));
            int y = (int)(Math.random()*(rabbitAgentSpace.getSizeY()));
            if(!isCellOccupiedR(x, y)){
                rabbitAgentSpace.putObjectAt(x,y,agent);
                agent.setXY(x,y);
                agent.setSim_space(this);
                retVal = true;
            }
            count++;
        }

        return retVal;
    }

    public Object2DGrid getCurrentFoodSpace(){
        return rabbitFoodSpace;
    }

    public Object2DGrid getCurrentAgentSpace(){
        return rabbitAgentSpace;
    }
    public boolean isCellOccupiedR(int x, int y){
        boolean retVal = false;
        if(rabbitAgentSpace.getObjectAt(x, y)!=null) retVal = true;
        return retVal;
    }

    public boolean isCellOccupiedF(int x, int y){
        boolean retVal = false;
        if(rabbitFoodSpace.getObjectAt(x, y)!=null) retVal = true;
        return retVal;
    }

    public boolean isCellOccupied(int x, int y) {
        return isCellOccupiedF(x, y) || isCellOccupiedR(x,y);
    }

    public void removeAgentAt(int x, int y){
        rabbitAgentSpace.putObjectAt(x, y, null);
    }

    public void removeFoodAt(int x, int y){
        rabbitFoodSpace.putObjectAt(x, y, 0);
    }

    public int takeFoodAt(int x, int y){
        int food = 0;
        if(isCellOccupiedF(x,y))
            food = 1;
        rabbitFoodSpace.putObjectAt(x, y, 0);
        return food;
    }

    public boolean moveAgentAt(int x, int y, int newX, int newY){
        boolean retVal = false;
        if(!isCellOccupied(newX, newY)){
            RabbitsGrassSimulationAgent cda = (RabbitsGrassSimulationAgent)rabbitAgentSpace.getObjectAt(x, y);
            removeAgentAt(x,y);
            cda.setXY(newX, newY);
            rabbitAgentSpace.putObjectAt(newX, newY, cda);
            retVal = true;
        }
        return retVal;
    }
}
