package uberapp.balran.uberapp.pojos

class User {
    var name: String? = null
    var id: String? = null
    var email: String? = null
    var latitude: String? = null
    var longitude: String? = null

    constructor() {}
    constructor(name: String?, email: String?, id: String?, latitude: String?, longitude: String?) {
        this.name = name
        this.email = email
        this.id = id
        this.latitude = latitude
        this.longitude = longitude
    }
}