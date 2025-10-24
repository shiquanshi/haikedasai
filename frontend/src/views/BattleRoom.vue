<template>
  <div class="battle-container">
    <!-- 房间列表 -->
    <div v-if="currentView === 'lobby'" class="lobby-view">
      <div class="lobby-header">
        <h1>对战大厅</h1>
        <el-button type="primary" @click="showCreateDialog = true">
          <el-icon><Plus /></el-icon>
          创建房间
        </el-button>
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

    <!-- 房间内 -->
    <div v-else-if="currentView === 'room'" class="room-view">
      <div class="room-header">
        <div class="room-title">
          <h2>{{ currentRoom?.roomName }}</h2>
          <el-tag>{{ currentRoom?.status }}</el-tag>
        </div>
        <el-button @click="leaveRoom">离开房间</el-button>
      </div>

      <div class="room-content">
        <!-- 玩家列表 -->
        <div class="players-section">
          <h3>玩家列表 ({{ currentRoom?.currentPlayers }}/{{ currentRoom?.maxPlayers }})</h3>
          <div class="player-list">
            <div 
              v-for="player in currentRoom?.players" 
              :key="player.userId"
              class="player-item"
              :class="{ 'is-ready': player.ready, 'is-host': player.userId === currentRoom?.hostUserId }"
            >
              <el-avatar>{{ player.username.charAt(0) }}</el-avatar>
              <div class="player-info">
                <span class="player-name">{{ player.username }}</span>
                <span v-if="player.userId === currentRoom?.hostUserId" class="host-badge">房主</span>
              </div>
              <el-tag v-if="player.ready" type="success" size="small">已准备</el-tag>
              <el-tag v-else size="small">未准备</el-tag>
            </div>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="actions-section">
          <el-button 
            v-if="!isHost"
            :type="myPlayer?.ready ? 'info' : 'success'"
            @click="toggleReady"
            :disabled="currentRoom?.status !== 'WAITING'"
          >
            {{ myPlayer?.ready ? '取消准备' : '准备' }}
          </el-button>
          <el-button 
            v-if="isHost"
            type="primary"
            @click="startGame"
            :disabled="!canStart"
          >
            开始游戏
          </el-button>
        </div>
      </div>
    </div>

    <!-- 游戏中 -->
    <div v-else-if="currentView === 'playing'" class="playing-view">
      <div class="game-header">
        <div class="round-info">
          <h2>第 {{ currentRound }} / {{ currentRoom?.totalRounds }} 轮</h2>
          <div class="countdown" :class="{ 'warning': countdown <= 10 }">
            <el-icon><Timer /></el-icon>
            <span>{{ countdown }}s</span>
          </div>
        </div>
      </div>

      <div class="game-content">
        <!-- 题目 -->
        <div class="question-section">
          <h3>题目</h3>
          <div class="question-content">{{ currentQuestion }}</div>
        </div>

        <!-- 答题区 -->
        <div class="answer-section">
          <el-input
            v-model="myAnswer"
            type="textarea"
            :rows="6"
            placeholder="请输入你的答案..."
            :disabled="hasSubmitted"
            maxlength="1000"
            show-word-limit
          />
          <el-button 
            type="primary" 
            @click="submitAnswer"
            :disabled="hasSubmitted || !myAnswer.trim()"
            :loading="submitting"
          >
            {{ hasSubmitted ? '已提交' : '提交答案' }}
          </el-button>
        </div>

        <!-- 提交状态 -->
        <div class="submit-status">
          <div class="status-item" v-for="player in currentRoom?.players" :key="player.userId">
            <span>{{ player.username }}</span>
            <el-icon v-if="submittedPlayers.includes(player.userId)" color="green"><Check /></el-icon>
            <el-icon v-else color="gray"><Clock /></el-icon>
          </div>
        </div>
      </div>
    </div>

    <!-- 评分结果 -->
    <div v-else-if="currentView === 'scoring'" class="scoring-view">
      <div class="scoring-header">
        <h2>第 {{ scoreResult?.round }} 轮结果</h2>
      </div>

      <div class="scoring-content">
        <!-- 本轮得分 -->
        <div class="round-scores">
          <h3>本轮得分</h3>
          <div class="score-list">
            <div 
              v-for="score in scoreResult?.scores" 
              :key="score.userId"
              class="score-item"
            >
              <div class="score-player">
                <span class="rank">{{ getRank(score.userId, 'current') }}</span>
                <span class="name">{{ score.username }}</span>
              </div>
              <div class="score-details">
                <div class="answer">答案: {{ score.answer || '未提交' }}</div>
                <div class="feedback">{{ score.feedback }}</div>
              </div>
              <div class="score-value">{{ score.score }} 分</div>
            </div>
          </div>
        </div>

        <!-- 总排名 -->
        <div class="total-ranking">
          <h3>总排名</h3>
          <div class="ranking-list">
            <div 
              v-for="rank in scoreResult?.ranking?.total" 
              :key="rank.userId"
              class="ranking-item"
            >
              <span class="rank-number">{{ rank.rank }}</span>
              <span class="rank-name">{{ rank.username }}</span>
              <span class="rank-score">{{ rank.score }} 分</span>
            </div>
          </div>
        </div>
      </div>

      <div class="scoring-actions">
        <el-button v-if="isGameFinished" type="primary" @click="backToLobby">返回大厅</el-button>
        <div v-else class="waiting-text">等待下一轮...</div>
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
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Timer, Check, Clock } from '@element-plus/icons-vue'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

