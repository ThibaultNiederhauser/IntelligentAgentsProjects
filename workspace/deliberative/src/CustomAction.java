import logist.task.Task;

public class CustomAction {

    public enum Type {DELIVER, PICKUP}
    public Type type;
    public Task task;

    /**public CustomAction(Task task){
        this.task = task;
        this.type = Type.DELIVER;
    }**/

    public CustomAction(Type type, Task task){
        this.type = type;
        this.task = task;
    }
}

