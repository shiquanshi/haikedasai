<template>
  <div class="room-detail-container">
    <!-- 房间内等待 -->
    <div v-if="currentView === 'room'" class="room-view">
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

    <!-- 题目生成中 -->
    <div v-else-if="currentView === 'generating'" class="generating-view">
      <div class="generating-container">
        <div class="loading-spinner"></div>
        <div class="generating-text">{{ generatingText }}</div>
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Timer, Check, Clock } from '@element-plus/icons-vue'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'
import { useUserStore } from '../stores/user'
import request from '../api/request'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 当前视图
const currentView = ref<'room' | 'generating' | 'playing' | 'scoring'>('room')
const isGenerating = ref(false)
const generatingText = ref('')

// 当前房间
const currentRoom = ref<any>(null)

// 用户信息
const userId = computed(() => userStore.userInfo?.id || 0)
const username = computed(() => userStore.userInfo?.username || '游客')

// WebSocket相关
const stompClient = ref<Client | null>(null)
const roomSubscription = ref<any>(null)
const questionSubscription = ref<any>(null)
const scoreSubscription = ref<any>(null)

// 游戏状态
const currentRound = ref(0)
const countdown = ref(0)
const currentQuestion = ref('')
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
  if (!currentRoom.value) return false
  const allReady = currentRoom.value.players.every((p: any) => 
    p.userId === currentRoom.value.hostUserId || p.ready
  )
  return allReady && currentRoom.value.currentPlayers >= 2
})

// 连接WebSocket
const connectWebSocket = () => {
  const token = userStore.token?.trim()
  if (!token) {
    ElMessage.error('未登录或登录已过期，请先登录')
    router.push('/login')
    return
  }

  // 动态获取WebSocket URL，生产环境使用wss协议
  const protocol = window.location.protocol === 'https:' ? 'https:' : 'http:'
  const host = window.location.host
  const wsUrl = `${protocol}//${host}/api/ws`
  const socket = new SockJS(wsUrl)
  stompClient.value = new Client({
    webSocketFactory: () => socket,
    connectHeaders: {
      Authorization: `Bearer ${token}`
    },
    debug: (str) => console.log(str),
    onConnect: () => {
      console.log('WebSocket连接成功')
      subscribeToRoom()
    },
    onStompError: (frame) => {
      console.error('WebSocket错误:', frame)
      ElMessage.error('连接失败，请重试')
    }
  })
  stompClient.value.activate()
}

// 加载房间信息
const loadRoomInfo = async () => {
  const roomId = route.params.roomId as string
  if (!roomId) return

  try {
    const response = await request.get(
      `/battle/room/${roomId}`,
      {
        headers: {
          'X-User-Id': userId.value.toString(),
          'X-Username': username.value
        }
      }
    )
    console.log('房间信息加载响应:', response)
    if (response.success && response.room) {
      currentRoom.value = response.room
      console.log('房间信息已设置:', currentRoom.value)
      
      // 检查房间状态并给出相应提示
      const status = response.room.status
      if (status === 'FINISHED') {
        ElMessage.warning('该房间对战已结束')
        setTimeout(() => router.push('/battle-room'), 2000)
        return
      } else if (status === 'PLAYING') {
        ElMessage.info('房间对战进行中，正在加入...')
        currentView.value = 'playing'
      } else if (status === 'SCORING') {
        ElMessage.info('房间正在评分中，请稍候...')
      }
    } else {
      throw new Error(response.message || '房间信息格式错误')
    }
  } catch (error: any) {
    console.error('加载房间信息失败:', error)
    const errorMsg = error.response?.data?.message || error.message || '加载房间信息失败'
    
    // 根据不同错误类型给出不同提示
    if (errorMsg.includes('不存在')) {
      ElMessage.error('房间不存在或已被删除')
    } else if (errorMsg.includes('已满')) {
      ElMessage.error('房间人数已满')
    } else {
      ElMessage.error(errorMsg)
    }
    
    setTimeout(() => router.push('/battle-room'), 1500)
  }
}

