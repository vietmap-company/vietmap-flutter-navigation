package vn.vietmap.models

import android.location.Location
import android.os.Build
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mapbox.api.directions.v5.models.BannerInstructions
import org.json.JSONObject
import vn.vietmap.services.android.navigation.v5.routeprogress.RouteProgress

class VietMapRouteProgressEvent(progress: RouteProgress,location: Location, snappedLocation: Location) {

    var arrived: Boolean? = null
    private var distanceRemaining: Float? = null
    private var durationRemaining: Double? = null
    private var distanceTraveled: Float? = null
    private var currentLegDistanceTraveled: Float? = null
    private var currentLegDistanceRemaining: Float? = null
    private var distanceToNextTurn: Float? = null
    private var currentStepInstruction: String? = null
    private var currentModifier:String? = null
    private var currentModifierType:String? = null

    private var legIndex: Int? = null
    var stepIndex: Int? = null
    private var currentLeg: VietMapRouteLeg? = null
    var priorLeg: VietMapRouteLeg? = null
    lateinit var remainingLegs: List<VietMapRouteLeg>
    private val _location = location
    private val _snappedLocation = snappedLocation
    init {

        val bannerInstructionsList: List<BannerInstructions> =
            progress.currentLegProgress().currentStep().bannerInstructions() as List<BannerInstructions>
        currentModifier = bannerInstructionsList?.get(0)?.primary()?.modifier()
        currentModifierType= bannerInstructionsList?.get(0)?.primary()?.type()
        // val util = RouteUtils()
        // arrived = util.isArrivalEvent(progress) && util.isLastLeg(progress)
        distanceRemaining = progress.distanceRemaining().toFloat()
        durationRemaining = progress.durationRemaining()
        distanceTraveled = progress.distanceTraveled().toFloat()
        legIndex = progress.currentLegProgress()?.stepIndex()
        // stepIndex = progress.stepIndex
//        val leg = progress.currentLegProgress()?.routeLeg
        val leg = progress.currentLeg()
        if (leg != null)
            currentLeg = VietMapRouteLeg(leg)
        currentStepInstruction = bannerInstructionsList?.get(0)
            ?.primary()
            ?.text()
        currentLegDistanceTraveled = progress.currentLegProgress()?.distanceTraveled()?.toFloat()
        currentLegDistanceRemaining = progress.currentLegProgress()?.distanceRemaining()?.toFloat()
        distanceToNextTurn = progress.stepDistanceRemaining().toFloat()
    }

    fun toJson(): String {
        return Gson().toJson(toJsonObject())
    }

    private fun toJsonObject(): JsonObject {
        val json = JsonObject()
        addProperty(json, "distanceRemaining", distanceRemaining)
        addProperty(json, "durationRemaining", durationRemaining)
        addProperty(json, "distanceTraveled", distanceTraveled)
        addProperty(json, "legIndex", legIndex)
        addProperty(json, "currentLegDistanceRemaining", currentLegDistanceRemaining)
        addProperty(json, "currentLegDistanceTraveled", currentLegDistanceTraveled)
        addProperty(json, "currentStepInstruction", currentStepInstruction)
        addProperty(json, "distanceToNextTurn", distanceToNextTurn)
        addProperty(json, "currentModifier", currentModifier)
        addProperty(json, "currentModifierType", currentModifierType)
        if (currentLeg != null) {
            json.add("currentLeg", currentLeg!!.toJsonObject())
        }
       json.add("location",locationToJsonObject(_location))
       json.add("snappedLocation",locationToJsonObject(_snappedLocation))
        return json
    }

    private fun addProperty(json: JsonObject, prop: String, value: Double?) {
        if (value != null) {
            json.addProperty(prop, value)
        }
    }

    private fun addProperty(json: JsonObject, prop: String, value: Int?) {
        if (value != null) {
            json.addProperty(prop, value)
        }
    }
    private fun locationToJsonObject(location: Location): JsonElement {
        val json = JSONObject()
        json.put("latitude", location.latitude)
        json.put("longitude", location.longitude)
        json.put("provider", location.provider)
        json.put("speed", location.speed)
        json.put("bearing", location.bearing)
        json.put("altitude", location.altitude)
        json.put("accuracy", location.accuracy)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            json.put("speedAccuracyMetersPerSecond", location.speedAccuracyMetersPerSecond)
        }
        val jsonString = json.toString()
        return JsonParser.parseString(jsonString)

    }
    private fun addProperty(json: JsonObject, prop: String, value: String?) {
        if (value?.isNotEmpty() == true) {
            json.addProperty(prop, value)
        }
    }

    private fun addProperty(json: JsonObject, prop: String, value: Float?) {
        if (value != null) {
            json.addProperty(prop, value)
        }
    }
}
