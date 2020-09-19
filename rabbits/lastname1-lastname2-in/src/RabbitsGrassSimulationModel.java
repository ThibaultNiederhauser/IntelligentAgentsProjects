import java.awt.Color;
import java.util.ArrayList;

import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.util.SimUtilities;


/**
 * Class that implements the simulation model for the rabbits grass
 * simulation.  This is the first class which needs to be setup in
 * order to run Repast simulation. It manages the entire RePast
 * environment and the simulation.
 *
 * @author Sfera e basta
 */


//public class RabbitsGrassSimulationModel extends SimModelImpl {
public class RabbitsGrassSimulationModel extends SimModelImpl {

        public static void main(String[] args) {

        System.out.println("Crazy rabbit goes brrrr");

        SimInit init = new SimInit();
        RabbitsGrassSimulationModel model = new RabbitsGrassSimulationModel();
        // Do "not" modify the following lines of parsing arguments
        if (args.length == 0) // by default, you don't use parameter file nor batch mode
            init.loadModel(model, "", false);
        else
            init.loadModel(model, args[0], Boolean.parseBoolean(args[1]));
    }

    private Schedule schedule;
    private RabbitsGrassSimulationSpace rabbitsGS;
    private DisplaySurface displaySurf;
    private ArrayList<RabbitsGrassSimulationAgent> rabbitList;

    // Parameters skra
    //todo add all

	private static final int GRIDSIZE = 20;
	private static final int NUMINITRABBITS = 15;
    private static final int NUMINITGRASS = 40;

	private int gridSize = GRIDSIZE;
    private int numInitRabbits = NUMINITRABBITS;
    private int numInitGrass = NUMINITGRASS;

    public void begin() {
        // Start everything
        buildModel();
        buildSchedule();
        buildDisplay();
        displaySurf.display();
    }

    public String[] getInitParam() {
        // TODO Auto-generated method stub
        // Parameters to be set by users via the Repast UI slider bar
        // Do "not" modify the parameters names provided in the skeleton code, you can add more if you want
        String[] params = {"GridSize", "NumInitRabbits", "NumInitGrass", "GrassGrowthRate", "BirthThreshold"};
        return params;
    }

    public String getName() {
        // Titre
        return "Cats with legs that like cakes";
    }

    public void setup() {
        // setup
        System.out.println("Running setup");
        rabbitList = new ArrayList<RabbitsGrassSimulationAgent>();
        rabbitsGS = null;
        schedule = new Schedule(1);

        if (displaySurf != null){
            displaySurf.dispose();
        }
        displaySurf = null;

        displaySurf = new DisplaySurface(this, "Rabbit Model Window 1");

        registerDisplaySurface("Rabbit Model Window 1", displaySurf);

    }

    public void buildModel() {
		System.out.println("Building model");
		rabbitsGS = new RabbitsGrassSimulationSpace(gridSize);
		rabbitsGS.spreadFood(numInitGrass);

        for(int i = 0; i < numInitRabbits; i++){
            addNewAgent();
        }

        for (RabbitsGrassSimulationAgent rabbitsGrassSimulationAgent : rabbitList) {
            rabbitsGrassSimulationAgent.report();
        }
    }

    public void buildSchedule() {
		System.out.println("Building schedule");

		class BunnyStep extends BasicAction {
		    public void execute() {
		        SimUtilities.shuffle(rabbitList);
		        for (int i = 0; i< rabbitList.size(); i++){
		            RabbitsGrassSimulationAgent ra = (RabbitsGrassSimulationAgent) rabbitList.get(i);
		            ra.step();
                }

                int deadAgents = reapDeadAgents();
                for(int i =0; i < deadAgents; i++){
                    //handleAgentDeath();
                    continue;
                }

                displaySurf.updateDisplay();
            }
        }

        schedule.scheduleActionBeginning(0, new BunnyStep());

        class RabbitCountLiving extends BasicAction {
            public void execute(){
                countLivingRabbits();
            }
        }

        schedule.scheduleActionAtInterval(10, new RabbitCountLiving());

    }

    public void buildDisplay() {
        System.out.println("Build display");

        ColorMap map = new ColorMap();

        for(int i = 1; i<16; i++){
            map.mapColor(i, new Color((int)(i * 8 + 127), 0, 0));
        }
        map.mapColor(0, Color.white);

        Value2DDisplay displayFood =
                new Value2DDisplay(rabbitsGS.getCurrentFoodSpace(), map);

        Object2DDisplay displayAgents = new Object2DDisplay(rabbitsGS.getCurrentAgentSpace());
        displayAgents.setObjectList(rabbitList);

        displaySurf.addDisplayable(displayFood, "Food");
        displaySurf.addDisplayable(displayAgents, "Agents");

    }

    private void addNewAgent(){
        RabbitsGrassSimulationAgent a = new RabbitsGrassSimulationAgent();
        rabbitList.add(a);
        rabbitsGS.addAgent(a);
    }

    private int countLivingRabbits(){
        int livingAgents = 0;
        for (RabbitsGrassSimulationAgent bunny : rabbitList) {
            if(bunny.getAge() < 100)
                livingAgents++;
        }
        System.out.println("Number of living agents is: " + livingAgents);

        return livingAgents;
    }

    private int reapDeadAgents(){
        int count = 0;
        for(int i = (rabbitList.size() - 1); i >= 0 ; i--){
            RabbitsGrassSimulationAgent cda = (RabbitsGrassSimulationAgent)rabbitList.get(i);
            if(cda.getAge() > 100){
                rabbitsGS.removeAgentAt(cda.getX(), cda.getY());
                rabbitList.remove(i);
                count++;
            }
        }
        return count;
    }

    public Schedule getSchedule(){
		return schedule;
	}

	public int getNumInitRabbits(){
		return numInitRabbits;
	}

	public void setNumInitRabbits(int na){
		numInitRabbits = na;
	}

    public int getNumInitGrass(){
        return numInitGrass;
    }

    public void setNumInitGrass(int na){
        numInitGrass = na;
    }

    public int getGridSize(){
    	return gridSize;
	}

	public void setGridSize(int size){
    	gridSize = size;
	}

}
