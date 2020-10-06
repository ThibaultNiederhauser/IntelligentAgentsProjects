import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import template.*;

public class RandomAgent implements ReactiveBehavior {

    RandomTemplate rndTemplate = new RandomTemplate();
    @Override
    public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
        rndTemplate.setup(topology, distribution, agent);
    }

    @Override
    public Action act(Vehicle vehicle, Task availableTask) {
        return rndTemplate.act(vehicle, availableTask);
    }
}
