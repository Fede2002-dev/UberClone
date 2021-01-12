package uberapp.balran.uberapp.pojos;

public class UserDriver {
    private String name;
    private String id;
    private String email;
    private String connected;
    private String matricula;
    private String phone;
    private String dni;
    private String latitude;
    private String longitude;

    public UserDriver() {
    }

    public UserDriver(String name, String id, String email, String connected, String matricula, String phone, String dni, String latitude, String longitude) {
        this.name = name;
        this.id = id;
        this.email = email;
        this.connected = connected;
        this.matricula = matricula;
        this.phone = phone;
        this.dni = dni;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getConnected() {
        return connected;
    }

    public void setConnected(String connected) {
        this.connected = connected;
    }
}
