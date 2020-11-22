
public class Node {

    public Variables data = null;
    public double prob;
    public int deep;
    public Long cost;

    public Node(Variables data, double prob, int deep, Long cost) {
        if (data != null)
            this.data = data.copy();
        else this.data = null;
        this.prob = prob;
        this.deep = deep;
        this.cost = cost;
    }

    public Node light(){
        return new Node(null, this.prob, this.deep, this.cost);
    }


}