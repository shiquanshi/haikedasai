import request from './request'

export const questionBankApi = {
  // AI生成题库
  generateAIBank(params: { topic: string; scenario?: string; cardCount: number; difficulty: string; language: string }) {
    return request({
      url: '/api/question-bank/generate',
      method: 'post',
      data: params,
      timeout: 120000 // AI生成需要更长时间,设置为120秒
    })
  },

  // AI流式生成题库
  generateAIBankStream(
    params: { topic: string; scenario?: string; cardCount: number; difficulty: string; language: string; withImages?: boolean },
    onMessage: (content: string) => void,
    onError?: (error: string) => void,
    onComplete?: () => void,
    onThinking?: (thinkingContent: string) => void
  ) {
    const token = localStorage.getItem('token')?.trim()
    // 使用相对路径,自动适配当前域名和协议
    const url = `/api/question-bank/generate-stream?` +
      `topic=${encodeURIComponent(params.topic)}&` +
      `scenario=${encodeURIComponent(params.scenario || '')}&` +
      `cardCount=${params.cardCount}&` +
      `difficulty=${encodeURIComponent(params.difficulty)}&` +
      `language=${params.language}&` +
      `withImages=${params.withImages || false}&` +
      `token=${token}`
    
    console.log('🔗 正在连接SSE:', url)
    const eventSource = new EventSource(url)

    eventSource.onopen = () => {
      console.log('✅ SSE连接已建立')
    }

    eventSource.addEventListener('message', (event) => {
      const content = event.data
      const timestamp = new Date()
      console.log(`[SSE接收] 时间=${timestamp}, 内容长度=${content.length}, 内容前50字=${content.substring(0, 50)}`)
      console.log('[SSE接收] 完整内容:', content)
      if (content && content !== '[DONE]') {
        onMessage(content)
        console.log('[SSE接收] 已调用onMessage回调')
      }
    })

    eventSource.addEventListener('thinking', (event) => {
      const thinkingContent = event.data
      console.log('🧠 收到thinking事件，思考过程长度:', thinkingContent.length)
      if (thinkingContent && onThinking) {
        onThinking(thinkingContent)
        console.log('[SSE接收] 已调用onThinking回调')
      }
    })

    eventSource.addEventListener('image_single', (event) => {
      console.log('🖼️ 收到image_single事件，单张卡片图片数据')
      try {
        const cardData = JSON.parse(event.data)
        console.log('单张卡片图片数据:', cardData)
        // 通过特殊标记传递单张卡片图片数据
        onMessage(JSON.stringify({ type: 'image_single', data: cardData }))
      } catch (e) {
        console.error('解析单张卡片图片数据失败:', e)
      }
    })

    eventSource.addEventListener('images', (event) => {
      console.log('📸 收到images事件，包含图片描述数据')
      try {
        const imagesData = JSON.parse(event.data)
        console.log('图片描述数据:', imagesData)
        // 通过特殊标记传递图片数据
        onMessage(JSON.stringify({ type: 'images', data: imagesData }))
      } catch (e) {
        console.error('解析图片数据失败:', e)
      }
    })

    eventSource.addEventListener('saved', (event) => {
      console.log('💾 收到saved事件，卡片已保存到数据库')
      try {
        const savedCards = JSON.parse(event.data)
        console.log('保存的卡片数据:', savedCards)
        // 通过特殊标记传递保存后的卡片数据
        onMessage(JSON.stringify({ type: 'saved', data: savedCards }))
      } catch (e) {
        console.error('解析保存数据失败:', e)
      }
    })

    eventSource.addEventListener('done', () => {
      console.log('✅ 收到done事件，关闭连接')
      eventSource.close()
      onComplete?.()
    })

    eventSource.addEventListener('error', (event) => {
      console.log('❌ 收到error事件:', event)
      eventSource.close()
      const errorMsg = (event as any).data || '流式生成失败'
      onError?.(errorMsg)
    })

    eventSource.onerror = (error) => {
      console.log('❌ SSE连接错误:', error)
      console.log('连接状态:', eventSource.readyState)
      eventSource.close()
      onError?.('连接失败')
    }

    return eventSource
  },

  // AI批量生成题库(非流式,适用于外语内容)
  generateAIBankBatch(params: { topic: string; scenario?: string; cardCount: number; difficulty: string; language: string; withImages?: boolean }) {
    const token = localStorage.getItem('token')?.trim()
    // 使用相对路径,自动适配当前域名和协议
    const url = `/api/question-bank/generate-batch?` +
      `topic=${encodeURIComponent(params.topic)}&` +
      `scenario=${encodeURIComponent(params.scenario || '')}&` +
      `cardCount=${params.cardCount}&` +
      `difficulty=${encodeURIComponent(params.difficulty)}&` +
      `language=${params.language}&` +
      `withImages=${params.withImages || false}&` +
      `token=${token}`
    
    console.log('🔗 正在调用批量生成接口:', url)
    return request({
      url: `/api/question-bank/generate-batch`,
      method: 'get',
      params: {
        topic: params.topic,
        scenario: params.scenario,
        cardCount: params.cardCount,
        difficulty: params.difficulty,
        language: params.language,
        withImages: params.withImages || false
      },
      timeout: 120000
    })
  },

  // 创建自定义题库
  createCustomBank(params: { name: string; description?: string; topic: string; difficulty?: string; language?: string }) {
    return request({
      url: '/api/question-bank/create',
      method: 'post',
      data: params
    })
  },

  // 获取系统推荐题库列表（支持分页）
  getSystemBanks(topic: string, page: number = 1, pageSize: number = 10) {
    return request({
      url: '/api/question-bank/system',
      method: 'get',
      params: { topic, page, pageSize }
    })
  },

  // 获取用户自定义题库列表（支持分页）
  getUserCustomBanks(page: number = 1, pageSize: number = 10) {
    return request({
      url: '/api/question-bank/custom',
      method: 'get',
      params: { page, pageSize }
    })
  },

  // 获取指定题库的卡片
  getBankCards(bankId: number) {
    return request({
      url: `/api/question-bank/${bankId}/cards`,
      method: 'get'
    })
  },

  // 根据ID获取题库详情
  getBankById(bankId: number) {
    return request({
      url: `/api/question-bank/${bankId}`,
      method: 'get'
    })
  },

  // 上传自定义文档生成题库
  uploadCustomBank(formData: FormData) {
    return request({
      url: '/api/question-bank/upload',
      method: 'post',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  // 文本转语音
  textToSpeech(text: string) {
    return request({
      url: '/api/tts/convert',
      method: 'post',
      data: { text }
    })
  },

  // 高级搜索题库(分页)
  searchBanks(params: {
    page?: number
    pageSize?: number
    sortBy?: string
    sortOrder?: string
    topic?: string
    type?: string
    difficulty?: string
    tags?: string
    userId?: number
    minCards?: number
    maxCards?: number
  }) {
    return request({
      url: '/api/question-bank/search',
      method: 'post',
      data: params
    })
  },

  // 增加题库浏览次数
  incrementViewCount(bankId: number) {
    return request({
      url: `/api/question-bank/${bankId}/view`,
      method: 'post'
    })
  },

  // 收藏题库
  addFavorite(bankId: number, userId: number) {
    return request({
      url: `/api/question-bank/${bankId}/favorite`,
      method: 'post',
      params: { userId }
    })
  },

  // 取消收藏
  removeFavorite(bankId: number, userId: number) {
    return request({
      url: `/api/question-bank/${bankId}/favorite`,
      method: 'delete',
      params: { userId }
    })
  },

  // 检查是否已收藏
  checkFavorite(bankId: number, userId: number) {
    return request({
      url: `/api/question-bank/${bankId}/favorite/check`,
      method: 'get',
      params: { userId }
    })
  },

  // 获取用户收藏的题库ID列表
  getUserFavorites(userId: number) {
    return request({
      url: '/api/question-bank/favorites',
      method: 'get',
      params: { userId }
    })
  },

  // 批量添加卡片到题库
  addCardsToBank(params: { targetBankId: number; cardIds: number[] }) {
    return request({
      url: '/api/question-bank/add-cards',
      method: 'post',
      data: params
    })
  },

  // 批量添加卡片内容到题库（用于AI生成的临时卡片）
  addCardContentsToBank(params: { 
    targetBankId: number; 
    cardContents: Array<{ question: string; answer: string; questionImage?: string; answerImage?: string }> 
  }) {
    return request({
      url: '/api/question-bank/add-card-contents',
      method: 'post',
      data: params
    })
  },

  // 从Excel导入题库
  importBankFromExcel(formData: FormData) {
    return request({
      url: '/api/question-bank/import',
      method: 'post',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  // 更新题库信息
  updateBank(params: { id: number; name: string; description: string; difficulty: string; language: string; tags?: string }) {
    return request({
      url: `/api/question-bank/${params.id}`,
      method: 'put',
      data: params
    })
  },

  // 删除题库
  deleteBank(id: number) {
    return request({
      url: `/api/question-bank/${id}`,
      method: 'delete'
    })
  },

  // 删除单个题库卡片
  deleteCard(cardId: number) {
    return request({
      url: `/api/question-bank/cards/${cardId}`,
      method: 'delete'
    })
  },

  // 新增卡片到题库
  addCard(bankId: number, params: { question: string; answer: string; questionImage?: string; answerImage?: string }) {
    return request({
      url: `/api/question-bank/${bankId}/card`,
      method: 'post',
      data: params
    })
  },

  // 更新卡片
  updateCard(cardId: number, params: { question: string; answer: string; questionImage?: string; answerImage?: string }) {
    return request({
      url: `/api/question-bank/cards/${cardId}`,
      method: 'put',
      data: params
    })
  },

  // 生成题库分享码
  generateShareCode(bankId: number, expireHours?: number) {
    return request({
      url: `/api/question-bank/${bankId}/share`,
      method: 'post',
      params: { expireHours }
    })
  },

  // 取消题库分享
  cancelShare(bankId: number) {
    return request({
      url: `/api/question-bank/${bankId}/share`,
      method: 'delete'
    })
  },

  // 通过分享码获取题库
  getByShareCode(shareCode: string) {
    return request({
      url: `/api/question-bank/shared/${shareCode}`,
      method: 'get'
    })
  },

  // 获取用户的分享记录
  getSharedRecords() {
    return request({
      url: '/api/question-bank/shared-records',
      method: 'get'
    })
  }
}