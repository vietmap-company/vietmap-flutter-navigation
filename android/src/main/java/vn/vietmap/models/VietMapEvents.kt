package vn.vietmap.models


enum class VietMapEvents(val value: String) {
    MAP_READY("mapReady"),
    ROUTE_BUILDING("routeBuilding"),
    ROUTE_BUILT("routeBuilt"),
    ROUTE_BUILD_FAILED("routeBuildFailed"),
    ROUTE_BUILD_CANCELLED("routeBuildCancelled"),
    ROUTE_BUILD_NO_ROUTES_FOUND("routeBuildNoRoutesFound"),
    PROGRESS_CHANGE("progressChange"),
    USER_OFF_ROUTE("userOffRoute"),
    MILESTONE_EVENT("milestoneEvent"),
    NAVIGATION_RUNNING("navigationRunning"),
    NAVIGATION_CANCELLED("navigationCancelled"),
    NAVIGATION_FINISHED("navigationFinished"),
    FASTER_ROUTE_FOUND("fasterRouteFound"),
    SPEECH_ANNOUNCEMENT("speechAnnouncement"),
    BANNER_INSTRUCTION("bannerInstruction"),
    ON_ARRIVAL("onArrival"),
    FAILED_TO_REROUTE("failedToReroute"),
    REROUTE_ALONG("rerouteAlong"),
    ON_MAP_MOVE("onMapMove"),
    ON_MAP_LONG_CLICK("onMapLongClick"),
    ON_MAP_MOVE_END("onMapMoveEnd"),
    ON_MAP_CLICK("onMapClick"),
    ON_MAP_RENDERED("onMapRendered"),
    ON_NEW_ROUTE_SELECTED("onNewRouteSelected"),

}
