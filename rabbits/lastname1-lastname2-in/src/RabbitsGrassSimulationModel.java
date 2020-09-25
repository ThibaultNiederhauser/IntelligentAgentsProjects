import java.awt.Color;
import java.util.ArrayList;

import uchicago.src.sim.analysis.DataSource;
import uchicago.src.sim.analysis.Sequence;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.util.SimUtilities;
import uchicago.src.sim.analysis.OpenSequenceGraph;


/**
 * Class that implements the simulation model for the rabbits grass
 * simulation.  This is the first class which needs to be setup in
 * order to run Repast simulation. It manages the entire RePast
 * environment and the simulation.
 *
 * @author 
 */


public class RabbitsGrassSimulationModel extends SimModelImpl {

	//Default Values
	private static final int NUMINITRABBITS = 100;
	private static final int GRIDSIZE = 20;
	private static final int NUMINITGRASS = 100;
	private static final int ENERGYINIT = 100;
	private static final int GRASSGROWTHRATE = 1;
	private static final int BIRTHTHRESHOLD = 150;
	private static final int REPRODUCTIONENERGY = 1;


	private int numInitRabbits = NUMINITRABBITS;
	private int gridSize = GRIDSIZE;
	private int numInitGrass = NUMINITGRASS;
	private int grassGrowthRate = GRASSGROWTHRATE;
	private int birthThreshold = BIRTHTHRESHOLD;

	private Schedule schedule;
	private RabbitsGrassSimulationSpace rgsSpace;
	private DisplaySurface displaySurf;
	private ArrayList agentList;
	private OpenSequenceGraph amountOfGrassInSpace;
	private OpenSequenceGraph numLivingAgents;


	class grassInSpace implements DataSource, Sequence {

		public Object execute() {
			return new Double(getSValue());
		}

		public double getSValue() {
			return (double)rgsSpace.getTotalGrass();
		}
	}

	class livingAgents implements DataSource, Sequence {

		public Object execute() {
			return new Double(getSValue());
		}

		public double getSValue() { return (double)countLivingAgents(); }

	}

	public static void main(String[] args) {

		System.out.println("Rabbit skeleton");

		SimInit init = new SimInit();
		RabbitsGrassSimulationModel model = new RabbitsGrassSimulationModel();
		// Do "not" modify the following lines of parsing arguments
		if (args.length == 0) // by default, you don't use parameter file nor batch mode
			init.loadModel(model, "", false);
		else
			init.loadModel(model, args[0], Boolean.parseBoolean(args[1]));

	}

	public void setup() {
		System.out.println("Running setup");
		rgsSpace = null;
		agentList = new ArrayList();
		schedule = new Schedule(1);

		// Tear down Displays
		if (displaySurf != null){
			displaySurf.dispose();
		}
		displaySurf = null;

		if (amountOfGrassInSpace != null){
			amountOfGrassInSpace.dispose();
		}
		amountOfGrassInSpace = null;

		if (numLivingAgents != null){
			numLivingAgents.dispose();
		}
		numLivingAgents = null;


		//Create Displays
		displaySurf = new DisplaySurface(this, "Rabbit Grass Model Window 1");
		amountOfGrassInSpace = new OpenSequenceGraph("Amount Of Grass In Space",this);
		numLivingAgents = new OpenSequenceGraph("Living Agents", this);


		//Register Displays
		registerDisplaySurface("Rabbit Grass Model Window 1", displaySurf);
		this.registerMediaProducer("Plot", amountOfGrassInSpace);
		this.registerMediaProducer("PLot", numLivingAgents);

	}

	public void begin() {
		buildModel();
		buildSchedule();
		buildDisplay();

		displaySurf.display();
		amountOfGrassInSpace.display();
		numLivingAgents.display();
	}

	public void buildModel() {
		System.out.println("Running BuildModel");
		rgsSpace = new RabbitsGrassSimulationSpace(gridSize);
		rgsSpace.spreadGrass(numInitGrass);

		for(int i = 0; i < numInitRabbits; i++){
			addNewRabbit();
		}

		for(int i = 0; i < agentList.size(); i++){
			RabbitsGrassSimulationAgent rga = (RabbitsGrassSimulationAgent) agentList.get(i);
			rga.report();
		}

		displaySurf.display();
	}

