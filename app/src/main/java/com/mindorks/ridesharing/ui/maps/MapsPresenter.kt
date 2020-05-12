package com.mindorks.ridesharing.ui.maps

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.mindorks.ridesharing.simulator.WebSocket
import com.mindorks.ridesharing.simulator.WebSocketListener
import com.mindorks.ridesharing.utils.Constants
import data.network.NetworkService
import org.json.JSONObject

class MapsPresenter(private val networkService: NetworkService) : WebSocketListener {

    companion object{
        private const val TAG ="MapsPresenter"
    }

    private var view: MapsView? = null
    private lateinit var webSocket: WebSocket

    fun onAttach(view: MapsView){
        this.view = view
        webSocket = networkService.createWebSocket(this)
        webSocket.connect()
    }

    fun onDetach(){
        webSocket.disconnect()
        view = null
    }

    fun requestNearbyCabs(latLng: com.google.android.gms.maps.model.LatLng){
        val jsonObject = JSONObject()
        jsonObject.put(Constants.TYPE, Constants.NEAR_BY_CABS)
        jsonObject.put(Constants.LAT, latLng.latitude)
        jsonObject.put(Constants.LNG, latLng.longitude)
        webSocket.sendMessage(jsonObject.toString())
    }

    fun requestCab(pickupLatLng: LatLng, dropLatLng: LatLng){
        val jsonObject = JSONObject()
        jsonObject.put(Constants.TYPE, Constants.REQUEST_CAB)
        jsonObject.put("pickUpLat", pickupLatLng.latitude)
        jsonObject.put("pickUpLng", pickupLatLng.longitude)
        jsonObject.put("dropLat", dropLatLng.latitude)
        jsonObject.put("dropLng", dropLatLng.longitude)
        webSocket.sendMessage(jsonObject.toString())
    }

    private fun handleOnMessageNearbyCabs(jsonObject: JSONObject){
        val nearbyCabLocations = arrayListOf<LatLng>()
        val jsonArray =jsonObject.getJSONArray(Constants.LOCATIONS)
        for(i in 0 until jsonArray.length()){
            val lat = (jsonArray.get(i) as JSONObject).getDouble(Constants.LAT)
            val lng = (jsonArray.get(i) as JSONObject).getDouble(Constants.LNG)
            val latLng = LatLng(lat, lng)
            nearbyCabLocations.add(latLng)
        }
        view?.showNearbyCabs(nearbyCabLocations)
    }

    override fun onConnect() {
        Log.d(TAG, "onConnect")

    }

    override fun onMessage(data: String) {
        Log.d(TAG, "onMessage data: $data")
        val jsonObject = JSONObject(data)
        when(jsonObject.getString(Constants.TYPE)){
            Constants.NEAR_BY_CABS -> {
                handleOnMessageNearbyCabs(jsonObject)
            }
            Constants.CAB_BOOKED -> {
                view?.informCabBooked()
            }
            Constants.PICKUP_PATH, Constants.TRIP_PATH -> {
                val jsonArray = jsonObject.getJSONArray("path")
                val pickUpPath = arrayListOf<LatLng>()
                for(i in 0 until jsonArray.length()){
                    val lat = (jsonArray.get(i) as JSONObject).getDouble(Constants.LAT)
                    val lng = (jsonArray.get(i) as JSONObject).getDouble(Constants.LNG)
                    val latLng = LatLng(lat, lng)
                    pickUpPath.add(latLng)
                }
                view?.showPath(pickUpPath)
            }
            Constants.LOCATION -> {
                val latCurrent = jsonObject.getDouble("lat")
                val lngCurrent = jsonObject.getDouble("lng")
                view?.updateCabLocation(LatLng(latCurrent, lngCurrent))
            }
            Constants.CAB_IS_ARRIVING -> {
                view?.informCabIsArriving()
            }
            Constants.CAB_ARRIVED -> {
                view?.informCabArrived()
            }
            Constants.TRIP_START ->{
                view?.informTripStart()
            }
            Constants.TRIP_END -> {
                view?.informTripEnd()
            }
        }

    }

    override fun onDisconnect() {
        Log.d(TAG, "onDisconnect")

    }

    override fun onError(error: String) {
        Log.d(TAG, "onError : $error")
        val jsonObject = JSONObject(error)
        when(jsonObject.getString(Constants.TYPE)){
            Constants.ROUTES_NOT_AVAILABLE -> {
                view?.showRoutesNotAvailableError()
            }
            Constants.DIRECTION_API_FAILED -> {
                view?.showDirectionApiFailedError(
                        "Direction Api Failed: " + jsonObject.getString(
                                Constants.ERROR
                ))
            }
        }

    }
}