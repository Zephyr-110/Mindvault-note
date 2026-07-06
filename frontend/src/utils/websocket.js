import { Stomp } from '@stomp/stompjs'

const BASE_URL = 'ws://localhost:8082'

function getToken() {
  const userInfo = localStorage.getItem('userInfo')
  if (userInfo) {
    try {
      return JSON.parse(userInfo).token || ''
    } catch (e) {
      return ''
    }
  }
  return ''
}

function getUserId() {
  const userInfo = localStorage.getItem('userInfo')
  if (userInfo) {
    try {
      return JSON.parse(userInfo).id
    } catch (e) {
      return null
    }
  }
  return null
}

let stompClient = null
let subscriptions = {}

export function connect(userId) {
    return new Promise((resolve, reject) => {
        if (stompClient?.connected) {
            resolve(stompClient)
            return
        }
        const wsUrl = `${BASE_URL}/ws`
        const socket = new WebSocket(wsUrl, ['v10.stomp', 'jwt.' + getToken()])
        stompClient = Stomp.over(socket)
        stompClient.connect({}, () => {
            console.log('WebSocket 连接成功')
            resolve(stompClient)
        }, (error) => {
            console.error('WebSocket 连接失败', error)
            reject(error)
        })
    })
}

export function disconnect() {
    if (stompClient) {
        Object.values(subscriptions).forEach(sub => sub.unsubscribe())
        subscriptions = {}
        stompClient.deactivate()
        stompClient = null
    }
}

export function subscribeChat(userId, callback) {
    if (!stompClient?.connected) {
        console.warn('WebSocket 未连接')
        return
    }
    const destination = `/queue/chat.${userId}`
    if (subscriptions[destination]) {
        subscriptions[destination].unsubscribe()
    }
    subscriptions[destination] = stompClient.subscribe(destination, (message) => {
        callback(JSON.parse(message.body))
    })
}

export function sendMessage(message) {
    if (!stompClient?.connected) {
        console.warn('WebSocket 未连接，无法发送')
        return false
    }
    stompClient.publish({
        destination: '/app/chat.send',
        body: JSON.stringify(message)
    })
    return true
}

export function isConnected() {
    return stompClient?.connected
}