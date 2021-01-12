package uberapp.balran.uberapp.pojos

class UserDriver {
    var name: String? = null
    var id: String? = null
    var email: String? = null
    var connected: String? = null
    var matricula: String? = null
    var phone: String? = null
    var dni: String? = null
    var latitude: String? = null
    var longitude: String? = null

    constructor() {}
    constructor(name: String?, id: String?, email: String?, connected: String?, matricula: String?, phone: String?, dni: String?, latitude: String?, longitude: String?) {
        this.name = name
        this.id = id
        this.email = email
        this.connected = connected
        this.matricula = matricula
        this.phone = phone
        this.dni = dni
        this.latitude = latitude
        this.longitude = longitude
    }
}