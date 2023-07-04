/// All possible events that could occur in the course of navigation
///
enum MapEvent {
  mapReady,
  routeBuilding,
  routeBuilt,
  routeBuildFailed,
  routeBuildCancelled,
  routeBuildNoRoutesFound,
  progressChange,
  userOffRoute,
  milestoneEvent,
  navigationRunning,
  navigationCancelled,
  navigationFinished,
  fasterRouteFound,
  speechAnnouncement,
  bannerInstruction,
  onArrival,
  failedToReroute,
  rerouteAlong,
  onMapMove,
  onMapMoveEnd,
  onMapLongClick,
  onMapClick
}
