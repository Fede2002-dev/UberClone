package uberapp.balran.uberapp.pojos

class Trip {
    var id_driver: String? = null
    var id_client: String? = null
    var destination: String? = null
    var start: String? = null
    var state: String? = null

    constructor() {}
    constructor(id_driver: String?, id_client: String?, destination: String?, start: String?, state: String?) {
        this.id_driver = id_driver
        this.destination = destination
        this.start = start
        this.state = state
        this.id_client = id_client
    }
}