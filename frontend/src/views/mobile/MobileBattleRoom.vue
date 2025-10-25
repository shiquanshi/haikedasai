<template>
  <div class="mobile-battle-container">
    <!-- 头部区域 -->
    <div class="mobile-header">
      <h1 class="header-title">对战大厅</h1>
      <el-button 
        type="primary" 
        size="small" 
        @click="showCreateDialog = true"
        class="create-button"
      >
        <el-icon><Plus /></el-icon>
        创建房间
      </el-button>
    </div>

    <!-- 主要内容区 -->
    <div class="mobile-content">
      <!-- 加入房间输入 -->
      <div class="join-room-section">
        <el-input 
          v-model="roomIdToJoin" 
          placeholder="输入房间号"
          clearable
          size="large"
          style="width: 100%; margin-bottom: 12px;"
        />
        <el-button 
          type="primary" 
          size="large" 
          @click="joinRoomById"
          style="width: 100%;"
          :loading="joining"
        >
          进入房间
        </el-button>
      </div>

      <!-- 房间列表 -->
      <div class="room-list">
        <h2 class="list-title">房间列表</h2>
        <el-empty v-if="rooms.length === 0" description="暂无房间，快来创建一个吧！" />
        <div v-else class="room-list-content">
          <div v-for="room in rooms" :key="room.roomId" class="room-card">
            <div class="room-info">
              <h3 class="room-name">{{ room.roomName }}</h3>
              <div class="room-details">
                <el-tag size="small">{{ room.topic }}</el-tag>
                <el-tag size="small" type="warning">{{ room.difficulty }}</el-tag>
              </div>
              <div class="room-stats">
                <span>玩家: {{ room.currentPlayers }}/{{ room.maxPlayers }}</span>
                <span>轮次: {{ room.totalRounds }}</span>
              </div>
              <div class="room-id">
                <span>房间码: {{ room.roomId }}</span>
              </div>
            </div>
            <el-button 
              type="primary" 
              size="small"
              :disabled="room.currentPlayers >= room.maxPlayers || room.status !== 'WAITING'"
              @click="joinRoom(room.roomId)"
            >
              加入房间
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 返回首页按钮 -->
    <div class="bottom-action">
      <el-button 
        type="default" 
        size="large" 
        @click="$router.push('/mobile')"
        style="width: 90%; margin: 20px auto;"
      >
        <el-icon><HomeFilled /></el-icon>
        返回首页
      </el-button>
    </div>

    <!-- 创建房间对话框 -->
    <el-dialog v-model="showCreateDialog" title="创建房间" width="90%" center>
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="房间名称">
          <el-input v-model="createForm.roomName" placeholder="请输入房间名称" size="large" />
        </el-form-item>
        <el-form-item label="答题主题">
          <el-input v-model="createForm.topic" placeholder="例如: 编程、历史、科学等" size="large" />
        </el-form-item>
        <el-form-item label="学习场景">
          <el-input v-model="createForm.scenario" placeholder="学习场景（可选）" size="large" />
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="createForm.difficulty" placeholder="选择难度" size="large">
            <el-option label="简单" value="easy" />
            <el-option label="中等" value="medium" />
            <el-option label="困难" value="hard" />
          </el-select>
        </el-form-item>
        <el-form-item label="最大玩家">
          <el-input-number v-model="createForm.maxPlayers" :min="2" :max="8" size="large" />
        </el-form-item>
        <el-form-item label="轮次数">
          <el-input-number v-model="createForm.totalRounds" :min="1" :max="10" size="large" />
        </el-form-item>
        <el-form-item label="答题时间">
          <el-input-number v-model="createForm.timeLimit" :min="10" :max="300" size="large" />
          <div class="time-limit-unit">秒</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="createRoom">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, HomeFilled } from '@element-plus/icons-vue'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'
import { useUserStore } from '../../stores/user'

const router = useRouter()

// 房间列表
const rooms = ref<any[]>([])
const roomIdToJoin = ref('')

// 当前用户信息（从userStore获取）
const userStore = useUserStore()
const userId = computed(() => userStore.userInfo?.id || 0)
const username = computed(() => userStore.userInfo?.username || '游客')

// WebSocket相关
let stompClient: Client | null = null

// 创建房间表单
const showCreateDialog = ref(false)
const createForm = ref({
  roomName: '',
  topic: '',
  scenario: '',
  difficulty: 'medium',
  maxPlayers: 4,
  totalRounds: 3,
  timeLimit: 60
})

const joining = ref(false) // 防止重复加入房间

