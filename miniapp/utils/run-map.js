function normalizePoint(point) {
  if (!point) {
    return null
  }
  const latitude = Number(point.latitude)
  const longitude = Number(point.longitude)
  if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) {
    return null
  }
  return {
    latitude,
    longitude,
    timestamp: typeof point.timestamp === 'number' ? point.timestamp : Number(point.timestamp) || Date.now(),
    accuracy: typeof point.accuracy === 'number' ? point.accuracy : null,
    speedMetersPerSecond: typeof point.speedMetersPerSecond === 'number' ? point.speedMetersPerSecond : null
  }
}

function normalizeRoutePoints(routePoints) {
  if (!Array.isArray(routePoints)) {
    return []
  }
  return routePoints
    .map(normalizePoint)
    .filter(Boolean)
}

function buildMapState(routePoints, currentPosition) {
  const normalizedRoutePoints = normalizeRoutePoints(routePoints)
  const normalizedCurrent = normalizePoint(currentPosition)
  const lastPoint = normalizedRoutePoints[normalizedRoutePoints.length - 1] || normalizedCurrent
  const includePoints = normalizedRoutePoints.map((point) => ({
    latitude: point.latitude,
    longitude: point.longitude
  }))

  if (normalizedCurrent && !normalizedRoutePoints.length) {
    includePoints.push({
      latitude: normalizedCurrent.latitude,
      longitude: normalizedCurrent.longitude
    })
  }

  return {
    mapReady: Boolean(lastPoint),
    mapLatitude: lastPoint ? lastPoint.latitude : null,
    mapLongitude: lastPoint ? lastPoint.longitude : null,
    mapMarkers: lastPoint ? [{
      id: 1,
      latitude: lastPoint.latitude,
      longitude: lastPoint.longitude,
      width: 26,
      height: 26,
      callout: {
        content: normalizedRoutePoints.length ? '当前位置' : '定位成功',
        display: 'BYCLICK',
        padding: 8,
        borderRadius: 12
      }
    }] : [],
    mapPolyline: normalizedRoutePoints.length ? [{
      points: normalizedRoutePoints.map((point) => ({
        latitude: point.latitude,
        longitude: point.longitude
      })),
      color: '#d44b2b',
      width: 6,
      dottedLine: false,
      arrowLine: false
    }] : [],
    mapIncludePoints: includePoints,
    mapScale: normalizedRoutePoints.length > 1 ? 15 : 17
  }
}

function formatLocationTimeFromPoints(routePoints) {
  const normalizedRoutePoints = normalizeRoutePoints(routePoints)
  const lastPoint = normalizedRoutePoints[normalizedRoutePoints.length - 1]
  if (!lastPoint || !lastPoint.timestamp) {
    return '--'
  }
  return new Date(lastPoint.timestamp).toLocaleTimeString('zh-CN', { hour12: false })
}

module.exports = {
  normalizePoint,
  normalizeRoutePoints,
  buildMapState,
  formatLocationTimeFromPoints
}
