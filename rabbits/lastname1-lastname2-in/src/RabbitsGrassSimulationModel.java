import java.awt.Color;

import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.Value2DDisplay;


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
        // TODO Auto-generated method stub
        System.out.println("Running setup");
        rabbitsGS = null;

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
    }

    public void buildSchedule() {
		System.out.println("Building schedule");

		System.out.println("I have no idea what this does");
    }

    public void buildDisplay() {
        System.out.println("Build display");

        ColorMap map = new ColorMap();

        for(int i = 1; i<16; i++){
            map.mapColor(i, new Color((int)(i * 8 + 127), 0, 0));
        }
        map.mapColor(0, Color.white);

        Value2DDisplay displayMoney =
                new Value2DDisplay(rabbitsGS.getCurrentFoodSpace(), map);

        displaySurf.addDisplayable(displayMoney, "Food");

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
