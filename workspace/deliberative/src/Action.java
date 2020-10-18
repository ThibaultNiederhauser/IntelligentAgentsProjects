import logist.task.Task;

public class Action {

    public enum Type {DELIVER, PICKUP}
    public Type type;
    public Task task;

    public Action(Task task){
        this.task = task;
        this.type = Type.DELIVER;
    }

    public Action(Type type, Task task){
        this.type = type;
        this.task = task;
    }
}

