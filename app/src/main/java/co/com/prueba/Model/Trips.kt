package co.com.prueba.Model

import java.io.Serializable

class Trips : Serializable {

    val start = Point()

    val end = Point()

    val city = City()

    val passenger = Name()

    val driver = Name()

    val car = Car()

    val status: String? = null

    val check_code: String? = null

    val price: String? = null

    val driver_location = DriverLocation()
}