// 当前视图
const currentView = ref<'lobby' | 'room' | 'playing' | 'scoring'>('lobby')

// 房间列表
const rooms = ref<any[]>([])

// 当前房间
const currentRoom = ref<any>(null)

// 当前用户信息（从localStorage获取）
const userId = ref(parseInt(localStorage.getItem('userId') || '0'))
const username = ref(localStorage.getItem('username') || '游客')

// WebSocket相关
let stompClient: Client | null = null
let subscription: any = null

// 创建房间表单
const showCreateDialog = ref(false)
const createForm = ref({
  roomName: '',
  topic: '',
  difficulty: 'medium',
  maxPlayers: 4,
  totalRounds: 3
})

// 游戏状态
const currentQuestion = ref('')
const currentRound = ref(1)
const countdown = ref(60)
const myAnswer = ref('')
const hasSubmitted = ref(false)
const submitting = ref(false)
const submittedPlayers = ref<number[]>([])
const scoreResult = ref<any>(null)
const isGameFinished = ref(false)

// 计算属性
const isHost = computed(() => userId.value === currentRoom.value?.hostUserId)
const myPlayer = computed(() => 
  currentRoom.value?.players?.find((p: any) => p.userId === userId.value)
)
const canStart = computed(() => {
  if (!currentRoom.value || currentRoom.value.status !== 'WAITING') return false
  const players = currentRoom.value.players || []
  return players.length >= 2 && players.every((p: any) => p.ready || p.userId === currentRoom.value.hostUserId)
})

// WebSocket连接
const connectWebSocket = () => {
  const socket = new SockJS('http://localhost:8080/ws-battle')
  stompClient = new Client({
    webSocketFactory: () => socket,
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
  })

  stompClient.onConnect = () => {
    console.log('WebSocket connected')
    // 连接成功后加载房间列表
    loadRooms()
  }

  stompClient.onStompError = (frame) => {
    console.error('STOMP error:', frame)
    ElMessage.error('连接失败')
  }

  stompClient.activate()
}

// 订阅房间消息
const subscribeToRoom = (roomId: string) => {
  if (!stompClient) return

  subscription = stompClient.subscribe(`/topic/battle/${roomId}`, (message) => {
    const data = JSON.parse(message.body)
    handleRoomMessage(data)
  })
}

// 处理房间消息
const handleRoomMessage = (message: any) => {
  console.log('Received message:', message)

  switch (message.type) {
    case 'ROOM_CREATED':
    case 'PLAYER_JOINED':
    case 'PLAYER_READY':
      currentRoom.value = message.data
      break

    case 'PLAYER_LEFT':
      if (message.data.userId === userId.value) {
        backToLobby()
      } else {
        loadRoomInfo()
      }
      break

    case 'GAME_START':
      currentView.value = 'playing'
      resetGameState()
      break

    case 'QUESTION_GENERATED':
      currentQuestion.value = message.data.content
      currentRound.value = message.data.round
      countdown.value = message.data.timeLimit
      break

    case 'COUNTDOWN':
      countdown.value = message.data.remaining
      break

    case 'ANSWER_SUBMITTED':
      if (!submittedPlayers.value.includes(message.data.userId)) {
        submittedPlayers.value.push(message.data.userId)
      }
      break

    case 'SCORES_UPDATED':
      scoreResult.value = message.data
      currentView.value = 'scoring'
      break

    case 'ROUND_FINISHED':
      // 轮次结束，等待3秒后进入下一轮
      break

    case 'GAME_FINISHED':
      isGameFinished.value = true
      break

    case 'ERROR':
      ElMessage.error(message.data.message || '操作失败')
      break
  }
}