	public void buildSchedule() {
		System.out.println("Running Buildschedule");

		class RabbitGrassStep extends BasicAction {
			public void execute() {
				SimUtilities.shuffle(agentList);
				for(int i =0; i < agentList.size(); i++){
					RabbitsGrassSimulationAgent rga = (RabbitsGrassSimulationAgent) agentList.get(i);
					rga.step();
				}
				int deadAgents = reapDeadAgents();
				displaySurf.updateDisplay();

			}
		}
		schedule.scheduleActionBeginning(0, new RabbitGrassStep());


		class CarryDropCountLiving extends BasicAction {
			public void execute(){
				numLivingAgents.step();
			}
		}

		schedule.scheduleActionAtInterval(10, new CarryDropCountLiving());

		class RabbitGrassUpdateGrassInSpace extends BasicAction {
			public void execute(){
				amountOfGrassInSpace.step();
			}
		}
		schedule.scheduleActionAtInterval(10, new RabbitGrassUpdateGrassInSpace());


		class RabbitGrassGrowthGrass extends BasicAction {
			public void execute() {rgsSpace.spreadGrass(grassGrowthRate);}
		}

		schedule.scheduleActionAtInterval(1, new RabbitGrassGrowthGrass());

		//class RabbitGrassReproduce extends BasicAction {
		//	public void execute() {reproduce(birthThreshold);}
		//}

		//schedule.scheduleActionAtInterval(1, new RabbitGrassReproduce());

	}

	public void buildDisplay() {
		System.out.println("Running BuildDisplay");
		ColorMap map = new ColorMap();

		for(int i = 1; i<16; i++){
			map.mapColor(i, new Color(0, (int)(i * 8 + 127), 0));
		}
		map.mapColor(0, Color.white);

		Value2DDisplay displayGrass=
				new Value2DDisplay(rgsSpace.getCurrentGrassSpace(), map);


		Object2DDisplay displayAgents = new Object2DDisplay(rgsSpace.getCurrentAgentSpace());
		displayAgents.setObjectList(agentList);

		displaySurf.addDisplayableProbeable(displayGrass, "Grass");
		displaySurf.addDisplayableProbeable(displayAgents, "Agents");

		amountOfGrassInSpace.addSequence("Grass In Space", new grassInSpace());
		numLivingAgents.addSequence("Living Agents", new livingAgents());

	}


	public String[] getInitParam() {
		// TODO Auto-generated method stub
		// Parameters to be set by users via the Repast UI slider bar
		// Do "not" modify the parameters names provided in the skeleton code, you can add more if you want
		String[] params = {"GridSize", "NumInitRabbits", "NumInitGrass", "GrassGrowthRate", "BirthThreshold"};
		return params;
	}

	public String getName() {
		return "Rabbits Grass Simulation";
	}

	private void addNewRabbit(){
		RabbitsGrassSimulationAgent a = new RabbitsGrassSimulationAgent(ENERGYINIT);
		agentList.add(a);
		rgsSpace.addAgent(a);
	}

	private int countLivingAgents(){
		int livingAgents = 0;
		for(int i = 0; i < agentList.size(); i++){
			RabbitsGrassSimulationAgent rga = (RabbitsGrassSimulationAgent) agentList.get(i);
			if(rga.getEnergy() > 0) livingAgents++;
		}
		System.out.println("Number of living agents is: " + livingAgents);

		return livingAgents;
	}

	private int reapDeadAgents(){
		int count = 0;
		for(int i = (agentList.size() - 1); i >= 0 ; i--){
			RabbitsGrassSimulationAgent rga = (RabbitsGrassSimulationAgent) agentList.get(i);
			if(rga.getEnergy() < 1){
				rgsSpace.removeAgentAt(rga.getX(), rga.getY());
				agentList.remove(i);
				count++;
			}
		}
		return count;
	}

	private void reproduce(int thres){
		System.out.println("BIRTH");

		for(int i =0; i < agentList.size(); i++){
			RabbitsGrassSimulationAgent rga = (RabbitsGrassSimulationAgent) agentList.get(i);

			if(rga.getEnergy() >= thres){
				rga.setEnergy(rga.getEnergy()-REPRODUCTIONENERGY);
				addNewRabbit();

			}
		}
	}

	public Schedule getSchedule() {
		// TODO Auto-generated method stub
		return schedule;
	}

	public int getNumInitRabbits() {
		return numInitRabbits;
	}

	public void setNumInitRabbits(int nr) {
		numInitRabbits = nr;
	}

	public int getGridSize() {
		return gridSize;
	}

	public void setGridSize(int gs) {
		gridSize = gs;
	}

	public int getNumInitGrass() {
		return numInitGrass;
	}

	public void setNumInitGrass(int i) {
		numInitGrass = i;
	}

	public int getGrassGrowthRate() {
		return grassGrowthRate;
	}

	public void setGrassGrowthRate(int i) {
		grassGrowthRate = i;
	}

	public int getBirthThreshold() {
		return birthThreshold;
	}

	public void setBirthThreshold(int t) {
		birthThreshold = t;
	}

}



