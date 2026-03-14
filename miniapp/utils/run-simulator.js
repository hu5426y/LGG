const ROUTES = [
  {
    id: 'campus_loop',
    name: '\u6821\u56ed\u73af\u7ebf',
    center: { latitude: 31.2304, longitude: 121.4737 },
    points: [
      [0, 0], [0.0004, 0.0002], [0.0008, 0.0004], [0.0012, 0.0003], [0.0016, 0],
      [0.0012, -0.0005], [0.0007, -0.0008], [0.0001, -0.0009], [-0.0004, -0.0007], [-0.0006, -0.0002], [-0.0002, 0.0001]
    ]
  },
  {
    id: 'library_run',
    name: '\u56fe\u4e66\u9986\u6298\u8fd4',
    center: { latitude: 31.2318, longitude: 121.4722 },
    points: [
      [0, 0], [0.0003, 0.0001], [0.0008, 0.0001], [0.0012, 0.0001], [0.0016, 0.0001],
      [0.0012, -0.0002], [0.0008, -0.0002], [0.0003, -0.0002], [-0.0001, 0]
    ]
  },
  {
    id: 'stadium_interval',
    name: '\u64cd\u573a\u95f4\u6b47',
    center: { latitude: 31.2297, longitude: 121.4749 },
    points: [
      [0, 0], [0.0002, 0.0003], [0.0005, 0.0004], [0.0008, 0.0002], [0.0009, -0.0002],
      [0.0006, -0.0005], [0.0002, -0.0005], [-0.0001, -0.0002], [0, 0.0001]
    ]
  }
]

const PACES = [
  { id: 'easy', label: '\u8f7b\u677e\u8dd1', secondsPerKm: 420 },
  { id: 'steady', label: '\u7a33\u6001\u8dd1', secondsPerKm: 360 },
  { id: 'tempo', label: '\u8282\u594f\u8dd1', secondsPerKm: 320 }
]

const MIN_SIMULATION_DURATION_SECONDS = 330
const MIN_SIMULATION_DISTANCE_METERS = 650
const MIN_SIMULATION_ROUTE_POINTS = 12

function haversineMeters(pointA, pointB) {
  const toRad = (value) => value * Math.PI / 180
  const earthRadiusMeters = 6371000
  const lat1 = toRad(pointA.latitude)
  const lat2 = toRad(pointB.latitude)
  const deltaLat = toRad(pointB.latitude - pointA.latitude)
  const deltaLng = toRad(pointB.longitude - pointA.longitude)
  const a = Math.sin(deltaLat / 2) ** 2 +
    Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLng / 2) ** 2
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  return earthRadiusMeters * c
}

function createStaticPoint(center, offset) {
  return {
    latitude: Number((center.latitude + offset[0]).toFixed(6)),
    longitude: Number((center.longitude + offset[1]).toFixed(6))
  }
}

function toPoint(basePoint, timestamp, speedMetersPerSecond) {
  return {
    latitude: basePoint.latitude,
    longitude: basePoint.longitude,
    timestamp,
    accuracy: 8,
    speedMetersPerSecond: Number(speedMetersPerSecond.toFixed(2))
  }
}

function buildSimulatedRoute(routeId, paceId) {
  const route = ROUTES.find((item) => item.id === routeId) || ROUTES[0]
  const pace = PACES.find((item) => item.id === paceId) || PACES[0]
  const now = Date.now()
  const speedMetersPerSecond = 1000 / pace.secondsPerKm
  const basePoints = []
  let totalDistanceMeters = 0
  let loopCount = 0

  while (
    !basePoints.length ||
    totalDistanceMeters < MIN_SIMULATION_DISTANCE_METERS ||
    totalDistanceMeters / speedMetersPerSecond < MIN_SIMULATION_DURATION_SECONDS ||
    basePoints.length < MIN_SIMULATION_ROUTE_POINTS
  ) {
    for (const offset of route.points) {
      const point = createStaticPoint(route.center, offset)
      const previousPoint = basePoints[basePoints.length - 1]
      if (previousPoint) {
        totalDistanceMeters += haversineMeters(previousPoint, point)
      }
      basePoints.push(point)
    }
    loopCount += 1
  }

  let elapsedMillis = 0
  const points = basePoints.map((point, index) => {
    if (index > 0) {
      const segmentDistanceMeters = haversineMeters(basePoints[index - 1], point)
      elapsedMillis += Math.max(1000, Math.round(segmentDistanceMeters / speedMetersPerSecond * 1000))
    }
    return toPoint(point, now + elapsedMillis, speedMetersPerSecond)
  })

  return {
    route,
    pace,
    points,
    loopCount,
    distanceKm: Number((totalDistanceMeters / 1000).toFixed(3)),
    durationSeconds: Math.round(elapsedMillis / 1000)
  }
}

module.exports = {
  ROUTES,
  PACES,
  buildSimulatedRoute
}