// 加载房间列表
const loadRooms = () => {
  if (!stompClient) return

  stompClient.publish({
    destination: '/app/battle/rooms',
    body: JSON.stringify({})
  })

  // 临时订阅获取房间列表
  const tempSub = stompClient.subscribe('/user/queue/rooms', (message) => {
    rooms.value = JSON.parse(message.body)
    tempSub.unsubscribe()
  })
}

// 创建房间
const createRoom = () => {
  if (!createForm.value.roomName || !createForm.value.topic) {
    ElMessage.warning('请填写完整信息')
    return
  }

  if (!stompClient) return

  stompClient.publish({
    destination: '/app/battle/create',
    body: JSON.stringify({
      ...createForm.value,
      userId: userId.value,
      username: username.value
    })
  })

  showCreateDialog.value = false
  // 重置表单
  createForm.value = {
    roomName: '',
    topic: '',
    difficulty: 'medium',
    maxPlayers: 4,
    totalRounds: 3
  }

  // 订阅创建结果
  const tempSub = stompClient.subscribe('/user/queue/create', (message) => {
    const result = JSON.parse(message.body)
    if (result.success) {
      currentRoom.value = result.room
      currentView.value = 'room'
      subscribeToRoom(result.room.roomId)
      ElMessage.success('房间创建成功')
    } else {
      ElMessage.error(result.message || '创建失败')
    }
    tempSub.unsubscribe()
  })
}

// 加入房间
const joinRoom = (roomId: string) => {
  if (!stompClient) return

  stompClient.publish({
    destination: '/app/battle/join',
    body: JSON.stringify({
      roomId,
      userId: userId.value,
      username: username.value
    })
  })

  // 订阅加入结果
  const tempSub = stompClient.subscribe('/user/queue/join', (message) => {
    const result = JSON.parse(message.body)
    if (result.success) {
      currentRoom.value = result.room
      currentView.value = 'room'
      subscribeToRoom(roomId)
      ElMessage.success('加入房间成功')
    } else {
      ElMessage.error(result.message || '加入失败')
    }
    tempSub.unsubscribe()
  })
}

// 离开房间
const leaveRoom = () => {
  if (!stompClient || !currentRoom.value) return

  stompClient.publish({
    destination: '/app/battle/leave',
    body: JSON.stringify({
      roomId: currentRoom.value.roomId,
      userId: userId.value
    })
  })

  if (subscription) {
    subscription.unsubscribe()
    subscription = null
  }

  backToLobby()
}

// 切换准备状态
const toggleReady = () => {
  if (!stompClient || !currentRoom.value) return

  stompClient.publish({
    destination: '/app/battle/ready',
    body: JSON.stringify({
      roomId: currentRoom.value.roomId,
      userId: userId.value
    })
  })
}

// 开始游戏
const startGame = () => {
  if (!stompClient || !currentRoom.value) return

  stompClient.publish({
    destination: '/app/battle/start',
    body: JSON.stringify({
      roomId: currentRoom.value.roomId
    })
  })
}

// 提交答案
const submitAnswer = () => {
  if (!myAnswer.value.trim() || hasSubmitted.value) return
  if (!stompClient || !currentRoom.value) return

  submitting.value = true

  stompClient.publish({
    destination: '/app/battle/answer',
    body: JSON.stringify({
      roomId: currentRoom.value.roomId,
      answer: myAnswer.value
    })
  })

  hasSubmitted.value = true
  submitting.value = false
  ElMessage.success('答案已提交')
}

// 获取排名
const getRank = (userId: number, type: 'current' | 'total') => {
  if (!scoreResult.value) return '-'
  
  const rankings = type === 'current' 
    ? scoreResult.value.ranking?.current 
    : scoreResult.value.ranking?.total
  
  const rank = rankings?.find((r: any) => r.userId === userId)
  return rank ? `#${rank.rank}` : '-'
}

// 重置游戏状态
const resetGameState = () => {
  currentQuestion.value = ''
  myAnswer.value = ''
  hasSubmitted.value = false
  submitting.value = false
  submittedPlayers.value = []
  countdown.value = 60
}

// 加载房间信息
const loadRoomInfo = () => {
  // 重新订阅房间消息以获取最新状态
  if (currentRoom.value && stompClient) {
    subscribeToRoom(currentRoom.value.roomId)
  }
}

// 返回大厅
const backToLobby = () => {
  currentView.value = 'lobby'
  currentRoom.value = null
  scoreResult.value = null
  isGameFinished.value = false
  resetGameState()
  loadRooms()
}

// 生命周期
onMounted(() => {
  connectWebSocket()
})

onUnmounted(() => {
  if (subscription) {
    subscription.unsubscribe()
  }
  if (stompClient) {
    stompClient.deactivate()
  }
})
</script>