// 订阅房间消息
const subscribeToRoom = () => {
  const roomId = route.params.roomId as string
  if (!roomId || !stompClient.value) return

  // 订阅房间状态更新
  roomSubscription.value = stompClient.value.subscribe(
    `/topic/battle/${roomId}`,
    (msg) => {
      const message = JSON.parse(msg.body)
      console.log('收到房间消息:', message)
      // 处理错误消息
      if (message.type === 'ERROR') {
        ElMessage.error(message.data.message || '操作失败')
        return
      }
      // 处理游戏开始消息
      if (message.type === 'GAME_START') {
        console.log('游戏开始，切换到题目生成视图')
        currentView.value = 'generating'
        isGenerating.value = true
        startTypingEffect('正在生成题目，请稍候...')
        ElMessage.success('游戏开始！')
        return
      }
      // 处理答案提交消息
      if (message.type === 'ANSWER_SUBMITTED') {
        const submittedUserId = message.data?.userId
        const submittedUsername = message.data?.username
        if (submittedUserId && !submittedPlayers.value.includes(submittedUserId)) {
          submittedPlayers.value.push(submittedUserId)
          console.log('玩家已提交答案:', submittedUsername, '当前已提交:', submittedPlayers.value.length)
          
          // 检查是否所有玩家都已提交
          if (currentRoom.value && submittedPlayers.value.length === currentRoom.value.currentPlayers) {
            console.log('所有玩家已提交，等待评分')
            ElMessage.success('所有玩家已提交答案，正在评分...')
          }
        }
        return
      }
      // 处理玩家离开消息
      if (message.type === 'PLAYER_LEFT') {
        const leftUserId = message.data?.userId
        const leftUsername = currentRoom.value?.players?.find(p => p.userId === leftUserId)?.username
        console.log('玩家离开房间:', leftUserId, leftUsername)
        console.log('接收到的消息数据:', JSON.stringify(message.data, null, 2))
        console.log('是否包含room字段:', !!message.data?.room)
        
        // 如果消息中包含更新后的房间信息，直接更新
        if (message.data?.room) {
          console.log('更新前的玩家列表:', currentRoom.value?.players)
          handleRoomUpdate(message.data.room)
          console.log('更新后的玩家列表:', currentRoom.value?.players)
          ElMessage.info(`玩家 ${leftUsername || leftUserId} 离开了房间`)
        } else {
          console.warn('PLAYER_LEFT消息中没有room字段，无法更新玩家列表')
        }
        return
      }
      
      // 处理轮次结束消息
      if (message.type === 'ROUND_FINISHED') {
        console.log('轮次结束，重置答题状态')
        // 重置答题状态
        myAnswer.value = ''
        hasSubmitted.value = false
        submittedPlayers.value = []
        return
      }
      
      // BattleMessage的data字段才是真正的房间数据
      if (message.data) {
        handleRoomUpdate(message.data)
      }
    }
  )

  // 订阅题目
  questionSubscription.value = stompClient.value.subscribe(
    `/topic/battle/${roomId}/question`,
    (msg) => {
      const message = JSON.parse(msg.body)
      console.log('收到题目消息:', message)
      if (message.data) {
        handleQuestion(message.data)
      }
    }
  )

  // 订阅评分结果
  scoreSubscription.value = stompClient.value.subscribe(
    `/topic/battle/${roomId}/score`,
    (msg) => {
      const message = JSON.parse(msg.body)
      console.log('收到评分消息:', message)
      if (message.data) {
        handleScore(message.data)
      }
    }
  )

  // 加载房间信息
  loadRoomInfo()
}

// 处理房间更新
const handleRoomUpdate = (data: any) => {
  console.log('房间更新:', data)
  
  // 只有包含完整房间信息时才更新currentRoom
  // 倒计时消息只包含remaining字段，不应覆盖整个房间信息
  if (data.players || data.status || data.roomId) {
    console.log('玩家列表:', data.players)
    console.log('玩家数量:', data.players?.length)
    currentRoom.value = data
    
    // 检查房间状态是否异常
    checkRoomStatus(data.status)
    
    // 根据状态切换视图
    if (data.status === 'PLAYING' && currentView.value === 'room') {
      currentView.value = 'playing'
    } else if (data.status === 'FINISHED') {
      currentView.value = 'result'
    }
  } else {
    // 倒计时更新 - 增加合理性验证，避免数值跳跃
    if (data.remaining !== undefined) {
      // 检查数值是否合理：必须是非负整数，且与当前值的差异不超过5秒
      const newRemaining = Number(data.remaining)
      if (Number.isInteger(newRemaining) && newRemaining >= 0) {
        // 只有当新值合理时才更新（初值为0或与当前值相差不超过5秒）
        if (countdown.value === 0 || Math.abs(newRemaining - countdown.value) <= 5) {
          countdown.value = newRemaining
          // 当倒计时为0时自动提交答案
          if (countdown.value <= 0 && !hasSubmitted.value) {
            submitAnswer()
          }
        } else {
          console.warn('忽略不合理的倒计时更新:', {old: countdown.value, new: newRemaining})
        }
      }
    }
  }
}