// WebSocket连接
const connectWebSocket = () => {
  const token = userStore.token?.trim()
  
  if (!token) {
    ElMessage.error('未登录或登录已过期，请先登录')
    return
  }
  
  // SockJS需要使用HTTP/HTTPS协议，内部会自动转换为WebSocket连接
  const protocol = window.location.protocol
  const host = window.location.host
  const wsUrl = `${protocol}//${host}/api/ws-battle`
  const socket = new SockJS(wsUrl)
  stompClient = new Client({
    webSocketFactory: () => socket,
    connectHeaders: {
      Authorization: `Bearer ${token}`
    },
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
  })

  stompClient.onConnect = () => {
    console.log('WebSocket connected')
    
    // 订阅全局房间列表更新消息
    stompClient!.subscribe('/topic/battle/roomlist', (msg) => {
      const message = JSON.parse(msg.body)
      console.log('收到房间列表更新消息:', message)
      if (message.type === 'ROOM_LIST_UPDATED') {
        console.log('房间列表已更新，重新加载')
        loadRooms()
      }
    })
    
    // 连接成功后加载房间列表
    loadRooms()
  }

  stompClient.onStompError = (frame) => {
    console.error('STOMP error:', frame)
    ElMessage.error('连接失败')
  }

  stompClient.activate()
}

// 加载房间列表
const loadRooms = async () => {
  if (!stompClient) return

  // 检查连接是否已激活
  if (!stompClient.connected) {
    console.warn('WebSocket连接未就绪，跳过加载房间列表')
    return
  }

  // 先订阅获取房间列表(必须包含userId前缀)
  const tempSub = stompClient.subscribe('/user/queue/rooms', (msg) => {
    rooms.value = JSON.parse(msg.body)
    tempSub.unsubscribe()
  })

  // 等待订阅完全建立后再发送请求
  await new Promise(resolve => setTimeout(resolve, 100))

  // 再发送获取房间列表请求
  stompClient.publish({
    destination: '/app/battle/rooms',
    body: JSON.stringify({})
  })
}

// 创建房间
const createRoom = async () => {
  if (!createForm.value.roomName || !createForm.value.topic) {
    ElMessage.warning('请填写完整信息')
    return
  }

  if (!stompClient) {
    ElMessage.error('连接未建立，请稍后重试')
    return
  }

  // 检查连接是否已激活
  if (!stompClient.connected) {
    ElMessage.error('WebSocket连接未就绪，请稍后重试')
    return
  }

  // 先订阅创建结果
  console.log('订阅路径: /user/queue/create userId:', userId.value)
  const tempSub = stompClient.subscribe('/user/queue/create', (msg) => {
    const result = JSON.parse(msg.body)
    console.log('收到创建房间响应:', result)
    if (result.success) {
      ElMessage.success('房间创建成功')
      // 跳转到房间详情页面
      router.push(`/battle-room/${result.room.roomId}`)
    } else {
      ElMessage.error(result.message || '创建失败')
    }
    tempSub.unsubscribe()
  })

  // 等待订阅完全建立后再发送请求
  await new Promise(resolve => setTimeout(resolve, 100))

  // 再发送创建房间请求
  const requestData = {
    ...createForm.value,
    userId: userId.value,
    username: username.value
  }
  console.log('发送创建房间请求:', requestData)
  stompClient.publish({
    destination: '/app/battle/createRoom',
    body: JSON.stringify(requestData),
    headers: {
      userId: userId.value.toString(),
      username: username.value
    }
  })

  showCreateDialog.value = false
  // 重置表单
  createForm.value = {
    roomName: '',
    topic: '',
    scenario: '',
    difficulty: 'medium',
    maxPlayers: 4,
    totalRounds: 3,
    timeLimit: 60
  }
}

// 通过房间号加入房间
const joinRoomById = async () => {
  if (!roomIdToJoin.value.trim()) {
    ElMessage.warning('请输入房间号')
    return
  }
  
  const roomId = roomIdToJoin.value.trim()
  
  try {
    // 检查用户是否已有房间
    if (userStore.currentRoomId) {
      // 如果用户已有房间并且和要加入的房间相同，直接跳转
      if (userStore.currentRoomId === roomId) {
        ElMessage.info('您已在该房间中，正在跳转...')
        router.push(`/battle-room/${roomId}`)
        // 清空输入框
        roomIdToJoin.value = ''
        return
      }
      
      // 如果用户已有房间但想加入新房间，先离开老房间
      try {
        console.log('用户已有房间，尝试离开老房间:', userStore.currentRoomId)
        if (stompClient && stompClient.connected) {
          stompClient.publish({
            destination: '/app/battle/leaveRoom',
            body: JSON.stringify({
              roomId: userStore.currentRoomId,
              userId: userId.value.toString()
            })
          })
        }
        // 清除用户当前房间ID
        userStore.setCurrentRoomId(null)
        ElMessage.success('已离开原有房间')
      } catch (error) {
        console.error('离开老房间失败:', error)
        // 即使离开失败，也继续尝试加入新房间
      }
    }
    
    // 使用统一的joinRoom函数来处理加入房间逻辑，确保错误处理一致
    await joinRoom(roomId)
    
    // 清空输入框
    roomIdToJoin.value = ''
  } catch (error) {
    console.error('加入房间失败:', error)
    ElMessage.error('加入房间失败，请稍后重试')
  }
}

