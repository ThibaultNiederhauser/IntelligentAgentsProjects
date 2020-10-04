import logist.topology.Topology.City;

public class State {
    private  City city;

    public State(City city){
        this.city = city;
    }

    public City getCity(){return this.city;}
    public void setCity(City city){this.city = city;}

}
