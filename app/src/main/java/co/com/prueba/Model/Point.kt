package co.com.prueba.Model

class Point {

    var pickup_address: String? = null

    var pickup_location = PickupLocation()

    class PickupLocation {

        var coordinates = ArrayList<Double>()
    }
}