// 加入房间
const joinRoom = async (roomId: string) => {
  if (!stompClient) {
    ElMessage.error('连接未建立，请稍后重试')
    return
  }

  // 检查连接是否已激活
  if (!stompClient.connected) {
    ElMessage.error('WebSocket连接未就绪，请稍后重试')
    return
  }

  // 防止重复请求
  if (joining.value) {
    return
  }
  joining.value = true

  // 检查用户是否已有房间
  if (userStore.currentRoomId) {
    // 如果用户已有房间并且和要加入的房间相同，直接跳转
    if (userStore.currentRoomId === roomId) {
      joining.value = false
      ElMessage.info('您已在该房间中，正在跳转...')
      router.push(`/battle-room/${roomId}`)
      return
    }
    
    // 如果用户已有房间但想加入新房间，先离开老房间
    try {
      console.log('用户已有房间，尝试离开老房间:', userStore.currentRoomId)
      stompClient.publish({
        destination: '/app/battle/leaveRoom',
        body: JSON.stringify({
          roomId: userStore.currentRoomId,
          userId: userId.value.toString()
        })
      })
      // 清除用户当前房间ID
      userStore.setCurrentRoomId(null)
      ElMessage.success('已离开原有房间')
    } catch (error) {
      console.error('离开老房间失败:', error)
      // 即使离开失败，也继续尝试加入新房间
    }
  }

  // 先订阅加入结果 - 注意：订阅路径要与后端发送路径对应
  // 后端使用 messagingTemplate.convertAndSendToUser(userId, "/queue/join", result)
  // STOMP会自动转换为 /user/{userId}/queue/join
  const tempSub = stompClient.subscribe('/user/queue/join', (msg) => {
    joining.value = false // 重置加入状态
    const result = JSON.parse(msg.body)
    console.log('收到加入房间响应:', result)
    if (result.success) {
      ElMessage.success('加入房间成功')
      // 更新当前房间ID并跳转到房间详情页面
      userStore.setCurrentRoomId(roomId)
      router.push(`/battle-room/${roomId}`)
    } else {
      // 改进"已在房间中"错误的处理逻辑
      if (result.message === '已在房间中') {
        ElMessage.info('您已在房间中，正在跳转...')
        // 优先使用传入的roomId进行跳转，如果currentRoomId存在则使用currentRoomId
        if (userStore.currentRoomId) {
          router.push(`/battle-room/${userStore.currentRoomId}`)
        } else {
          // 即使currentRoomId为null，也尝试使用传入的roomId跳转
          router.push(`/battle-room/${roomId}`)
        }
      } else {
        ElMessage.error(result.message || '加入失败')
      }
    }
    tempSub.unsubscribe()
  })

  // 等待订阅完全建立后再发送请求
  await new Promise(resolve => setTimeout(resolve, 100))

  // 再发送加入房间请求
  stompClient.publish({
    destination: '/app/battle/joinRoom',
    body: JSON.stringify({
      roomId
    }),
    headers: {
      userId: userId.value.toString(),
      username: username.value
    }
  })
}

// 生命周期
onMounted(async () => {
  // 检查登录状态
  if (!userStore.isLoggedIn) {
    ElMessage.error('请先登录')
    router.push('/mobile/login')
    return
  }

  // 获取用户信息
  if (userStore.token && !userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
    } catch (error) {
      console.error('获取用户信息失败:', error)
    }
  }

  // 连接WebSocket
  connectWebSocket()
})

onUnmounted(() => {
  // 断开连接
  if (stompClient?.connected) {
    stompClient.deactivate()
  }
})

// 当组件被激活时（从其他页面返回）
onActivated(() => {
  // 如果WebSocket已连接，重新加载房间列表
  if (stompClient?.connected) {
    loadRooms()
  } else {
    // 如果WebSocket未连接，重新建立连接
    connectWebSocket()
  }
})
</script>

<style scoped>
.mobile-battle-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding-bottom: 80px;
}

.mobile-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  color: white;
  position: sticky;
  top: 0;
  z-index: 10;
}

.header-title {
  margin: 0;
  font-size: 24px;
  font-weight: bold;
}

.create-button {
  display: flex;
  align-items: center;
  gap: 6px;
}

.mobile-content {
  padding: 20px;
}

.join-room-section {
  background: white;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 20px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.room-list {
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.list-title {
  margin: 0 0 16px 0;
  font-size: 18px;
  color: #333;
  border-bottom: 2px solid #f0f0f0;
  padding-bottom: 12px;
}

.room-list-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.room-card {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 16px;
  transition: all 0.3s;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.room-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.room-info h3 {
  margin: 0 0 10px 0;
  color: #333;
  font-size: 16px;
}

.room-details {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.room-stats {
  display: flex;
  gap: 16px;
  font-size: 14px;
  color: #666;
}

.room-id {
  font-size: 14px;
  color: #409eff; /* 使用蓝色突出显示房间码 */
  font-weight: 500;
}

.bottom-action {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: white;
  padding: 16px 0;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.1);
  text-align: center;
}

.time-limit-unit {
  display: inline-block;
  margin-left: 8px;
  font-size: 14px;
  color: #666;
  vertical-align: middle;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .mobile-header {
    padding: 12px 16px;
  }
  
  .header-title {
    font-size: 20px;
  }
  
  .mobile-content {
    padding: 16px;
  }
  
  .room-card {
    padding: 14px;
  }
}
</style>