// 检查房间状态
const checkRoomStatus = (status: string) => {
  // 如果房间已结束，提示用户并可选择返回
  if (status === 'FINISHED' && currentView.value !== 'result') {
    ElMessage.warning('当前房间对战已结束')
    setTimeout(() => {
      router.push('/battle-room')
    }, 3000)
  }
  
  // 如果房间状态与当前视图不匹配，尝试同步
  if (status === 'WAITING' && currentView.value !== 'room') {
    ElMessage.info('房间已重置，返回等待状态')
    currentView.value = 'room'
    hasSubmitted.value = false
    myAnswer.value = ''
    submittedPlayers.value = []
  }
  
  // 如果在对战中但视图不对，同步到对战视图
  if (status === 'PLAYING' && currentView.value === 'room') {
    currentView.value = 'playing'
  }
}

// 处理题目
const handleQuestion = (data: any) => {
  console.log('收到题目:', data)
  
  // 停止生成中的打字机效果
  if (typingTimer) {
    clearInterval(typingTimer)
    typingTimer = null
  }
  isGenerating.value = false
  
  currentRound.value = data.round
  // 重置倒计时为题目时间限制
  countdown.value = data.timeLimit
  myAnswer.value = ''
  hasSubmitted.value = false
  submittedPlayers.value = []
  currentView.value = 'playing'
  
  // 使用打字机效果显示题目内容
  currentQuestion.value = ''
  const questionText = data.content || ''
  let charIndex = 0
  
  typingTimer = setInterval(() => {
    if (charIndex < questionText.length) {
      currentQuestion.value += questionText[charIndex]
      charIndex++
    } else {
      clearInterval(typingTimer)
      typingTimer = null
    }
  }, 50) // 每50ms显示一个字符，比生成提示快一些
  
  // 倒计时由后端WebSocket消息更新，不需要前端定时器
}

// 处理评分结果
const handleScore = (data: any) => {
  console.log('收到评分结果:', data)
  scoreResult.value = data
  currentView.value = 'scoring'
  
  // 检查是否游戏结束
  if (data.round >= currentRoom.value?.totalRounds) {
    isGameFinished.value = true
  } else {
    // 自动进入下一轮 - 先显示题目生成状态
    setTimeout(() => {
      currentView.value = 'generating'
      isGenerating.value = true
      startTypingEffect('正在生成下一轮题目，请稍候...')
    }, 5000)
  }
}

// 准备/取消准备
const toggleReady = () => {
  if (!stompClient.value || !currentRoom.value) return
  
  stompClient.value.publish({
    destination: '/app/battle/toggleReady',
    body: JSON.stringify({
      roomId: currentRoom.value.roomId,
      userId: userId.value.toString(),
      username: username.value
    })
  })
}

// 开始游戏
const startGame = () => {
  if (!stompClient.value || !currentRoom.value) return
  
  stompClient.value.publish({
    destination: '/app/battle/startGame',
    body: JSON.stringify({
      roomId: currentRoom.value.roomId,
      userId: userId.value.toString()
    })
  })
}

// 打字机效果
let typingTimer: any = null
let roomStatusCheckTimer: any = null
const startTypingEffect = (text: string) => {
  generatingText.value = ''
  let index = 0
  if (typingTimer) clearInterval(typingTimer)
  
  typingTimer = setInterval(() => {
    if (index < text.length) {
      generatingText.value += text[index]
      index++
    } else {
      clearInterval(typingTimer)
    }
  }, 100) // 每100ms显示一个字符
}

// 提交答案
const submitAnswer = async () => {
  if (hasSubmitted.value) return
  
  try {
    submitting.value = true
    
    // 检查WebSocket连接状态
    if (!stompClient.value || !stompClient.value.connected) {
      console.warn('WebSocket未连接,无法提交答案')
      ElMessage.error('连接已断开,请刷新页面重试')
      return
    }
    
    // 调试：打印当前房间信息
    console.log('当前房间信息:', currentRoom.value)
    console.log('房间ID:', currentRoom.value?.roomId)
    console.log('路由参数roomId:', route.params.roomId)
    
    // 通过WebSocket发送答案
    stompClient.value.publish({
      destination: '/app/battle/submitAnswer',
      headers: {
        userId: userId.value.toString(),
        username: username.value
      },
      body: JSON.stringify({
        roomId: currentRoom.value?.roomId,
        answer: myAnswer.value.trim() || '未作答'
      })
    })
    
    hasSubmitted.value = true
    submittedPlayers.value.push(userId.value)
    ElMessage.success('提交成功')
  } catch (error: any) {
    console.error('提交答案失败:', error)
    ElMessage.error('提交失败')
  } finally {
    submitting.value = false
  }
}

