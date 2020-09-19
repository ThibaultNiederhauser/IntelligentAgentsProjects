import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;

import java.awt.Color;

/**
 * Class that implements the simulation agent for the rabbits grass simulation.

 * @author
 */

public class RabbitsGrassSimulationAgent implements Drawable {

	private int x_;
	private int y_;
	private int age;
	private int food;
	private RabbitsGrassSimulationSpace sim_space;

	private static int IDNumber = 0;
	private int ID;

	public RabbitsGrassSimulationAgent(){
		age = 0;
		x_ = -1;
		y_ = -1;
		IDNumber++;
		ID = IDNumber;
	}

	public RabbitsGrassSimulationAgent(int x, int y){
		age = 0;
		x_ = -1;
		y_ = -1;
		IDNumber++;
		ID = IDNumber;
	}

	public void step(){
		age++;
		food += sim_space.takeFoodAt(x_, y_);
	}

	public void setXY(int x, int y){
		x_ = x;
		y_ = y;
	}

	public void draw(SimGraphics arg0) {
		// TODO Auto-generated method stub
		if (age < 100)
			arg0.drawFastRoundRect(Color.blue);
		else
			arg0.drawFastRoundRect(Color.black);
	}

	public int getX() {
		// TODO Auto-generated method stub
		return x_;
	}

	public int getY() {
		// TODO Auto-generated method stub
		return y_;
	}

	public String getID(){
		return "A-" + ID;
	}

	public void report(){
		System.out.println(getID() +
				" at " +
				x_ + ", " + y_ +
				"- age: " + age);
	}

	public int getAge(){
		return age;
	}

	public void setSim_space(RabbitsGrassSimulationSpace space){
		sim_space = space;
	}

	public int getFood(){
		return food;
	}

	public void setFood(int newFood){
		food = newFood;
	}
}