<style scoped>
.battle-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

/* 大厅视图 */
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

.room-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.room-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.room-card h3 {
  margin: 0;
  font-size: 20px;
  color: #333;
}

.room-details {
  display: flex;
  gap: 8px;
}

.room-stats {
  display: flex;
  justify-content: space-between;
  color: #666;
  font-size: 14px;
}

/* 房间视图 */
.room-view {
  max-width: 900px;
  margin: 0 auto;
  background: white;
  border-radius: 16px;
  padding: 30px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.room-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 2px solid #f0f0f0;
}

.room-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.room-title h2 {
  margin: 0;
  color: #333;
}

.players-section h3 {
  margin-bottom: 15px;
  color: #333;
}

.player-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 30px;
}

.player-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f9f9f9;
  border-radius: 8px;
  border: 2px solid transparent;
  transition: all 0.3s;
}

.player-item.is-ready {
  background: #f0f9ff;
  border-color: #3b82f6;
}

.player-item.is-host {
  background: #fef3c7;
}

.player-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}

.player-name {
  font-weight: 500;
  color: #333;
}

.host-badge {
  background: #fbbf24;
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.actions-section {
  display: flex;
  justify-content: center;
  gap: 12px;
}

/* 游戏视图 */
.playing-view {
  max-width: 900px;
  margin: 0 auto;
  background: white;
  border-radius: 16px;
  padding: 30px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.game-header {
  margin-bottom: 30px;
}

.round-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.round-info h2 {
  margin: 0;
  color: #333;
}

.countdown {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 24px;
  font-weight: bold;
  color: #3b82f6;
}

.countdown.warning {
  color: #ef4444;
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.question-section {
  margin-bottom: 30px;
}

.question-section h3 {
  margin-bottom: 15px;
  color: #666;
}

.question-content {
  padding: 20px;
  background: #f9fafb;
  border-radius: 8px;
  font-size: 18px;
  line-height: 1.6;
  color: #333;
}

.answer-section {
  display: flex;
  flex-direction: column;
  gap: 15px;
  margin-bottom: 30px;
}

.submit-status {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 12px;
}

.status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  background: #f9f9f9;
  border-radius: 6px;
}

/* 评分视图 */
.scoring-view {
  max-width: 1000px;
  margin: 0 auto;
  background: white;
  border-radius: 16px;
  padding: 30px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.scoring-header {
  text-align: center;
  margin-bottom: 30px;
}

.scoring-header h2 {
  margin: 0;
  color: #333;
  font-size: 28px;
}

.scoring-content {
  display: grid;
  gap: 30px;
  margin-bottom: 30px;
}

.round-scores h3,
.total-ranking h3 {
  margin-bottom: 15px;
  color: #333;
}

.score-list,
.ranking-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.score-item {
  padding: 20px;
  background: #f9fafb;
  border-radius: 8px;
  border-left: 4px solid #3b82f6;
}

.score-player {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.score-player .rank {
  font-size: 18px;
  font-weight: bold;
  color: #3b82f6;
}

.score-player .name {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.score-details {
  margin-bottom: 10px;
}

.score-details .answer {
  padding: 10px;
  background: #f0f0f0;
  border-radius: 4px;
  margin-bottom: 8px;
  color: #555;
}

.score-details .score {
  font-size: 20px;
  font-weight: bold;
  color: #10b981;
  margin-bottom: 8px;
}

.score-details .feedback {
  color: #666;
  line-height: 1.5;
}

.ranking-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 15px;
  background: #f9fafb;
  border-radius: 8px;
}

.ranking-item.gold {
  background: linear-gradient(135deg, #ffd700 0%, #ffed4e 100%);
  border-left: 4px solid #ff8c00;
}

.ranking-item.silver {
  background: linear-gradient(135deg, #c0c0c0 0%, #e8e8e8 100%);
  border-left: 4px solid #808080;
}

.ranking-item.bronze {
  background: linear-gradient(135deg, #cd7f32 0%, #e6a859 100%);
  border-left: 4px solid #8b4513;
}

.ranking-player {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ranking-player .rank {
  font-size: 24px;
  font-weight: bold;
  color: #3b82f6;
}

.ranking-player .name {
  font-size: 18px;
  font-weight: 500;
  color: #333;
}

.ranking-score {
  font-size: 20px;
  font-weight: bold;
  color: #10b981;
}

.scoring-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .room-grid {
    grid-template-columns: 1fr;
  }

  .lobby-header h1 {
    font-size: 24px;
  }

  .room-view,
  .playing-view,
  .scoring-view {
    padding: 20px;
  }

  .submit-status {
    grid-template-columns: 1fr;
  }
}
</style>