// 离开房间
const leaveRoom = () => {
  console.log('点击离开房间', { 
    hasStompClient: !!stompClient.value, 
    isConnected: stompClient.value?.connected,
    hasRoom: !!currentRoom.value,
    roomId: currentRoom.value?.roomId 
  })
  
  if (!stompClient.value) {
    console.error('WebSocket未连接')
    ElMessage.error('WebSocket未连接，无法离开房间')
    // 直接跳转回大厅
    router.push('/battle-room')
    return
  }
  
  if (!currentRoom.value) {
    console.error('房间信息不存在')
    ElMessage.error('房间信息不存在')
    router.push('/battle-room')
    return
  }
  
  try {
    console.log('发送离开房间消息', {
      roomId: currentRoom.value.roomId,
      userId: userId.value
    })
    
    // 检查WebSocket连接状态
    if (stompClient.value && stompClient.value.connected) {
      stompClient.value.publish({
        destination: '/app/battle/leaveRoom',
        body: JSON.stringify({
          roomId: currentRoom.value.roomId,
          userId: userId.value.toString()
        })
      })
    } else {
      console.warn('WebSocket未连接，跳过发送离开房间消息')
    }
    
    console.log('离开房间消息已发送')
    
    // 取消房间相关订阅
    if (roomSubscription.value) {
      roomSubscription.value.unsubscribe()
      roomSubscription.value = null
    }
    if (questionSubscription.value) {
      questionSubscription.value.unsubscribe()
      questionSubscription.value = null
    }
    if (scoreSubscription.value) {
      scoreSubscription.value.unsubscribe()
      scoreSubscription.value = null
    }
    
    // 跳转回大厅（不断开WebSocket，让BattleRoom页面复用）
    console.log('跳转回房间列表')
    router.push('/battle-room')
  } catch (error: any) {
    console.error('离开房间失败:', error)
    ElMessage.error('离开房间失败: ' + error.message)
  }
}

// 返回大厅
const backToLobby = () => {
  router.push('/battle-room')
}

// 获取排名
const getRank = (playerId: number, type: 'current' | 'total') => {
  if (!scoreResult.value) return '-'
  if (type === 'current') {
    const sorted = [...scoreResult.value.scores].sort((a, b) => b.score - a.score)
    return sorted.findIndex(s => s.userId === playerId) + 1
  }
  return scoreResult.value.ranking?.total?.find((r: any) => r.userId === playerId)?.rank || '-'
}

// 定期检查房间状态
const startRoomStatusCheck = () => {
  // 每10秒检查一次房间状态
  roomStatusCheckTimer = setInterval(async () => {
    const roomId = route.params.roomId as string
    if (!roomId || !currentRoom.value) return

    try {
      const response = await request.get(`/battle/room/${roomId}`)
      if (response.success && response.room) {
        // 检查房间状态是否发生异常变化
        const newStatus = response.room.status
        const currentStatus = currentRoom.value.status
        
        // 如果房间已结束，但当前不在scoring视图
        if (newStatus === 'FINISHED' && currentView.value !== 'scoring') {
          ElMessage.warning('房间已结束')
          setTimeout(() => router.push('/battle-room'), 2000)
          return
        }
        
        // 如果房间正在游戏中，但当前在room视图
        if (newStatus === 'PLAYING' && currentView.value === 'room') {
          ElMessage.info('游戏已开始，正在同步...')
          currentView.value = 'playing'
        }
      } else {
        // 房间不存在
        ElMessage.error('房间已不存在')
        setTimeout(() => router.push('/battle-room'), 2000)
      }
    } catch (error) {
      console.error('检查房间状态失败:', error)
    }
  }, 10000) // 10秒检查一次
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

  // 加载房间信息
  await loadRoomInfo()

  // 连接WebSocket
  connectWebSocket()
  
  // 启动房间状态监控
  startRoomStatusCheck()
})

