package uberapp.balran.uberapp.pojos;

public class Trip {
    String id_driver;
    String id_client;
    String destination;
    String start;
    String state;

    public Trip() {
    }

    public Trip(String id_driver, String id_client, String destination, String start, String state) {
        this.id_driver = id_driver;
        this.destination = destination;
        this.start = start;
        this.state = state;
        this.id_client = id_client;
    }

    public String getId_client() {
        return id_client;
    }

    public void setId_client(String id_client) {
        this.id_client = id_client;
    }


    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getId_driver() {
        return id_driver;
    }

    public void setId_driver(String id_driver) {
        this.id_driver = id_driver;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }
}
