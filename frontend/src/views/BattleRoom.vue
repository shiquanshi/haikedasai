<template>
  <div class="battle-container">
    <!-- 房间列表 -->
    <div class="lobby-view">
      <div class="lobby-header">
        <h1>对战大厅</h1>
        <div class="header-buttons">
          <el-button @click="$router.push('/')">
            <el-icon><HomeFilled /></el-icon>
            返回首页
          </el-button>
          <el-button type="primary" @click="showCreateDialog = true">
            <el-icon><Plus /></el-icon>
            创建房间
          </el-button>
        </div>
      </div>

      <div class="room-list">
        <el-empty v-if="rooms.length === 0" description="暂无房间，快来创建一个吧！" />
        <div v-else class="room-grid">
          <div v-for="room in rooms" :key="room.roomId" class="room-card">
            <div class="room-info">
              <h3>{{ room.roomName }}</h3>
              <div class="room-details">
                <el-tag size="small">{{ room.topic }}</el-tag>
                <el-tag size="small" type="warning">{{ room.difficulty }}</el-tag>
              </div>
              <div class="room-stats">
                <span>玩家: {{ room.currentPlayers }}/{{ room.maxPlayers }}</span>
                <span>轮次: {{ room.totalRounds }}</span>
              </div>
            </div>
            <el-button 
              type="primary" 
              :disabled="room.currentPlayers >= room.maxPlayers || room.status !== 'WAITING'"
              @click="joinRoom(room.roomId)"
            >
              加入房间
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 创建房间对话框 -->
    <el-dialog v-model="showCreateDialog" title="创建房间" width="500px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="房间名称">
          <el-input v-model="createForm.roomName" placeholder="请输入房间名称" />
        </el-form-item>
        <el-form-item label="答题主题">
          <el-input v-model="createForm.topic" placeholder="例如: 编程、历史、科学等" />
        </el-form-item>
        <el-form-item label="学习场景">
          <el-input v-model="createForm.scenario" placeholder="学习场景（可选）" />
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="createForm.difficulty" placeholder="选择难度">
            <el-option label="简单" value="easy" />
            <el-option label="中等" value="medium" />
            <el-option label="困难" value="hard" />
          </el-select>
        </el-form-item>
        <el-form-item label="最大玩家">
          <el-input-number v-model="createForm.maxPlayers" :min="2" :max="8" />
        </el-form-item>
        <el-form-item label="轮次数">
          <el-input-number v-model="createForm.totalRounds" :min="1" :max="10" />
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
import { ElMessage } from 'element-plus'
import { Plus, HomeFilled } from '@element-plus/icons-vue'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'
import { useUserStore } from '../stores/user'

const router = useRouter()

// 房间列表
const rooms = ref<any[]>([])

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
  totalRounds: 3
})

const joining = ref(false) // 防止重复加入房间

// WebSocket连接
const connectWebSocket = () => {
  const token = userStore.token?.trim()
  
  if (!token) {
    ElMessage.error('未登录或登录已过期，请先登录')
    return
  }
  
  // 动态获取WebSocket URL，生产环境使用wss协议
  const protocol = window.location.protocol === 'https:' ? 'https:' : 'http:'
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
    totalRounds: 3
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

  // 先订阅加入结果 - 注意：订阅路径要与后端发送路径对应
  // 后端使用 messagingTemplate.convertAndSendToUser(userId, "/queue/join", result)
  // STOMP会自动转换为 /user/{userId}/queue/join
  const tempSub = stompClient.subscribe('/user/queue/join', (msg) => {
    joining.value = false // 重置加入状态
    const result = JSON.parse(msg.body)
    console.log('收到加入房间响应:', result)
    if (result.success) {
      ElMessage.success('加入房间成功')
      // 跳转到房间详情页面
      router.push(`/battle-room/${roomId}`)
    } else {
      ElMessage.error(result.message || '加入失败')
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
    router.push('/login')
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
.battle-container {
  min-height: 100vh;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.lobby-view {
  max-width: 1200px;
  margin: 0 auto;
}

.lobby-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  color: white;
}

.lobby-header h1 {
  margin: 0;
  font-size: 32px;
}

.header-buttons {
  display: flex;
  gap: 12px;
}

.room-list {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.room-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.room-card {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 20px;
  transition: all 0.3s;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.room-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.room-info h3 {
  margin: 0 0 12px 0;
  color: #333;
  font-size: 18px;
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

/* 响应式设计 */
@media (max-width: 768px) {
  .battle-container {
    padding: 12px;
  }

  .room-grid {
    grid-template-columns: 1fr;
  }
}
</style>