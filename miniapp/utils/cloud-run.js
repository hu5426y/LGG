function getCloudDatabase() {
  const app = getApp()
  if (!app || !app.globalData || !app.globalData.cloudReady || !wx.cloud) {
    return null
  }
  try {
    return wx.cloud.database({
      env: app.globalData.cloudEnvId
    })
  } catch (error) {
    return null
  }
}

function readFallbackState() {
  return wx.getStorageSync('campusRunCloudFallback') || {
    sessions: {},
    batches: []
  }
}

function writeFallbackState(state) {
  wx.setStorageSync('campusRunCloudFallback', state)
}

function createFallbackId(prefix) {
  return `${prefix}_${Date.now()}_${Math.random().toString(16).slice(2, 8)}`
}

function addCollectionRecord(collection, data) {
  const db = getCloudDatabase()
  if (!db) {
    return Promise.resolve({ _id: '' })
  }
  return db.collection(collection).add({ data })
}

function updateCollectionRecord(collection, docId, data) {
  const db = getCloudDatabase()
  if (!db || !docId) {
    return Promise.resolve()
  }
  return db.collection(collection).doc(docId).update({ data })
}

async function createRunSession(data) {
  const db = getCloudDatabase()
  if (db) {
    const result = await db.collection('run_raw_sessions').add({
      data: {
        ...data,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      }
    })
    return result._id || ''
  }

  const fallback = readFallbackState()
  const sessionId = createFallbackId('session')
  fallback.sessions[sessionId] = {
    ...data,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    points: []
  }
  writeFallbackState(fallback)
  return sessionId
}

async function appendRunPoints(sessionId, batchIndex, points) {
  const normalizedPoints = Array.isArray(points) ? points : []
  const db = getCloudDatabase()
  if (db && sessionId) {
    await db.collection('run_raw_points').add({
      data: {
        sessionId,
        batchIndex,
        pointCount: normalizedPoints.length,
        points: normalizedPoints,
        createdAt: new Date().toISOString()
      }
    })
    return
  }

  const fallback = readFallbackState()
  fallback.batches.push({
    sessionId,
    batchIndex,
    pointCount: normalizedPoints.length,
    points: normalizedPoints
  })
  if (fallback.sessions[sessionId]) {
    fallback.sessions[sessionId].points = fallback.sessions[sessionId].points.concat(normalizedPoints)
    fallback.sessions[sessionId].updatedAt = new Date().toISOString()
  }
  writeFallbackState(fallback)
}

async function finalizeRunSession(sessionId, summary) {
  const db = getCloudDatabase()
  if (db && sessionId) {
    await db.collection('run_raw_sessions').doc(sessionId).update({
      data: {
        ...summary,
        updatedAt: new Date().toISOString()
      }
    })
    return
  }

  const fallback = readFallbackState()
  if (fallback.sessions[sessionId]) {
    fallback.sessions[sessionId] = {
      ...fallback.sessions[sessionId],
      ...summary,
      updatedAt: new Date().toISOString()
    }
    writeFallbackState(fallback)
  }
}

module.exports = {
  getCloudDatabase,
  createRunSession,
  appendRunPoints,
  finalizeRunSession
}
