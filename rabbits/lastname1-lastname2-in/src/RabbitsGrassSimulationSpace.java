import uchicago.src.sim.space.Object2DGrid;
import java.util.Random;




/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author 
 */

public class RabbitsGrassSimulationSpace {
    private Object2DGrid grassSpace;
    private Object2DGrid agentSpace;


    public RabbitsGrassSimulationSpace(int gridSize){
        grassSpace = new Object2DGrid(gridSize, gridSize);
        agentSpace = new Object2DGrid(gridSize, gridSize);


        for(int i = 0; i < gridSize; i++){
            for(int j = 0; j < gridSize; j++){
                grassSpace.putObjectAt(i,j, 0);
            }
        }
    }

    public void spreadGrass(int grass){
        // Randomly place money in moneySpace
        for(int i = 0; i < grass; i++){

            // Choose coordinates
            int x = (int)(Math.random()*(grassSpace.getSizeX()));
            int y = (int)(Math.random()*(grassSpace.getSizeY()));

            // Get the value of the object at those coordinates
            int currentValue = getGrassAt(x, y);
            int counter = 0;

            while (currentValue >= 50 && counter < 10){
                x = (int)(Math.random()*(grassSpace.getSizeX()));
                y = (int)(Math.random()*(grassSpace.getSizeY()));
                currentValue = getGrassAt(x, y);
                counter++;
            }

            // Replace the Integer object with another one with the new value
            if (currentValue < 50)
                grassSpace.putObjectAt(x,y, currentValue + 1);
        }
    }

    public int getGrassAt(int x, int y){
        int i;
        if(grassSpace.getObjectAt(x,y)!= null){
            i = (Integer) grassSpace.getObjectAt(x, y);
        }
        else{
            i = 0;
        }
        return i;
    }


    public Object2DGrid getCurrentGrassSpace(){
        return grassSpace;
    }

    public Object2DGrid getCurrentAgentSpace(){
        return agentSpace;
    }


    public boolean isCellOccupied(int x, int y){
        boolean retVal = false;
        if(agentSpace.getObjectAt(x, y)!=null) retVal = true;
        return retVal;
    }

    public boolean addAgent(RabbitsGrassSimulationAgent agent){
        boolean retVal = false;
        int count = 0;
        int countLimit = 10 * agentSpace.getSizeX() * agentSpace.getSizeY();

        while((!retVal) && (count < countLimit)){
            int x = (int)(Math.random()*(agentSpace.getSizeX()));
            int y = (int)(Math.random()*(agentSpace.getSizeY()));
            if(!isCellOccupied(x, y)){
                agentSpace.putObjectAt(x,y,agent);
                agent.setXY(x,y);
                retVal = true;
                agent.setRabbitGrassSpace(this);
            }
            count++;
        }

        return retVal;
    }

    public boolean addAgent(int posX, int posY, RabbitsGrassSimulationAgent agent){
        boolean retVal = false;
        int count = 0;
        int countLimit = 10;

        while((retVal==false) && (count < countLimit)){
            int dx = (int)Math.floor(Math.random() * 3) - 1;
            int dy = 0;

            if(dx == 0){
                while(dy == 0){
                    dy = (int)Math.floor(Math.random() * 3) - 1;
                }
            }

            int x = (dx + posX) % agentSpace.getSizeX();
            int y = (dy + posY) % agentSpace.getSizeY();

            if(x < 0){
                x = agentSpace.getSizeX()-1;}
            if(y < 0){
                y = agentSpace.getSizeY()-1;}


            if(isCellOccupied(x,y) == false){
                agentSpace.putObjectAt(x,y,agent);
                agent.setXY(x,y);
                retVal = true;
            }
            agent.setRabbitGrassSpace(this);
            count++;
        }

        return retVal;
    }


    public void removeAgentAt(int x, int y){
        agentSpace.putObjectAt(x, y, null);
    }

    public int eatGrassAt(int x, int y){
        int grass = getGrassAt(x, y);
        grassSpace.putObjectAt(x, y, new Integer(0));
        return grass;
    }

    public boolean moveAgentAt(int x, int y, int newX, int newY){
        boolean retVal = false;
        if(!isCellOccupied(newX, newY)){
            RabbitsGrassSimulationAgent rga = (RabbitsGrassSimulationAgent) agentSpace.getObjectAt(x, y);
            removeAgentAt(x,y);
            rga.setXY(newX, newY);
            agentSpace.putObjectAt(newX, newY, rga);
            retVal = true;
        }
        return retVal;
    }

    public int getTotalGrass(){
        int totalGrass = 0;
        for(int i = 0; i < agentSpace.getSizeX(); i++){
            for(int j = 0; j < agentSpace.getSizeY(); j++){
                totalGrass += getGrassAt(i,j);
            }
        }
        return totalGrass;
    }


}