onUnmounted(() => {
  // 清理打字机定时器
  if (typingTimer) {
    clearInterval(typingTimer)
    typingTimer = null
  }
  
  // 清理房间状态检查定时器
  if (roomStatusCheckTimer) {
    clearInterval(roomStatusCheckTimer)
    roomStatusCheckTimer = null
  }
  
  // 取消订阅
  roomSubscription.value?.unsubscribe()
  questionSubscription.value?.unsubscribe()
  scoreSubscription.value?.unsubscribe()
  
  // 断开连接
  if (stompClient.value?.connected) {
    stompClient.value.deactivate()
  }
})
</script>

<style scoped>
.room-detail-container {
  min-height: 100vh;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

/* 房间视图样式 */
.room-view {
  max-width: 1200px;
  margin: 0 auto;
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

/* 题目生成视图样式 */
.generating-view {
  max-width: 800px;
  margin: 100px auto;
  background: white;
  border-radius: 12px;
  padding: 60px 40px;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);
  text-align: center;
}

.generating-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 30px;
}

.loading-spinner {
  width: 60px;
  height: 60px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.generating-text {
  font-size: 20px;
  color: #333;
  font-weight: 500;
  min-height: 30px;
  letter-spacing: 1px;
}

.room-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid #f0f0f0;
}

.room-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.room-title h2 {
  margin: 0;
  font-size: 24px;
  color: #333;
}

.room-content {
  display: grid;
  gap: 24px;
}

.players-section h3 {
  margin-bottom: 16px;
  color: #333;
}

.player-list {
  display: grid;
  gap: 12px;
}

.player-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 2px solid transparent;
  transition: all 0.3s;
}

.player-item.is-ready {
  border-color: #67c23a;
  background: #f0f9ff;
}

.player-item.is-host {
  background: #fff7e6;
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
  padding: 2px 8px;
  background: #ffa940;
  color: white;
  border-radius: 4px;
  font-size: 12px;
}

.actions-section {
  display: flex;
  gap: 12px;
  justify-content: center;
  padding-top: 16px;
}

/* 游戏视图样式 */
.playing-view {
  max-width: 1000px;
  margin: 0 auto;
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.game-header {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid #f0f0f0;
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
  color: #409eff;
}

.countdown.warning {
  color: #f56c6c;
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.game-content {
  display: grid;
  gap: 24px;
}

.question-section h3 {
  margin-bottom: 12px;
  color: #333;
}

.question-content {
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  font-size: 16px;
  line-height: 1.6;
  color: #333;
  white-space: pre-wrap;
}

.answer-section {
  display: grid;
  gap: 12px;
}

.submit-status {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

/* 评分视图样式 */
.scoring-view {
  max-width: 1000px;
  margin: 0 auto;
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.scoring-header {
  text-align: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid #f0f0f0;
}

.scoring-header h2 {
  margin: 0;
  color: #333;
}

.scoring-content {
  display: grid;
  gap: 32px;
  margin-bottom: 24px;
}

.round-scores h3,
.total-ranking h3 {
  margin-bottom: 16px;
  color: #333;
}

.score-list,
.ranking-list {
  display: grid;
  gap: 12px;
}

.score-item {
  display: grid;
  grid-template-columns: 200px 1fr auto;
  gap: 16px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  align-items: start;
}

.score-player {
  display: flex;
  align-items: center;
  gap: 12px;
}

.score-player .rank {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: #409eff;
  color: white;
  border-radius: 50%;
  font-weight: bold;
}

.score-player .name {
  font-weight: 500;
  color: #333;
}

.score-details {
  display: grid;
  gap: 8px;
}

.score-details .answer {
  font-size: 14px;
  color: #666;
}

.score-details .feedback {
  font-size: 14px;
  color: #909399;
  font-style: italic;
}

.score-value {
  font-size: 24px;
  font-weight: bold;
  color: #67c23a;
}

.ranking-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.rank-number {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: #409eff;
  color: white;
  border-radius: 50%;
  font-weight: bold;
}

.rank-name {
  flex: 1;
  font-weight: 500;
  color: #333;
}

.rank-score {
  font-size: 18px;
  font-weight: bold;
  color: #67c23a;
}

.scoring-actions {
  text-align: center;
  padding-top: 16px;
}

.waiting-text {
  font-size: 16px;
  color: #909399;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .room-detail-container {
    padding: 12px;
  }

  .score-item {
    grid-template-columns: 1fr;
  }

  .score-value {
    text-align: right;
  }
}
</style>