import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.engine.SimInit;

/**
 * Class that implements the simulation model for the rabbits grass
 * simulation.  This is the first class which needs to be setup in
 * order to run Repast simulation. It manages the entire RePast
 * environment and the simulation.
 *
 * @author
 */


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

    // Parameters skra

	private static final int GRIDSIZE = 20;
	private static final int NUMINITRABBITS = 15;

	private int gridSize = GRIDSIZE;
    private int numInitRabbits = NUMINITRABBITS;

    public void begin() {
        // Start everything
        buildModel();
        buildSchedule();
        buildDisplay();
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
    }

    public void buildModel() {
		System.out.println("Building model");
    }

    public void buildSchedule() {
		System.out.println("Building schedule");

		System.out.println("I have no idea what this does");
    }

    public void buildDisplay() {
		System.out.println("Build display");
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

	public int getGridSize(){
    	return gridSize;
	}

	public void setGridSize(int size){
    	gridSize = size;
	}
}
