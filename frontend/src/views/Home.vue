<template>
  <div class="home-container">
    <!-- 主要内容区 -->
    <div class="main-content">
      <!-- 左侧：创建闪卡表单 -->
      <div class="content-section left-section" v-if="!showCards">
        <div class="create-card-form">
        
        <!-- 主题输入 -->
        <el-input
          v-model="topic"
          placeholder="学习主题"
          size="large"
          clearable
          class="form-item"
          :prefix-icon="Search"
        />
        
        <!-- 场景输入 -->
        <el-input
          v-model="scenario"
          placeholder="学习场景（可选）"
          size="large"
          clearable
          class="form-item"
          :prefix-icon="VideoPlay"
        />
        
        <!-- 其他选项 -->
        <div class="form-options">
          <!-- 闪卡数量 -->
          <el-select 
            v-model="cardCount"
            placeholder="闪卡数量"
            size="large"
            class="form-select"
          >
            <el-option label="1张" value="1"></el-option>
            <el-option label="2张" value="2"></el-option>
            <el-option label="5张" value="5"></el-option>
            <el-option label="10张" value="10"></el-option>
            <el-option label="20张" value="20"></el-option>
          </el-select>
          
          <!-- 难度级别 -->
          <el-select 
            v-model="difficulty"
            placeholder="难度级别"
            size="large"
            class="form-select"
          >
            <el-option label="初级" value="easy"></el-option>
            <el-option label="中级" value="medium"></el-option>
            <el-option label="高级" value="hard"></el-option>
          </el-select>
          
          <!-- 语言选择 -->
          <el-select 
            v-model="language"
            placeholder="语言"
            size="large"
            class="form-select"
          >
            <el-option label="中文" value="中文"></el-option>
            <el-option label="英文" value="英文"></el-option>
            <el-option label="日语" value="日语"></el-option>
            <el-option label="韩语" value="韩语"></el-option>
            <el-option label="法语" value="法语"></el-option>
            <el-option label="德语" value="德语"></el-option>
            <el-option label="西班牙语" value="西班牙语"></el-option>
            <el-option label="俄语" value="俄语"></el-option>
          </el-select>
          
          <!-- 配图选项 -->
          <el-checkbox 
            v-model="withImages"
            size="large"
            class="form-checkbox"
          >
            生成配图描述
          </el-checkbox>
        </div>
        
        <!-- 生成闪卡按钮 -->
          <el-button 
            type="primary" 
            size="large" 
            @click="generateCards"
            class="generate-button"
            :disabled="!topic || isGenerating"
            :loading="isGenerating"
          >
            <el-icon v-if="!isGenerating"><Star /></el-icon> 
            {{ isGenerating ? '生成中...' : '生成闪卡' }}
          </el-button>
        </div>
      </div>

      <!-- 右侧:题库功能 -->
      <div class="content-section right-section" v-if="!showCards">
        <div class="question-bank">
          <div class="bank-header">
            <h2>题库</h2>
            <el-input
              v-model="bankSearchText"
              placeholder="搜索题库"
              size="small"
              clearable
              class="bank-search"
              :prefix-icon="Search"
            />
          </div>
          
          <div class="bank-list">
            <el-divider content-position="left">推荐题库</el-divider>
            <el-loading v-loading="isLoadingBanks" element-loading-text="加载中...">
              <el-card
                v-for="bank in systemBanks"
                :key="bank.id"
                class="bank-card"
                @click="loadBankCards(bank.id)"
              >
                <template #header>
                  <div class="card-header">
                    <span>{{ bank.name }}</span>
                    <div class="card-header-actions">
                      <el-tag size="small">{{ bank.cardCount }}张卡片</el-tag>
                      <el-button
                        type="warning"
                        size="small"
                        :icon="Edit"
                        @click.stop="showEditBankDialogFunc(bank)"
                        circle
                        title="编辑题库"
                      />
                      <el-button
                        type="primary"
                        size="small"
                        :icon="Download"
                        @click.stop="exportBank(bank.id, bank.name)"
                        circle
                        title="导出Excel"
                      />
                    </div>
                  </div>
                </template>
                <div class="bank-description">
                  {{ bank.description }}
                </div>
                <div class="bank-meta">
                  <span class="bank-difficulty">{{ bank.difficulty }}</span>
                  <span class="bank-language">{{ bank.language }}</span>
                </div>
              </el-card>
              
              <div v-if="!isLoadingBanks && systemBanks.length === 0" class="no-data">
                暂无推荐题库
              </div>
              
              <!-- 系统题库分页器 -->
              <el-pagination
                v-if="systemBanks.length > 0"
                v-model:current-page="systemPage"
                v-model:page-size="systemPageSize"
                :page-sizes="[5, 10, 20, 50]"
                :total="systemTotal"
                layout="total, sizes, prev, pager, next, jumper"
                @size-change="loadSystemBanks"
                @current-change="loadSystemBanks"
                style="margin-top: 20px; justify-content: center"
              />
            </el-loading>

            <el-divider content-position="left">我的题库</el-divider>
            <el-loading v-loading="isLoadingCustomBanks" element-loading-text="加载中...">
              <el-card
                v-for="bank in customBanks"
                :key="bank.id"
                class="bank-card"
                @click="loadBankCards(bank.id)"
              >
                <template #header>
                  <div class="card-header">
                    <span>{{ bank.name }}</span>
                    <div class="card-header-actions">
                      <el-tag size="small">{{ bank.cardCount }}张卡片</el-tag>
                      <el-button
                        type="warning"
                        size="small"
                        :icon="Edit"
                        @click.stop="showEditBankDialogFunc(bank)"
                        circle
                        title="编辑题库"
                      />
                      <el-button
                        type="primary"
                        size="small"
                        :icon="Download"
                        @click.stop="exportBank(bank.id, bank.name)"
                        circle
                        title="导出Excel"
                      />
                      <el-button
                        type="success"
                        size="small"
                        :icon="Star"
                        @click.stop="handleShareBank(bank.id)"
                        circle
                        title="分享题库"
                      />
                      <el-button
                        type="danger"
                        size="small"
                        :icon="Delete"
                        @click.stop="handleDeleteBank(bank.id)"
                        circle
                        title="删除题库"
                      />
                    </div>
                  </div>
                </template>
                <div class="bank-description">
                  {{ bank.description }}
                </div>
                <div class="bank-meta">
                  <span class="bank-difficulty">{{ bank.difficulty }}</span>
                  <span class="bank-language">{{ bank.language }}</span>
                </div>
              </el-card>
              
              <div v-if="!isLoadingCustomBanks && customBanks.length === 0" class="no-data">
                暂无自定义题库
              </div>
              
              <!-- 自定义题库分页器 -->
              <el-pagination
                v-if="customBanks.length > 0"
                v-model:current-page="customPage"
                v-model:page-size="customPageSize"
                :page-sizes="[5, 10, 20, 50]"
                :total="customTotal"
                layout="total, sizes, prev, pager, next, jumper"
                @size-change="loadCustomBanks"
                @current-change="loadCustomBanks"
                style="margin-top: 20px; justify-content: center"
              />
            </el-loading>

            <!-- 我的分享 -->
            <el-divider v-if="userStore.isLoggedIn" content-position="left">我的分享</el-divider>
            <el-loading v-if="userStore.isLoggedIn" v-loading="isLoadingShared" element-loading-text="加载中...">
              <el-card
                v-for="bank in sharedBanks"
                :key="bank.id"
                class="bank-card"
                @click="loadBankCards(bank.id)"
              >
                <template #header>
                  <div class="card-header">
                    <span>{{ bank.name }}</span>
                    <div class="card-header-actions">
                      <el-tag size="small">{{ bank.cardCount }}张卡片</el-tag>
                      <el-tag size="small" type="success">分享码: {{ bank.shareCode }}</el-tag>
                      <el-button
                        type="success"
                        size="small"
                        :icon="Star"
                        @click.stop="bank.shareCode && copyShareCodeDirect(bank.shareCode)"
                        circle
                        title="复制分享码"
                      />
                    </div>
                  </div>
                </template>
                <div class="bank-description">
                  {{ bank.description }}
                </div>
                <div class="bank-meta">
                  <span class="bank-difficulty">{{ bank.difficulty }}</span>
                  <span class="bank-language">{{ bank.language }}</span>
                </div>
              </el-card>
              
              <div v-if="userStore.isLoggedIn && !isLoadingShared && sharedBanks.length === 0" class="no-data">
                暂无分享记录
              </div>
            </el-loading>

          <!-- 历史生成记录 -->
          <el-divider v-if="userStore.isLoggedIn" content-position="left">历史生成记录</el-divider>
          <el-loading v-if="userStore.isLoggedIn" v-loading="isLoadingHistory" element-loading-text="加载中...">
            <el-card
                v-for="record in historyRecords"
                :key="record.id"
                class="bank-card"
                @click="loadBankCards(record.id)"
              >
                <template #header>
                  <div class="card-header">
                    <span>{{ record.name }}</span>
                    <div class="card-header-actions">
                      <el-tag size="small">{{ record.cardCount }}张卡片</el-tag>
                      <el-tag size="small" type="info">{{ formatDate(new Date(record.createTime), 'YYYY-MM-DD HH:mm') }}</el-tag>
                      <el-button
                        type="primary"
                        size="small"
                        :icon="Download"
                        @click.stop="exportBank(record.id, record.name)"
                        circle
                        title="导出Excel"
                      />
                      <el-button
                        type="success"
                        size="small"
                        :icon="Star"
                        @click.stop="handleShareBank(record.id)"
                        circle
                        title="分享题库"
                      />
                      <el-button
                        type="danger"
                        size="small"
                        :icon="Delete"
                        @click.stop="handleDeleteBank(record.id)"
                        circle
                        title="删除题库"
                      />
                    </div>
                  </div>
                </template>
              <div class="bank-meta">
                <span class="bank-difficulty">{{ record.difficulty }}</span>
                <span class="bank-language">{{ record.language }}</span>
              </div>
            </el-card>
            
            <!-- 分页组件 -->
            <div v-if="userStore.isLoggedIn && historyRecords.length > 0" class="pagination-container">
              <el-pagination
                background
                layout="prev, pager, next, total"
                :total="historyTotal"
                :page-size="historyPageSize"
                :current-page="historyPage"
                @current-change="handleHistoryPageChange"
                :disabled="isLoadingHistory"
              />
            </div>
            
            <div v-if="userStore.isLoggedIn && !isLoadingHistory && historyRecords.length === 0" class="no-data">
              暂无历史生成记录
            </div>
          </el-loading>
            
            <!-- 创建题库按钮 -->
            <div class="create-bank-button-container">
              <el-button 
                type="primary" 
                size="large"
                @click="showCreateBankDialog = true"
                class="create-bank-button"
                :icon="Plus"
                round
              >
                创建新题库
              </el-button>
              <el-button 
                type="success" 
                size="large"
                @click="handleShowImportDialog"
                class="import-bank-button"
                :icon="Upload"
                round
              >
                导入Excel题库
              </el-button>
              <el-button 
                type="warning" 
                size="large"
                @click="showAccessDialog = true"
                class="access-bank-button"
                :icon="Search"
                round
                style="margin-left: 10px"
              >
                访问分享题库
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 卡片展示区（覆盖整个宽度） -->
      <div class="cards-section full-width" v-if="showCards">
        <div class="cards-header">
          <div class="header-left">
            <h2>{{ topic }} - 学习卡片</h2>
            <div class="card-progress" v-if="!isGenerating">
              <span>进度：{{ currentCardIndex + 1 }} / {{ cards.length }}</span>
              <el-button 
                @click="toggleSelectionMode" 
                :type="isSelectionMode ? 'success' : 'default'"
                size="small"
                style="margin-left: 20px"
              >
                {{ isSelectionMode ? '完成选题' : '选题模式' }}
              </el-button>
              <el-button 
                @click="handleAddNewCard"
                type="success"
                size="small"
                style="margin-left: 10px"
                title="新增卡片到当前题库"
              >
                新增卡片
              </el-button>
              <el-button 
                v-if="isSelectionMode && selectedCardIds.length > 0"
                @click="showAddToBankDialog"
                type="primary"
                size="small"
                style="margin-left: 10px"
              >
                添加到题库 ({{ selectedCardIds.length }})
              </el-button>
              <el-button 
                v-if="isSelectionMode && selectedCardIds.length > 0"
                @click="handleDeleteSelectedCards"
                type="danger"
                size="small"
                style="margin-left: 10px"
                :disabled="selectedCardIds.length === cards.length"
                title="删除选中的卡片"
              >
                删除选中 ({{ selectedCardIds.length }})
              </el-button>
            </div>
            <div class="card-progress generating" v-else>
              <el-icon class="is-loading" style="margin-right: 8px"><Loading /></el-icon>
              <span>正在生成中... 已生成 {{ cards.length }} 张卡片</span>
            </div>
          </div>
          <el-button @click="backToHome" type="primary" size="large">返回</el-button>
        </div>

        <div class="card-container">
          <!-- 加载状态 -->
          <div v-if="isGenerating && cards.length === 0" class="loading-container">
            <el-icon class="loading-icon" :size="60"><Loading /></el-icon>
            <div class="loading-text">{{ displayedLoadingText }}<span v-if="displayedLoadingText && displayedLoadingText.length < loadingText.length" class="typing-cursor">|</span></div>
            <!-- 思考过程展示 -->
            <div class="thinking-process">
              <div class="thinking-header">
                <el-icon class="thinking-icon"><ChatDotRound /></el-icon>
                <span>AI思考过程</span>
              </div>
              <div class="thinking-content">
                {{ displayedThinking || '正在思考中...' }}
                <span v-if="isTyping" class="typing-cursor">|</span>
              </div>
            </div>
          </div>
          
          <!-- 田字格卡片展示 -->
          <div class="grid-container" v-else-if="cards.length > 0">
            <div v-for="(card, index) in cards" :key="index" class="grid-item">
              <div 
                v-if="isSelectionMode"
                class="card-checkbox"
                @click.stop="toggleCardSelection(index)"
              >
                <div class="checkbox-box" :class="{ checked: selectedCardIds.includes(index) }">
                  <span v-if="selectedCardIds.includes(index)" class="checkmark">✓</span>
                </div>
              </div>
              <div class="flip-card" :class="{ flipped: flippedCards[index] }" @click="flipCard(index)">
                <div class="flip-card-inner">
                  <div class="flip-card-front">
                    <div class="card-content">
                      <div class="card-label">问题</div>
                      <div class="card-text">{{ card.question }}</div>
                      <div v-if="card.questionImage" class="card-image">
                        <el-image :src="card.questionImage" fit="cover" class="card-img">
                          <template #error>
                            <div class="image-slot">加载失败</div>
                          </template>
                        </el-image>
                      </div>
                      <div class="card-actions">
                        <el-button 
                          @click.stop="playQuestion(index)" 
                          :loading="isPlayingQuestion && playingQuestionIndex === index"
                          circle
                          size="small"
                          class="play-button"
                          title="播放问题"
                        >
                          <template #icon v-if="!(isPlayingQuestion && playingQuestionIndex === index)">
                            <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                              <path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/>
                            </svg>
                          </template>
                        </el-button>
                      </div>
                      <div class="flip-hint">点击查看答案</div>
                    </div>
                  </div>
                  <div class="flip-card-back">
                    <div class="card-content">
                      <div class="card-label">答案</div>
                      <div class="card-text">{{ card.answer }}</div>
                      <div v-if="card.answerImage" class="card-image">
                        <el-image :src="card.answerImage" fit="cover" class="card-img">
                          <template #error>
                            <div class="image-slot">加载失败</div>
                          </template>
                        </el-image>
                      </div>
                      <div class="card-actions">
                        <el-button 
                          @click.stop="playQuestion(index)" 
                          :loading="isPlayingQuestion && playingQuestionIndex === index"
                          circle
                          size="small"
                          class="play-button"
                          title="播放答案"
                        >
                          <template #icon v-if="!(isPlayingQuestion && playingQuestionIndex === index)">
                            <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                              <path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/>
                            </svg>
                          </template>
                        </el-button>
                      </div>
                      <div class="flip-hint">点击返回问题</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="card-controls">
          <el-button 
            @click="backToHome" 
            type="primary" 
            size="large"
          >
            返回首页
          </el-button>
        </div>
      </div>
    </div>

    <!-- 选择目标题库对话框 -->
    <el-dialog
      v-model="showBankDialog"
      title="选择目标题库"
      width="750px"
    >
      <div class="bank-selection-dialog">
        <div class="create-bank-button-wrapper">
          <el-button type="success" @click="showCreateBankDialog = true" size="default">
            <el-icon><Plus /></el-icon>
            创建新题库
          </el-button>
        </div>
        <el-input
          v-model="bankSearchText"
          placeholder="搜索题库名称或主题..."
          clearable
          class="bank-search-input"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <div v-if="isLoadingDialogBanks" class="bank-loading">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>加载题库中...</span>
        </div>
        <div v-else-if="filteredDialogSystemBanks.length === 0 && filteredDialogCustomBanks.length === 0" class="bank-empty">
          <el-icon><Search /></el-icon>
          <span>{{ bankSearchText ? '未找到匹配的题库' : '暂无题库，请先创建' }}</span>
        </div>
        <div v-else class="bank-list">
          <!-- 系统推荐题库 -->
          <div v-if="filteredDialogSystemBanks.length > 0" class="bank-category">
            <div class="bank-category-title">系统推荐题库</div>
            <div
              v-for="bank in filteredDialogSystemBanks"
              :key="'system-' + bank.id"
              class="bank-radio-item"
              :class="{ 'is-selected': selectedTargetBankId === bank.id }"
              @click="selectedTargetBankId = bank.id"
            >
              <div class="radio-button">
                <div class="radio-inner" :class="{ 'is-checked': selectedTargetBankId === bank.id }">
                  <div v-if="selectedTargetBankId === bank.id" class="radio-dot"></div>
                </div>
              </div>
              <div class="bank-info">
                <div class="bank-name">
                  <el-tag type="info" size="small" style="margin-right: 8px">系统</el-tag>
                  {{ bank.name }}
                </div>
                <div class="bank-meta">
                  <span>{{ bank.cardCount }} 张卡片</span>
                  <span v-if="bank.difficulty">{{ bank.difficulty }}</span>
                  <span v-if="bank.language">{{ bank.language }}</span>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 我的自定义题库 -->
          <div v-if="filteredDialogCustomBanks.length > 0" class="bank-category">
            <div class="bank-category-title">我的题库</div>
            <div
              v-for="bank in filteredDialogCustomBanks"
              :key="'custom-' + bank.id"
              class="bank-radio-item"
              :class="{ 'is-selected': selectedTargetBankId === bank.id }"
              @click="selectedTargetBankId = bank.id"
            >
              <div class="radio-button">
                <div class="radio-inner" :class="{ 'is-checked': selectedTargetBankId === bank.id }">
                  <div v-if="selectedTargetBankId === bank.id" class="radio-dot"></div>
                </div>
              </div>
              <div class="bank-info">
                <div class="bank-name">
                  <el-tag type="success" size="small" style="margin-right: 8px">自定义</el-tag>
                  {{ bank.name }}
                </div>
                <div class="bank-meta">
                  <span>{{ bank.cardCount }} 张卡片</span>
                  <span v-if="bank.difficulty">{{ bank.difficulty }}</span>
                  <span v-if="bank.language">{{ bank.language }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showBankDialog = false" size="large">取消</el-button>
          <el-button type="primary" @click="addSelectedCardsToBank" size="large">
            确认添加 ({{ selectedCardIds.length }} 张)
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 创建题库对话框 -->
    <el-dialog
      v-model="showCreateBankDialog"
      title="创建自定义题库"
      width="600px"
    >
      <el-form :model="createBankForm" label-width="100px" class="create-bank-form">
        <el-form-item label="题库名称" required>
          <el-input v-model="createBankForm.name" placeholder="请输入题库名称" />
        </el-form-item>
        <el-form-item label="主题" required>
          <el-input v-model="createBankForm.topic" placeholder="请输入主题" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="createBankForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入题库描述（可选）"
          />
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="createBankForm.difficulty" placeholder="请选择难度" style="width: 100%">
            <el-option label="简单" value="easy" />
            <el-option label="中等" value="medium" />
            <el-option label="困难" value="hard" />
          </el-select>
        </el-form-item>
        <el-form-item label="语言">
          <el-select v-model="createBankForm.language" placeholder="请选择语言" style="width: 100%">
            <el-option label="中文" value="中文" />
            <el-option label="英文" value="英文" />
            <el-option label="日语" value="日语" />
            <el-option label="韩语" value="韩语" />
            <el-option label="法语" value="法语" />
            <el-option label="德语" value="德语" />
            <el-option label="西班牙语" value="西班牙语" />
            <el-option label="俄语" value="俄语" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showCreateBankDialog = false" size="large">取消</el-button>
          <el-button type="primary" @click="handleCreateBank" size="large">创建</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 导入题库对话框 -->
    <el-dialog
      v-model="showImportDialog"
      title="从Excel导入题库"
      width="600px"
    >
      <el-form :model="importForm" label-width="100px" class="import-form">
        <el-form-item label="导入方式" required>
          <el-radio-group v-model="importForm.importMode">
            <el-radio label="new">创建新题库</el-radio>
            <el-radio label="existing">导入到已有题库</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <!-- 选择已有题库 -->
        <el-form-item v-if="importForm.importMode === 'existing'" label="选择题库" required>
          <el-select 
            v-model="importForm.targetBankId" 
            placeholder="请选择要导入的题库" 
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="bank in userBanks"
              :key="bank.id"
              :label="bank.name"
              :value="bank.id"
            >
              <span>{{ bank.name }}</span>
              <span style="float: right; color: var(--el-text-color-secondary); font-size: 13px">
                {{ bank.cardCount }} 张卡片
              </span>
            </el-option>
          </el-select>
        </el-form-item>
        
        <!-- 新建题库信息 -->
        <template v-if="importForm.importMode === 'new'">
          <el-form-item label="题库名称">
            <el-input v-model="importForm.bankName" placeholder="可选,不填则使用文件名" />
          </el-form-item>
          <el-form-item label="题库描述">
            <el-input v-model="importForm.description" type="textarea" :rows="3" placeholder="可选,请输入题库描述" />
          </el-form-item>
          <el-form-item label="难度">
            <el-select v-model="importForm.difficulty" placeholder="选择难度" style="width: 100%">
              <el-option label="简单" value="easy" />
              <el-option label="中等" value="medium" />
              <el-option label="困难" value="hard" />
            </el-select>
          </el-form-item>
          <el-form-item label="语言">
            <el-select v-model="importForm.language" placeholder="选择语言" style="width: 100%">
              <el-option label="中文" value="中文" />
              <el-option label="英文" value="英文" />
              <el-option label="日语" value="日语" />
              <el-option label="韩语" value="韩语" />
              <el-option label="法语" value="法语" />
              <el-option label="德语" value="德语" />
              <el-option label="西班牙语" value="西班牙语" />
            </el-select>
          </el-form-item>
        </template>
        <el-form-item label="选择文件" required>
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".xlsx"
            :on-change="handleFileChange"
            :file-list="fileList"
          >
            <el-button type="primary">选择Excel文件</el-button>
            <template #tip>
              <div class="el-upload__tip">
                仅支持.xlsx格式的Excel文件<br/>
                文件格式:第一列-问题,第二列-答案,第三列-图片URL(可选)
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showImportDialog = false" size="large">取消</el-button>
          <el-button type="primary" @click="handleImport" :loading="isImporting" size="large">导入</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 编辑题库对话框 -->
    <el-dialog
      v-model="showEditBankDialog"
      title="编辑题库"
      width="600px"
    >
      <el-form :model="editBankForm" label-width="100px" class="edit-bank-form">
        <el-form-item label="题库名称" required>
          <el-input v-model="editBankForm.name" placeholder="请输入题库名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="editBankForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入题库描述（可选）"
          />
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="editBankForm.difficulty" placeholder="请选择难度" style="width: 100%">
            <el-option label="简单" value="easy" />
            <el-option label="中等" value="medium" />
            <el-option label="困难" value="hard" />
          </el-select>
        </el-form-item>
        <el-form-item label="语言">
          <el-select v-model="editBankForm.language" placeholder="请选择语言" style="width: 100%">
            <el-option label="中文" value="中文" />
            <el-option label="英文" value="英文" />
            <el-option label="日语" value="日语" />
            <el-option label="韩语" value="韩语" />
            <el-option label="法语" value="法语" />
            <el-option label="德语" value="德语" />
            <el-option label="西班牙语" value="西班牙语" />
            <el-option label="俄语" value="俄语" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showEditBankDialog = false" size="large">取消</el-button>
          <el-button type="primary" @click="handleUpdateBank" size="large">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 新增卡片对话框 -->
    <el-dialog
      v-model="showAddCardDialog"
      title="新增卡片"
      width="600px"
    >
      <el-form :model="addCardForm" label-width="100px" class="add-card-form">
        <el-form-item label="问题" required>
          <el-input
            v-model="addCardForm.question"
            type="textarea"
            :rows="3"
            placeholder="请输入问题内容"
          />
        </el-form-item>
        <el-form-item label="答案" required>
          <el-input
            v-model="addCardForm.answer"
            type="textarea"
            :rows="3"
            placeholder="请输入答案内容"
          />
        </el-form-item>
        <el-form-item label="问题图片">
          <el-upload
            :auto-upload="false"
            :limit="1"
            accept="image/*"
            :on-change="handleQuestionImageChange"
            :file-list="questionImageFileList"
            list-type="picture-card"
          >
            <el-icon><Plus /></el-icon>
            <template #tip>
              <div class="el-upload__tip">
                支持jpg/png/gif格式,大小不超过2MB
              </div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="答案图片">
          <el-upload
            :auto-upload="false"
            :limit="1"
            accept="image/*"
            :on-change="handleAnswerImageChange"
            :file-list="answerImageFileList"
            list-type="picture-card"
          >
            <el-icon><Plus /></el-icon>
            <template #tip>
              <div class="el-upload__tip">
                支持jpg/png/gif格式,大小不超过2MB
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showAddCardDialog = false" size="large">取消</el-button>
          <el-button type="primary" @click="handleSubmitNewCard" size="large">创建</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 编辑卡片对话框 -->
    <el-dialog
      v-model="showEditCardDialog"
      title="编辑卡片"
      width="600px"
    >
      <el-form :model="editCardForm" label-width="100px" class="edit-card-form">
        <el-form-item label="问题" required>
          <el-input
            v-model="editCardForm.question"
            type="textarea"
            :rows="3"
            placeholder="请输入问题内容"
          />
        </el-form-item>
        <el-form-item label="答案" required>
          <el-input
            v-model="editCardForm.answer"
            type="textarea"
            :rows="3"
            placeholder="请输入答案内容"
          />
        </el-form-item>
        <el-form-item label="问题图片">
          <div v-if="editCardForm.questionImage" class="current-image-preview">
            <el-image :src="editCardForm.questionImage" fit="contain" style="max-width: 200px; max-height: 200px" />
            <el-button @click="editCardForm.questionImage = ''" type="danger" size="small" style="margin-top: 8px">删除图片</el-button>
          </div>
          <el-upload
            v-else
            :auto-upload="false"
            :limit="1"
            accept="image/*"
            :on-change="handleEditQuestionImageChange"
            :file-list="editQuestionImageFileList"
            list-type="picture-card"
          >
            <el-icon><Plus /></el-icon>
            <template #tip>
              <div class="el-upload__tip">
                支持jpg/png/gif格式,大小不超过2MB
              </div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="答案图片">
          <div v-if="editCardForm.answerImage" class="current-image-preview">
            <el-image :src="editCardForm.answerImage" fit="contain" style="max-width: 200px; max-height: 200px" />
            <el-button @click="editCardForm.answerImage = ''" type="danger" size="small" style="margin-top: 8px">删除图片</el-button>
          </div>
          <el-upload
            v-else
            :auto-upload="false"
            :limit="1"
            accept="image/*"
            :on-change="handleEditAnswerImageChange"
            :file-list="editAnswerImageFileList"
            list-type="picture-card"
          >
            <el-icon><Plus /></el-icon>
            <template #tip>
              <div class="el-upload__tip">
                支持jpg/png/gif格式,大小不超过2MB
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showEditCardDialog = false" size="large">取消</el-button>
          <el-button type="primary" @click="handleSubmitEditCard" size="large">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 分享题库对话框 -->
    <el-dialog
      v-model="showShareDialog"
      title="分享题库"
      width="500px"
    >
      <div v-if="!shareCode">
        <el-form label-width="120px">
          <el-form-item label="有效期">
            <el-select v-model="shareExpireHours" placeholder="不限制（永久有效）" style="width: 100%" size="large">
              <el-option label="24小时" :value="24" />
              <el-option label="3天" :value="72" />
              <el-option label="7天" :value="168" />
              <el-option label="30天" :value="720" />
              <el-option label="永久有效" :value="null" />
            </el-select>
          </el-form-item>
        </el-form>
        <div style="text-align: center; margin-top: 20px">
          <el-button type="primary" @click="confirmGenerateShare" size="large">生成分享码</el-button>
          <el-button @click="showShareDialog = false" size="large">取消</el-button>
        </div>
      </div>
      <div v-else class="share-content">
        <p style="margin-bottom: 15px">分享码已生成，请复制分享码给其他用户：</p>
        <el-input
          v-model="shareCode"
          readonly
          size="large"
        >
          <template #append>
            <el-button @click="copyShareCode" type="primary">复制</el-button>
          </template>
        </el-input>
        <p style="margin-top: 15px; color: #909399; font-size: 14px">
          其他用户可以通过此分享码访问您的题库
        </p>
        <p v-if="shareExpireHours" style="margin-top: 10px; color: #e6a23c; font-size: 14px">
          ⏰ 有效期：{{ shareExpireHours }}小时
        </p>
      </div>
    </el-dialog>

    <!-- 访问分享题库对话框 -->
    <el-dialog
      v-model="showAccessDialog"
      title="访问分享题库"
      width="500px"
    >
      <el-form label-width="100px">
        <el-form-item label="分享码">
          <el-input
            v-model="accessShareCode"
            placeholder="请输入6位分享码"
            maxlength="6"
            size="large"
            clearable
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showAccessDialog = false" size="large">取消</el-button>
          <el-button type="primary" @click="handleAccessSharedBank" size="large">访问</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Star, VideoPlay, Search, Loading, Plus, Download, Upload, Edit, Delete } from '@element-plus/icons-vue'
import { questionBankApi } from '../api/questionBank'
import { useUserStore } from '../stores/user'
import { formatDate } from '../utils/date'

const router = useRouter()
const userStore = useUserStore()

interface Card {
  id: number
  question: string
  answer: string
  questionImage?: string
  answerImage?: string
}

interface QuestionBank {
  id: number
  name: string
  description: string
  cardCount: number
  difficulty: string
  language: string
  createTime: string
  shareCode?: string
  type?: string
}

const topic = ref('')
const scenario = ref('') // 新增：场景输入
const cardCount = ref('10')
const difficulty = ref('medium')
const language = ref('中文')  // 默认中文，与el-select的选项值保持一致
const withImages = ref(false)
const showCards = ref(false)
const cards = ref<Card[]>([])
const currentCardIndex = ref(0)
const isFlipped = ref(false)
const flippedCards = ref<boolean[]>([])
const isPlayingQuestion = ref(false)
const playingQuestionIndex = ref<number | null>(null)
let audioElement: HTMLAudioElement | null = null

// 题库相关
const systemBanks = ref<QuestionBank[]>([])  // 系统推荐题库
const customBanks = ref<QuestionBank[]>([])  // 用户自定义题库
const isLoadingBanks = ref(false)  // 系统题库加载状态
const isLoadingCustomBanks = ref(false)  // 自定义题库加载状态
const bankSearchText = ref('')
const currentBankId = ref<number | null>(null) // 当前正在查看的题库ID

// 系统推荐题库分页
const systemPage = ref(1)
const systemPageSize = ref(10)
const systemTotal = ref(0)

// 用户自定义题库分页
const customPage = ref(1)
const customPageSize = ref(10)
const customTotal = ref(0)

// 流式生成相关
const isGenerating = ref(false)
const streamContent = ref('')
const thinkingProcess = ref('') // 思考过程
const displayedThinking = ref('') // 当前显示的思考过程
const isTyping = ref(false) // 是否正在打字
const loadingText = ref('') // 加载文本
const displayedLoadingText = ref('') // 当前显示的加载文本
let streamEventSource: EventSource | null = null
let typingTimer: any = null
let hideTimer: any = null
let loadingTextTimer: any = null

// 思考过程智能累积显示 - 修复文本拼接问题
const startTypingEffect = (newText: string) => {
  // 清除所有相关定时器
  if (typingTimer) clearInterval(typingTimer)
  if (hideTimer) clearTimeout(hideTimer)
  
  // 确保思考过程区域显示
  thinkingProcess.value = 'show'
  
  // 核心修复：实现正确的文本拼接逻辑
  if (newText && newText.trim()) {
    // 将新接收的文本追加到已有显示内容后面，而不是替换
    displayedThinking.value = displayedThinking.value + newText
    
    // 同时更新原始思考过程变量
    thinkingProcess.value = displayedThinking.value
  }
  
  isTyping.value = false
  
  // 确保变量状态正确
  typingTimer = null
}

// 加载文本打字机效果
const startLoadingTextTyping = (text: string) => {
  // 清除之前的定时器
  if (loadingTextTimer) clearInterval(loadingTextTimer)
  
  displayedLoadingText.value = ''
  
  let index = 0
  const speed = 100 // 每个字符显示间隔（毫秒）
  
  loadingTextTimer = setInterval(() => {
    if (index < text.length) {
      displayedLoadingText.value += text[index]
      index++
    } else {
      clearInterval(loadingTextTimer)
    }
  }, speed)
}

// 选题模式相关
const isSelectionMode = ref(false)
const selectedCardIds = ref<number[]>([])
const showBankDialog = ref(false)
const selectedTargetBankId = ref<number | null>(null)
// 对话框中显示的题库列表（独立于页面主列表）
const dialogSystemBanks = ref<QuestionBank[]>([])
const dialogCustomBanks = ref<QuestionBank[]>([])
const isLoadingDialogBanks = ref(false)

// 创建题库相关
const showCreateBankDialog = ref(false)
const createBankForm = ref({
  name: '',
  topic: '',
  description: '',
  difficulty: 'medium',
  language: '中文'
})

// 导入题库相关
const showImportDialog = ref(false)
const importForm = ref({
  importMode: 'new' as 'new' | 'existing', // 导入方式：new-创建新题库，existing-导入到已有题库
  targetBankId: null as number | null, // 目标题库ID（导入到已有题库时使用）
  bankName: '',
  description: '',
  difficulty: 'medium',
  language: '中文'
})
const fileList = ref<any[]>([])
const uploadRef = ref()
const isImporting = ref(false)
const userBanks = ref<QuestionBank[]>([]) // 用户的所有题库列表

// 编辑题库相关
const showEditBankDialog = ref(false)
const editBankForm = ref({
  id: null as number | null,
  name: '',
  description: '',
  difficulty: 'medium',
  language: '中文'
})

// 新增卡片相关
const showAddCardDialog = ref(false)
const addCardForm = ref({
  question: '',
  answer: '',
  questionImage: '',
  answerImage: ''
})

// 编辑卡片相关
const showEditCardDialog = ref(false)
const editCardForm = ref({
  id: null as number | null,
  question: '',
  answer: '',
  questionImage: '',
  answerImage: ''
})
const editQuestionImageFileList = ref<any[]>([])
const editAnswerImageFileList = ref<any[]>([])

// 历史生成记录相关
const historyRecords = ref<QuestionBank[]>([])
const isLoadingHistory = ref(false)
const historyPage = ref(1)
const historyPageSize = ref(10)
const historyTotal = ref(0)

// 分享记录相关
const sharedBanks = ref<QuestionBank[]>([])
const isLoadingShared = ref(false)

const questionImageFileList = ref<any[]>([])
const answerImageFileList = ref<any[]>([])

// 分享相关
const showShareDialog = ref(false)
const shareCode = ref('')
const showAccessDialog = ref(false)
const accessShareCode = ref('')
const shareExpireHours = ref<number | null>(null) // 分享有效期（小时）
const currentSharingBankId = ref<number | null>(null) // 当前正在分享的题库ID

const currentCard = computed(() => cards.value[currentCardIndex.value] || { question: '', answer: '' })
const _isCurrentCardSelected = computed(() => {
  const currentId = cards.value[currentCardIndex.value]?.id
  return selectedCardIds.value.includes(currentId)
})
const currentBank = computed(() => {
  if (!currentBankId.value) return null
  // 从系统题库和自定义题库中查找当前题库
  return [...systemBanks.value, ...customBanks.value].find(bank => bank.id === currentBankId.value) || null
})

// 对话框中过滤后的题库列表
const filteredDialogSystemBanks = computed(() => {
  if (!bankSearchText.value.trim()) return dialogSystemBanks.value
  const searchText = bankSearchText.value.toLowerCase()
  return dialogSystemBanks.value.filter(bank => 
    bank.name.toLowerCase().includes(searchText) || 
    (bank.description && bank.description.toLowerCase().includes(searchText))
  )
})

const filteredDialogCustomBanks = computed(() => {
  if (!bankSearchText.value.trim()) return dialogCustomBanks.value
  const searchText = bankSearchText.value.toLowerCase()
  return dialogCustomBanks.value.filter(bank => 
    bank.name.toLowerCase().includes(searchText) || 
    (bank.description && bank.description.toLowerCase().includes(searchText))
  )
})

// 生成AI题库
const generateCards = async () => {
  if (!topic.value.trim()) {
    ElMessage.warning('请输入主题')
    return
  }

  try {
    isGenerating.value = true
    streamContent.value = ''
    thinkingProcess.value = '' // 清空思考过程
    loadingText.value = '正在生成闪卡...'
    startLoadingTextTyping(loadingText.value) // 启动加载文本打字机效果
    displayedThinking.value = '' // 清空显示内容
    // 清除之前的定时器
    if (typingTimer) clearInterval(typingTimer)
    if (hideTimer) clearTimeout(hideTimer)
    showCards.value = true
    cards.value = [] // 清空现有卡片
    
    // 统一使用流式接口（支持所有语言）
    const isNonChinese = language.value !== '中文'
    console.log(`[生成模式] 使用流式接口生成${language.value}内容${isNonChinese ? '（将应用智能空格处理）' : ''}`)
    // 使用闭包变量记录上次已显示的卡片数量，避免每次回调重置
    let lastCardCount = 0
    
    // 使用流式API生成闪卡
    streamEventSource = questionBankApi.generateAIBankStream(
      {
        topic: topic.value,
        scenario: scenario.value, // 新增：传递场景参数
        cardCount: parseInt(cardCount.value),
        difficulty: difficulty.value,
        language: language.value,
        withImages: withImages.value
      },
      // onMessage: 接收流式内容,在卡片内部逐字展示
      (content: string) => {
        const timestamp = new Date()
        console.log(`[前端处理] 时间=${timestamp}, 接收内容长度=${content.length}, 累积总长度=${streamContent.value.length + content.length}`)
        
        // 检查是否是特殊事件数据
        try {
          const parsed = JSON.parse(content)
          
          // 处理保存事件：用真实ID的卡片替换临时ID的卡片
          if (parsed.type === 'saved' && parsed.data) {
            console.log('💾 接收到saved事件，用真实ID替换临时ID')
            if (Array.isArray(parsed.data)) {
              // 建立临时ID到真实ID的映射
              const oldToNewIdMap = new Map()
              cards.value.forEach((oldCard, index) => {
                if (parsed.data[index]) {
                  oldToNewIdMap.set(oldCard.id, parsed.data[index].id)
                }
              })
              
              // 更新selectedCardIds中的临时ID为真实ID
              selectedCardIds.value = selectedCardIds.value.map(oldId => 
                oldToNewIdMap.get(oldId) || oldId
              )
              
              // 🔥 关键修复：合并图片数据，不直接覆盖
              cards.value = parsed.data.map((newCard: any, index: number) => {
                const oldCard = cards.value[index]
                return {
                  ...newCard,
                  // 保留旧卡片的图片数据（如果有）
                  questionImage: oldCard?.questionImage || newCard.questionImage,
                  answerImage: oldCard?.answerImage || newCard.answerImage
                }
              })
              console.log(`✅ 已更新为${cards.value.length}张真实ID的卡片，同时更新了selectedCardIds并保留图片数据`)
            }
            return
          }
          
          // 处理单张卡片图片数据（新增）
          if (parsed.type === 'image_single' && parsed.data) {
            console.log('🖼️ 接收到单张卡片图片数据')
            const cardData = parsed.data
            // 根据索引或其他标识更新对应卡片的图片
            if (cardData.index !== undefined && cards.value[cardData.index]) {
              cards.value[cardData.index].questionImage = cardData.questionImage
              cards.value[cardData.index].answerImage = cardData.answerImage
              console.log(`已更新第${cardData.index + 1}张卡片的图片`)
            } else {
              // 如果没有index，尝试通过question匹配
              const matchingCardIndex = cards.value.findIndex(
                card => card.question === cardData.question
              )
              if (matchingCardIndex >= 0) {
                cards.value[matchingCardIndex].questionImage = cardData.questionImage
                cards.value[matchingCardIndex].answerImage = cardData.answerImage
                console.log(`通过问题匹配，已更新第${matchingCardIndex + 1}张卡片的图片`)
              }
            }
            return
          }
          
          // 处理图片描述数据（保留原有逻辑）
          if (parsed.type === 'images' && parsed.data) {
            console.log('📸 接收到图片描述数据，更新卡片')
            // 更新现有卡片的图片描述
            if (Array.isArray(parsed.data)) {
              parsed.data.forEach((cardWithImage: any, index: number) => {
                if (cards.value[index]) {
                  cards.value[index].questionImage = cardWithImage.questionImage
                  cards.value[index].answerImage = cardWithImage.answerImage
                }
              })
              console.log(`已更新${parsed.data.length}张卡片的图片描述`)
            }
            return
          }
        } catch (e) {
          // 不是JSON格式，继续按普通流式内容处理
        }
        
        streamContent.value += content
        console.log(`[前端处理] 累积后总内容长度=${streamContent.value.length}`)
        
        // 清理可能的markdown代码块标记
        let cleanContent = streamContent.value.trim()
        if (cleanContent.startsWith('```json')) {
          cleanContent = cleanContent.replace(/^```json\s*/, '')
        }
        if (cleanContent.endsWith('```')) {
          cleanContent = cleanContent.replace(/```\s*$/, '')
        }
        
        // 实时提取所有卡片内容（包括完整和不完整的）
        try {
          console.log(`[前端解析] 开始解析, cleanContent长度=${cleanContent.length}`)
          const allCards: any[] = []
          
          // 先尝试解析完整JSON数组
          try {
            const parsed = JSON.parse(cleanContent)
            if (Array.isArray(parsed)) {
              console.log('[前端解析] 成功解析完整JSON数组')
              cards.value = parsed.map((card, index) => ({
                ...card,
                id: Date.now() + index
              }))
              console.log(`[前端解析] 界面已更新, 当前显示${cards.value.length}张卡片`)
              return
            }
          } catch (e) {
            console.log('[前端解析] 完整JSON解析失败，使用流式解析')
          }
          
          // 使用更健壮的方式提取JSON对象：逐个字符扫描找到完整对象
          const extractCards = (text: string) => {
            const cards: string[] = []
            let depth = 0
            let startIndex = -1
            
            for (let i = 0; i < text.length; i++) {
              if (text[i] === '{') {
                if (depth === 0) startIndex = i
                depth++
              } else if (text[i] === '}') {
                depth--
                if (depth === 0 && startIndex >= 0) {
                  const cardText = text.substring(startIndex, i + 1)
                  if (cardText.includes('"question"')) {
                    cards.push(cardText)
                  }
                  startIndex = -1
                }
              }
            }
            return cards
          }
          
          const matches = extractCards(cleanContent)
          
          if (matches.length > 0) {
            console.log(`[前端解析] 提取到${matches.length}个卡片块`)
            
            // 智能添加英文单词空格的函数
            const addSmartSpaces = (text: string): string => {
              if (!text) return text
              
              // 先处理波浪号替换（如果AI使用了波浪号）
              let result = text.replace(/~/g, ' ')
              
              // 然后处理粘连的英文单词：在小写字母后跟大写字母的位置添加空格
              // 例如: "Yourfriend" -> "Your friend"
              result = result.replace(/([a-z])([A-Z])/g, '$1 $2')
              
              // 处理数字和字母之间的粘连
              // 例如: "test123abc" -> "test 123 abc"
              result = result.replace(/(\d)([a-zA-Z])/g, '$1 $2')
              result = result.replace(/([a-zA-Z])(\d)/g, '$1 $2')
              
              return result
            }
            
            matches.forEach((block, index) => {
              const card: any = {
                id: Date.now() + index,
                question: '',
                answer: '',
                difficulty: 'medium'
              }
              
              try {
                const parsed = JSON.parse(block)
                if (parsed.question) card.question = isNonChinese ? addSmartSpaces(parsed.question) : parsed.question
                if (parsed.answer) card.answer = isNonChinese ? addSmartSpaces(parsed.answer) : parsed.answer
                if (parsed.difficulty) card.difficulty = parsed.difficulty
                if (parsed.questionImage) card.questionImage = parsed.questionImage
                if (parsed.answerImage) card.answerImage = parsed.answerImage
                console.log(`[前端解析] 块${index}完整解析成功`)
              } catch (e) {
                console.log(`[前端解析] 块${index}JSON解析失败，使用正则提取`)
                // 使用正则提取
                const questionMatch = block.match(/"question"\s*:\s*"((?:[^"\\]|\\.*)*)"/)  
                if (questionMatch) {
                  const rawQuestion = questionMatch[1].replace(/\\n/g, '\n').replace(/\\"/g, '"')
                  card.question = isNonChinese ? addSmartSpaces(rawQuestion) : rawQuestion
                }
                const answerMatch = block.match(/"answer"\s*:\s*"((?:[^"\\]|\\.*)*)"/)  
                if (answerMatch) {
                  const rawAnswer = answerMatch[1].replace(/\\n/g, '\n').replace(/\\"/g, '"')
                  card.answer = isNonChinese ? addSmartSpaces(rawAnswer) : rawAnswer
                }
                const difficultyMatch = block.match(/"difficulty"\s*:\s*"([^"]*)"/)  
                if (difficultyMatch) {
                  card.difficulty = difficultyMatch[1]
                }
                
                const questionImageMatch = block.match(/"questionImage"\s*:\s*"((?:[^"\\]|\\.)*)"/)  
                if (questionImageMatch) {
                  card.questionImage = questionImageMatch[1].replace(/\\n/g, '\n').replace(/\\"/g, '"')
                }
                
                const answerImageMatch = block.match(/"answerImage"\s*:\s*"((?:[^"\\]|\\.)*)"/)  
                if (answerImageMatch) {
                  card.answerImage = answerImageMatch[1].replace(/\\n/g, '\n').replace(/\\"/g, '"')
                }
              }
              
              if (card.question || card.answer) {
                allCards.push(card)
              }
            })
            
            if (allCards.length > 0) {
              console.log(`[前端解析] 提取到${allCards.length}张卡片, 上次显示${lastCardCount}张`)
              // 只添加新卡片，实现增量更新
              if (allCards.length > lastCardCount) {
                const newCards = allCards.slice(lastCardCount)
                console.log(`[前端解析] 新增${newCards.length}张卡片, 准备更新界面`)
                cards.value = [...cards.value, ...newCards]
                lastCardCount = allCards.length
                console.log(`[前端解析] 界面已更新, 当前显示${cards.value.length}张卡片`)
              }
            }
          }
        } catch (parseError) {
          console.error('解析错误:', parseError)
        }
      },
      // onError: 错误处理
      (error: string) => {
        isGenerating.value = false
        // 清除加载文本定时器
        if (loadingTextTimer) {
          clearInterval(loadingTextTimer)
          loadingTextTimer = null
        }
        displayedLoadingText.value = ''
        ElMessage.error(error || '生成闪卡失败，请重试')
      },
      // onComplete: 完成处理
      () => {
        isGenerating.value = false
        // 清除加载文本定时器
        if (loadingTextTimer) {
          clearInterval(loadingTextTimer)
          loadingTextTimer = null
        }
        displayedLoadingText.value = ''
        if (cards.value.length > 0) {
          ElMessage.success(`闪卡生成成功！共生成${cards.value.length}张卡片`)
          // 刷新历史生成记录列表
          if (userStore.isLoggedIn && userStore.userInfo) {
            loadHistoryRecords()
          }
        } else {
          ElMessage.warning('未能解析生成的卡片，请重试')
        }
      },
      // onThinking: 接收思考过程
      (thinking: string) => {
        console.log('🧠 接收到思考过程:', thinking.substring(0, 100))
        // 启动打字机效果
        startTypingEffect(thinking)
      }
    )
  } catch (error) {
    isGenerating.value = false
    // 清除加载文本定时器
    if (loadingTextTimer) {
      clearInterval(loadingTextTimer)
      loadingTextTimer = null
    }
    displayedLoadingText.value = ''
    ElMessage.error('生成闪卡失败，请重试')
  }
}

// 加载推荐题库（支持分页）
const loadSystemBanks = async () => {
  try {
    isLoadingBanks.value = true
    // 使用bankSearchText作为搜索关键词
    const response = await questionBankApi.getSystemBanks(bankSearchText.value || '', systemPage.value, systemPageSize.value)
    systemBanks.value = response.data.data || []
    systemTotal.value = response.data.total || 0
  } catch (error) {
    ElMessage.error('加载题库失败')
  } finally {
    isLoadingBanks.value = false
  }
}

// 加载用户自定义题库（支持分页）
const loadCustomBanks = async () => {
  try {
    isLoadingCustomBanks.value = true
    const response = await questionBankApi.getUserCustomBanks(customPage.value, customPageSize.value)
    customBanks.value = response.data.data || []
    customTotal.value = response.data.total || 0
  } catch (error) {
    ElMessage.error('加载自定义题库失败')
  } finally {
    isLoadingCustomBanks.value = false
  }
}

// 加载历史生成记录
const loadHistoryRecords = async (page: number = 1, loadMore: boolean = false) => {
  try {
    // 如果用户未登录或没有用户信息，则不加载历史记录
    if (!userStore.isLoggedIn || !userStore.userInfo) {
      return
    }
    
    isLoadingHistory.value = true
    const response = await questionBankApi.searchBanks({
      page: page,
      pageSize: historyPageSize.value,
      sortBy: 'created_at', // 修改为正确的数据库字段名
      sortOrder: 'desc',
      userId: userStore.userInfo.id,
      type: 'ai' // 只加载AI生成的题库，排除手动创建的题库
    })
    
    // 适配实际的API返回格式
    // 后端返回的是 {code, message, data: {data: [...], total, page, pageSize, totalPages}}
    const pageResponse = response.data || {}
    const records = pageResponse.data || []
    
    if (loadMore) {
      historyRecords.value = [...historyRecords.value, ...records]
    } else {
      historyRecords.value = records
    }
    
    // 更新总记录数
    historyTotal.value = pageResponse.total || 0
  } catch (error) {
    console.error('加载历史记录失败:', error)
    ElMessage.error('加载历史记录失败')
  } finally {
    isLoadingHistory.value = false
  }
}

// 处理历史记录分页变化
const handleHistoryPageChange = async (page: number) => {
  historyPage.value = page
  await loadHistoryRecords(page, false)
}

// 加载分享记录
const loadSharedBanks = async () => {
  try {
    isLoadingShared.value = true
    const response = await questionBankApi.getSharedRecords()
    sharedBanks.value = response.data || []
  } catch (error) {
    console.error('加载分享记录失败:', error)
    ElMessage.error('加载分享记录失败')
  } finally {
    isLoadingShared.value = false
  }
}

// 加载指定题库的卡片
const loadBankCards = async (bankId: number) => {
  try {
    // 添加严格的类型检查，确保bankId是有效的数字
    if (!bankId || typeof bankId !== 'number' || isNaN(bankId) || bankId <= 0) {
      console.error('无效的题库ID:', bankId)
      ElMessage.error('无效的题库ID')
      return
    }
    
    isLoadingBanks.value = true
    currentBankId.value = bankId // 保存当前题库ID
    const response = await questionBankApi.getBankCards(bankId)
    cards.value = response.data
    showCards.value = true
    ElMessage.success('题库加载成功！')
  } catch (error) {
    console.error('加载题库卡片失败:', error)
    ElMessage.error('加载题库卡片失败')
  } finally {
    isLoadingBanks.value = false
  }
}

const flipCard = (index: number) => {
  // 如果是单个卡片模式，使用原来的isFlipped
  if (cards.value.length === 1) {
    isFlipped.value = !isFlipped.value
  } else {
    // 如果是田字格模式，使用flippedCards数组
    if (!flippedCards.value[index]) {
      flippedCards.value[index] = false
    }
    flippedCards.value[index] = !flippedCards.value[index]
  }
}

const _nextCard = () => {
  if (currentCardIndex.value < cards.value.length - 1) {
    currentCardIndex.value++
    isFlipped.value = false
  }
}

const _previousCard = () => {
  if (currentCardIndex.value > 0) {
    currentCardIndex.value--
    isFlipped.value = false
  }
}

const backToHome = () => {
  showCards.value = false
  currentCardIndex.value = 0
  isFlipped.value = false
  flippedCards.value = []
  // stopAudio() // 这个函数可能不存在
  isSelectionMode.value = false
  selectedCardIds.value = []
  cards.value = []
  loadSystemBanks() // 返回首页时重新加载题库
  loadCustomBanks() // 返回首页时重新加载自定义题库
}

// 导出题库为Excel
const exportBank = async (bankId: number, bankName: string) => {
  try {
    const token = localStorage.getItem('token')?.trim()
    if (!token) {
      ElMessage.error('请先登录')
      return
    }
    
    // 使用相对路径,自动适配当前域名和协议
    const url = `/api/question-bank/${bankId}/export`
    
    // 使用fetch下载文件
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
    
    if (!response.ok) {
      const errorText = await response.text()
      console.error('导出失败:', response.status, errorText)
      throw new Error(`导出失败: ${response.status}`)
    }
    
    // 创建blob并触发下载
    const blob = await response.blob()
    const downloadUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = `${bankName}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(downloadUrl)
    
    ElMessage.success('导出成功！')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败,请稍后重试')
  }
};

// 切换选题模式
const toggleSelectionMode = () => {
  isSelectionMode.value = !isSelectionMode.value
  if (!isSelectionMode.value) {
    selectedCardIds.value = []
  }
}

// 切换当前卡片的选中状态
const toggleCardSelection = (index: number) => {
  const currentId = cards.value[index]?.id
  if (!currentId) return
  
  const idIndex = selectedCardIds.value.indexOf(currentId)
  if (idIndex > -1) {
    selectedCardIds.value.splice(idIndex, 1)
  } else {
    selectedCardIds.value.push(currentId)
  }
}

// 显示添加到题库对话框
const showAddToBankDialog = async () => {
  if (selectedCardIds.value.length === 0) {
    ElMessage.warning('请先选择要添加的卡片')
    return
  }
  // 打开对话框前加载所有题库（不使用分页，一次性加载所有）
  try {
    isLoadingDialogBanks.value = true
    // 加载所有系统题库
    const systemResponse = await questionBankApi.getSystemBanks('', 1, 1000)
    dialogSystemBanks.value = systemResponse.data.data || []
    // 加载所有用户自定义题库
    const customResponse = await questionBankApi.getUserCustomBanks(1, 1000)
    dialogCustomBanks.value = customResponse.data.data || []
  } catch (error) {
    ElMessage.error('加载题库列表失败')
  } finally {
    isLoadingDialogBanks.value = false
  }
  showBankDialog.value = true
}

// 添加卡片到指定题库
const addSelectedCardsToBank = async () => {
  if (!selectedTargetBankId.value) {
    ElMessage.warning('请选择目标题库')
    return
  }
  
  try {
    const targetId = selectedTargetBankId.value
    
    // 直接使用原有接口（通过ID复制卡片）
    // 因为流式生成的卡片已经在生成完成后自动保存到数据库，拥有真实ID
    await questionBankApi.addCardsToBank({
      targetBankId: targetId,
      cardIds: selectedCardIds.value
    })
    
    ElMessage.success(`成功添加 ${selectedCardIds.value.length} 张卡片到题库`)
    showBankDialog.value = false
    selectedCardIds.value = []
    isSelectionMode.value = false
    selectedTargetBankId.value = null
    // 刷新题库列表（更新卡片数量等信息）
    await loadSystemBanks()
    await loadCustomBanks()
    // 注意：不再自动跳转到目标题库，保持在当前生成卡片界面
  } catch (error) {
    ElMessage.error('添加卡片失败，请重试')
  }
}

// 创建自定义题库
const handleCreateBank = async () => {
  if (!createBankForm.value.name || !createBankForm.value.topic) {
    ElMessage.warning('请填写题库名称和主题')
    return
  }
  
  try {
    const response = await questionBankApi.createCustomBank({
      name: createBankForm.value.name,
      topic: createBankForm.value.topic,
      description: createBankForm.value.description,
      difficulty: createBankForm.value.difficulty,
      language: createBankForm.value.language
    })
    
    // 先关闭对话框
    showCreateBankDialog.value = false
    
    // 重置表单
    createBankForm.value = {
      name: '',
      topic: '',
      description: '',
      difficulty: 'medium',
      language: '中文'
    }
    
    // 刷新对话框的题库列表
    try {
      const [systemResponse, customResponse] = await Promise.all([
        questionBankApi.getSystemBanks('', 1, 1000),
        questionBankApi.getUserCustomBanks(1, 1000)
      ])
      
      dialogSystemBanks.value = systemResponse.data.data || []
      dialogCustomBanks.value = customResponse.data.data || []
    } catch (error) {
      console.error('刷新题库列表失败:', error)
    }
    
    // 自动选中新创建的题库
    if (response.data && response.data.id) {
      selectedTargetBankId.value = response.data.id
    }
    
    ElMessage.success('题库创建成功！')
  } catch (error) {
    ElMessage.error('创建题库失败，请重试')
  }
}

// 处理文件选择
const handleFileChange = (file: any) => {
  fileList.value = [file]
}

// 加载用户题库列表
const loadUserBanks = async () => {
  try {
    const response = await questionBankApi.getUserCustomBanks(1, 1000)
    userBanks.value = response.data.data || []
  } catch (error) {
    console.error('加载用户题库失败:', error)
    ElMessage.error('加载用户题库失败')
  }
}

// 导入题库
const handleImport = async () => {
  if (fileList.value.length === 0) {
    ElMessage.warning('请选择要导入的Excel文件')
    return
  }

  // 如果是导入到已有题库，检查是否选择了题库
  if (importForm.value.importMode === 'existing' && !importForm.value.targetBankId) {
    ElMessage.warning('请选择要导入的题库')
    return
  }

  const file = fileList.value[0].raw
  if (!file.name.endsWith('.xlsx')) {
    ElMessage.warning('仅支持.xlsx格式的Excel文件')
    return
  }

  try {
    isImporting.value = true
    const formData = new FormData()
    formData.append('file', file)
    
    if (importForm.value.importMode === 'existing') {
      // 导入到已有题库
      formData.append('targetBankId', importForm.value.targetBankId!.toString())
    } else {
      // 创建新题库
      if (importForm.value.bankName) {
        formData.append('bankName', importForm.value.bankName)
      }
      if (importForm.value.description) {
        formData.append('description', importForm.value.description)
      }
      formData.append('difficulty', importForm.value.difficulty)
      formData.append('language', importForm.value.language)
    }

    const result = await questionBankApi.importBankFromExcel(formData)
    
    ElMessage({
      message: '导入成功！',
      type: 'success',
      showClose: false,
      duration: 2000
    })
    showImportDialog.value = false
    // 重置表单
    importForm.value = {
      importMode: 'new',
      targetBankId: null,
      bankName: '',
      description: '',
      difficulty: 'medium',
      language: '中文'
    }
    fileList.value = []
    // 刷新题库列表
    await loadCustomBanks()
    await loadSystemBanks()
  } catch (error: any) {
    console.error('导入失败:', error)
    ElMessage.error(error.message || '导入失败，请重试')
  } finally {
    isImporting.value = false
  }
}

// 显示导入对话框
const handleShowImportDialog = async () => {
  // 加载用户题库列表
  await loadUserBanks()
  showImportDialog.value = true
}

// 显示编辑题库对话框
const showEditBankDialogFunc = (bank: QuestionBank) => {
  editBankForm.value = {
    id: bank.id,
    name: bank.name,
    description: bank.description || '',
    difficulty: bank.difficulty || 'medium',
    language: bank.language || '中文'
  }
  showEditBankDialog.value = true
}

// 更新题库
const handleUpdateBank = async () => {
  if (!editBankForm.value.name.trim()) {
    ElMessage.warning('请输入题库名称')
    return
  }

  try {
    const response = await questionBankApi.updateBank({
      id: editBankForm.value.id!,
      name: editBankForm.value.name,
      description: editBankForm.value.description,
      difficulty: editBankForm.value.difficulty,
      language: editBankForm.value.language
    })
    
    ElMessage({
      message: '更新成功！',
      type: 'success',
      showClose: false,
      duration: 2000
    })
    showEditBankDialog.value = false
    // 刷新题库列表
    await loadCustomBanks()
    await loadSystemBanks()
  } catch (error: any) {
    console.error('更新失败:', error)
    ElMessage.error(error.message || '更新失败，请重试')
  }
}

// 删除题库
const handleDeleteBank = async (bankId: number) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这个题库吗？删除后将无法恢复。',
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await questionBankApi.deleteBank(bankId)
    
    ElMessage.success('删除成功！')
    // 刷新题库列表
    await loadCustomBanks()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error(error.message || '删除失败，请重试')
    }
  }
}

const handleDeleteCurrentCard = () => {
  // 在田字格模式下，删除功能需要特殊处理
  // 简化为提示用户使用批量删除功能
  ElMessage.warning('在田字格模式下，请先选择卡片再进行删除操作')
}

// 播放问题/答案语音
const playQuestion = async (index: number) => {
  // 如果没有卡片，直接返回
  if (!cards.value || index >= cards.value.length) return
  
  const card = cards.value[index]
  // 获取要播放的文本（根据卡片翻转状态决定播放问题还是答案）
  const textToPlay = flippedCards.value[index] ? card.answer : card.question
  
  // 如果没有文本，不进行播放
  if (!textToPlay || textToPlay.trim() === '') {
    ElMessage.warning('没有可播放的内容')
    return
  }
  
  try {
    // 设置播放状态
    isPlayingQuestion.value = true
    playingQuestionIndex.value = index
    
    // 使用浏览器的语音合成API
    if ('speechSynthesis' in window) {
      // 停止任何正在播放的语音
      window.speechSynthesis.cancel()
      
      // 创建语音实例
      const utterance = new SpeechSynthesisUtterance(textToPlay)
      
      // 设置语音属性
      utterance.lang = 'zh-CN' // 使用中文语音
      utterance.rate = 1.0 // 语速
      utterance.pitch = 1.0 // 音高
      
      // 播放结束时的回调
      utterance.onend = () => {
        isPlayingQuestion.value = false
        playingQuestionIndex.value = -1
      }
      
      // 播放出错时的回调
      utterance.onerror = () => {
        isPlayingQuestion.value = false
        playingQuestionIndex.value = -1
        ElMessage.error('播放失败')
      }
      
      // 开始播放
      window.speechSynthesis.speak(utterance)
    } else {
      // 浏览器不支持语音合成
      ElMessage.error('您的浏览器不支持语音播放功能')
      isPlayingQuestion.value = false
      playingQuestionIndex.value = -1
    }
  } catch (error) {
    console.error('播放语音失败:', error)
    ElMessage.error('播放失败')
    isPlayingQuestion.value = false
    playingQuestionIndex.value = -1
  }
}

const handleDeleteSelectedCards = async () => {
  if (selectedCardIds.value.length === 0) {
    ElMessage.warning('请先选择要删除的卡片')
    return
  }

  if (selectedCardIds.value.length === cards.value.length) {
    ElMessage.warning('不能删除所有卡片，题库至少需要保留一张卡片')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedCardIds.value.length} 张卡片吗？删除后将无法恢复。`,
      '批量删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    // 批量删除卡片
    const deletePromises = selectedCardIds.value.map(cardId => 
      questionBankApi.deleteCard(cardId)
    )
    
    await Promise.all(deletePromises)
    
    ElMessage.success(`成功删除 ${selectedCardIds.value.length} 张卡片！`)
    
    // 从卡片列表中移除已删除的卡片
    cards.value = cards.value.filter(card => !selectedCardIds.value.includes(card.id))
    
    // 清空选中状态
    selectedCardIds.value = []
    isSelectionMode.value = false
    
    // 刷新题库列表
    await loadSystemBanks()
    await loadCustomBanks()
    
    // 调整当前卡片索引
    if (cards.value.length > 0) {
      if (currentCardIndex.value >= cards.value.length) {
        currentCardIndex.value = cards.value.length - 1
      }
    } else {
      // 如果没有卡片了，返回主页
      ElMessage.info('题库已无卡片')
      backToHome()
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('批量删除卡片失败:', error)
      ElMessage.error(error.message || '批量删除卡片失败，请重试')
    }
  }
}

// 处理问题图片变化
const handleQuestionImageChange = (file: any) => {
  const reader = new FileReader()
  reader.onload = (e) => {
    addCardForm.value.questionImage = e.target?.result as string
  }
  reader.readAsDataURL(file.raw)
  questionImageFileList.value = [file]
}

// 处理答案图片变化
const handleAnswerImageChange = (file: any) => {
  const reader = new FileReader()
  reader.onload = (e) => {
    addCardForm.value.answerImage = e.target?.result as string
  }
  reader.readAsDataURL(file.raw)
  answerImageFileList.value = [file]
}

const handleAddNewCard = () => {
  if (!currentBank.value) {
    ElMessage.warning('请先选择一个题库')
    return
  }
  
  // 重置表单
  addCardForm.value = {
    question: '',
    answer: '',
    questionImage: '',
    answerImage: ''
  }
  questionImageFileList.value = []
  answerImageFileList.value = []
  
  // 打开对话框
  showAddCardDialog.value = true
}

const handleSubmitNewCard = async () => {
  // 验证必填项
  if (!addCardForm.value.question.trim()) {
    ElMessage.warning('请输入问题内容')
    return
  }
  
  if (!addCardForm.value.answer.trim()) {
    ElMessage.warning('请输入答案内容')
    return
  }
  
  try {
    const newCard = await questionBankApi.addCard(currentBank.value!.id, {
      question: addCardForm.value.question.trim(),
      answer: addCardForm.value.answer.trim(),
      questionImage: addCardForm.value.questionImage.trim() || undefined,
      answerImage: addCardForm.value.answerImage.trim() || undefined
    })

    ElMessage.success('卡片创建成功！')
    
    // 关闭对话框
    showAddCardDialog.value = false
    
    ElMessage.success('卡片添加成功！')
    
    // 刷新当前题库的卡片列表（从服务器重新加载）
    if (currentBankId.value) {
      await loadBankCards(currentBankId.value)
      // 保持在当前卡片，不自动跳转到新添加的卡片
    }
    
    // 刷新题库列表（更新卡片数量）
    await loadSystemBanks()
    await loadCustomBanks()
  } catch (error: any) {
    console.error('创建卡片失败:', error)
    ElMessage.error(error.message || '创建卡片失败，请重试')
  }
}

// 处理编辑当前卡片
const handleEditCurrentCard = () => {
  if (!currentCard.value || !currentCard.value.id) {
    ElMessage.warning('无效的卡片')
    return
  }
  
  // 检查当前题库类型
  if (currentBank.value && currentBank.value.type === 'system') {
    ElMessage.warning('系统题库不支持编辑')
    return
  }
  
  // 填充编辑表单
  editCardForm.value = {
    id: currentCard.value.id,
    question: currentCard.value.question,
    answer: currentCard.value.answer,
    questionImage: currentCard.value.questionImage || '',
    answerImage: currentCard.value.answerImage || ''
  }
  
  // 设置图片文件列表（用于回显）
  editQuestionImageFileList.value = currentCard.value.questionImage 
    ? [{ url: currentCard.value.questionImage }] 
    : []
  editAnswerImageFileList.value = currentCard.value.answerImage 
    ? [{ url: currentCard.value.answerImage }] 
    : []
  
  showEditCardDialog.value = true
}

// 处理编辑卡片的问题图片变化
const handleEditQuestionImageChange = (file: any) => {
  const reader = new FileReader()
  reader.onload = (e) => {
    editCardForm.value.questionImage = e.target?.result as string
  }
  reader.readAsDataURL(file.raw)
  editQuestionImageFileList.value = [file]
}

// 处理编辑卡片的答案图片变化
const handleEditAnswerImageChange = (file: any) => {
  const reader = new FileReader()
  reader.onload = (e) => {
    editCardForm.value.answerImage = e.target?.result as string
  }
  reader.readAsDataURL(file.raw)
  editAnswerImageFileList.value = [file]
}

// 删除编辑卡片的问题图片
const handleRemoveEditQuestionImage = () => {
  editCardForm.value.questionImage = ''
  editQuestionImageFileList.value = []
}

// 删除编辑卡片的答案图片
const handleRemoveEditAnswerImage = () => {
  editCardForm.value.answerImage = ''
  editAnswerImageFileList.value = []
}

// 提交编辑卡片
const handleSubmitEditCard = async () => {
  // 验证必填项
  if (!editCardForm.value.question.trim()) {
    ElMessage.warning('请输入问题内容')
    return
  }
  
  if (!editCardForm.value.answer.trim()) {
    ElMessage.warning('请输入答案内容')
    return
  }
  
  if (!editCardForm.value.id) {
    ElMessage.error('卡片ID无效')
    return
  }
  
  try {
    await questionBankApi.updateCard(editCardForm.value.id, {
      question: editCardForm.value.question.trim(),
      answer: editCardForm.value.answer.trim(),
      questionImage: editCardForm.value.questionImage.trim() || undefined,
      answerImage: editCardForm.value.answerImage.trim() || undefined
    })

    ElMessage.success('卡片更新成功！')
    
    // 关闭对话框
    showEditCardDialog.value = false
    
    // 刷新当前题库的卡片列表
    if (currentBankId.value) {
      await loadBankCards(currentBankId.value)
    }
    
    // 刷新题库列表
    await loadSystemBanks()
    await loadCustomBanks()
  } catch (error: any) {
    console.error('更新卡片失败:', error)
    ElMessage.error(error.message || '更新卡片失败，请重试')
  }
}

const stopAudio = () => {
  if (audioElement) {
    audioElement.pause()
    audioElement.currentTime = 0
    audioElement = null
  }
  isPlayingQuestion.value = false
}

const base64ToBlob = (base64: string, mimeType: string): Blob => {
  const byteCharacters = atob(base64)
  const byteNumbers = new Array(byteCharacters.length)
  for (let i = 0; i < byteCharacters.length; i++) {
    byteNumbers[i] = byteCharacters.charCodeAt(i)
  }
  const byteArray = new Uint8Array(byteNumbers)
  return new Blob([byteArray], { type: mimeType })
}

// 退出登录
const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

// 监听搜索文本变化(防抖)
let searchTimer: number | null = null
watch(bankSearchText, () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    loadSystemBanks()
    loadCustomBanks()
  }, 300)
})

// 分享题库 - 打开分享对话框
const handleShareBank = (bankId: number) => {
  currentSharingBankId.value = bankId
  shareCode.value = ''
  shareExpireHours.value = null // 默认永久有效
  showShareDialog.value = true
}

// 确认生成分享码
const confirmGenerateShare = async () => {
  if (!currentSharingBankId.value) return
  
  try {
    const response = await questionBankApi.generateShareCode(
      currentSharingBankId.value,
      shareExpireHours.value || undefined
    )
    shareCode.value = response.data
    ElMessage.success('分享码生成成功')
  } catch (error) {
    console.error('生成分享码失败:', error)
    ElMessage.error('生成分享码失败，请稍后重试')
    showShareDialog.value = false
  }
}

// 复制分享码
const copyShareCode = async () => {
  try {
    await navigator.clipboard.writeText(shareCode.value)
    ElMessage.success('分享码已复制到剪贴板')
  } catch (error) {
    // 如果剪贴板API失败，使用传统方法
    const textarea = document.createElement('textarea')
    textarea.value = shareCode.value
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    ElMessage.success('分享码已复制到剪贴板')
  }
}

// 直接复制分享码
const copyShareCodeDirect = async (code: string) => {
  try {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      await navigator.clipboard.writeText(code)
    } else {
      const textArea = document.createElement('textarea')
      textArea.value = code
      textArea.style.position = 'fixed'
      textArea.style.top = '0'
      textArea.style.left = '0'
      textArea.style.opacity = '0'
      document.body.appendChild(textArea)
      textArea.focus()
      textArea.select()
      document.execCommand('copy')
      document.body.removeChild(textArea)
    }
    ElMessage.success('分享码已复制到剪贴板')
  } catch (error) {
    console.error('复制失败:', error)
    ElMessage.error('复制失败，请手动复制')
  }
}

// 通过分享码访问题库
const handleAccessSharedBank = async () => {
  if (!accessShareCode.value || accessShareCode.value.trim().length !== 6) {
    ElMessage.warning('请输入正确的6位分享码')
    return
  }
  
  try {
    const response = await questionBankApi.getByShareCode(accessShareCode.value.trim())
    const bank = response.data
    
    // 检查返回的题库数据是否有效
    if (!bank || !bank.id) {
      ElMessage.error('未找到该分享码对应的题库')
      return
    }
    
    // 加载该题库的卡片并显示
    await loadBankCards(bank.id)
    
    showAccessDialog.value = false
    accessShareCode.value = ''
    ElMessage.success('成功访问分享题库')
  } catch (error: any) {
    console.error('访问分享题库失败:', error)
    const errorMsg = error.response?.data?.message || '访问失败，请检查分享码是否正确'
    ElMessage.error(errorMsg)
  }
}

// 页面加载时获取题库列表
onMounted(() => {
  loadSystemBanks()
  loadCustomBanks()
  
  // 如果用户已登录，加载历史生成记录和分享记录
  if (userStore.isLoggedIn) {
    // 尝试获取用户信息
    userStore.fetchUserInfo().then(() => {
      loadHistoryRecords()
      loadSharedBanks()
    })
  }
})
</script>

<style scoped>
/* 全局样式重置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.home-container {
  width: 100%;
  min-height: 100vh;
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  position: relative;
  overflow: hidden;
}

/* 主内容区域布局 */
.main-content {
  width: 100%;
  max-width: 1600px;
  display: flex;
  gap: 30px;
  position: relative;
  z-index: 1;
  flex-direction: row;
  margin: 0 auto;
}

.content-section {
  flex: 1;
  min-width: 0;
}

/* 左侧区域样式 */
.left-section {
  flex: 1;
  max-width: 50%;
}

/* 右侧区域样式 */
.right-section {
  flex: 1;
  max-width: 50%;
  backdrop-filter: blur(10px);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.username {
  color: #333;
  font-weight: 600;
  font-size: 13px;
  white-space: nowrap;
}

.content {
  width: 100%;
  max-width: 100%;
  position: relative;
  z-index: 1;
}

/* 创建闪卡表单样式 */
.create-card-form {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 30px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.1);
  animation: fadeIn 0.8s ease-out;
  backdrop-filter: blur(10px);
  width: 100%;
}

.form-item {
  margin-bottom: 30px;
  position: relative;
}

.form-item .el-input__wrapper {
  border-radius: 15px !important;
  height: 60px;
  font-size: 16px;
  background-color: white !important;
  border: none !important;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.form-item .el-input__wrapper:hover {
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.08);
}

.form-item .el-input__wrapper:focus-within {
  box-shadow: 0 8px 30px rgba(79, 172, 254, 0.2);
}

.form-options {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 40px;
}

.form-select .el-select__wrapper {
  border-radius: 15px !important;
  height: 60px;
  font-size: 16px;
  background-color: white !important;
  border: none !important;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.form-select .el-select__wrapper:hover {
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.08);
}

.form-select .el-select__wrapper:focus-within {
  box-shadow: 0 8px 30px rgba(79, 172, 254, 0.2);
}

/* 生成按钮样式 */
.generate-button {
  width: 100%;
  font-size: 18px;
  height: 60px;
  border-radius: 30px;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  border: none;
  transition: all 0.3s ease;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 1px;
  box-shadow: 0 10px 30px rgba(79, 172, 254, 0.3);
}

.generate-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 15px 35px rgba(79, 172, 254, 0.4);
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.generate-button:disabled {
  opacity: 0.6;
  transform: none;
  box-shadow: 0 5px 15px rgba(79, 172, 254, 0.2);
}

/* 卡片展示区样式 */
.cards-section {
  width: 100%;
  max-width: 1000px;
  position: relative;
  z-index: 1;
  animation: fadeIn 0.8s ease-out;
}

/* 全屏宽度的卡片展示区 */
.cards-section.full-width {
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
}

/* 题库样式 */
.question-bank {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 30px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
  width: 100%;
  max-height: 600px;
  overflow-y: auto;
}

.bank-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.bank-header h2 {
  color: #333;
  font-size: 24px;
  font-weight: 700;
}

.bank-search {
  width: 200px;
}

.bank-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.bank-card {
  cursor: pointer;
  transition: all 0.3s ease;
  background: white !important;
  border: none !important;
  border-radius: 15px !important;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.bank-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.bank-description {
  color: #666;
  line-height: 1.6;
  margin-bottom: 15px;
  font-size: 14px;
  word-wrap: break-word;
  word-break: break-word;
  white-space: normal;
  overflow-wrap: break-word;
}

.bank-meta {
  display: flex;
  gap: 15px;
  font-size: 12px;
  color: #999;
}

.bank-difficulty,
.bank-language {
  padding: 4px 12px;
  border-radius: 12px;
  background: rgba(64, 158, 255, 0.1);
  color: #409eff;
  font-weight: 500;
}

/* 创建题库按钮容器 */
.create-bank-button-container {
  margin-top: 20px;
  padding: 20px;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.05) 0%, rgba(103, 194, 58, 0.05) 100%);
  border-radius: 15px;
  border: 2px dashed rgba(64, 158, 255, 0.3);
}

.create-bank-button {
  font-size: 16px;
  font-weight: 600;
  padding: 12px 32px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
  transition: all 0.3s ease;
}

.create-bank-button:hover {
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.6);
  background: linear-gradient(135deg, #764ba2 0%, #667eea 100%);
}

.create-bank-button:active {
  transform: translateY(0) scale(1);
  box-shadow: 0 2px 10px rgba(102, 126, 234, 0.4);
  white-space: nowrap;
}

.bank-difficulty {
  background: rgba(103, 194, 58, 0.1);
  color: #67c23a;
}

.bank-language {
  background: rgba(230, 162, 60, 0.1);
  color: #e6a23c;
}

.no-data {
  text-align: center;
  padding: 40px;
  color: #999;
  font-size: 14px;
}

.cards-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 40px;
  color: white;
  width: 100%;
}

.header-left {
  flex-direction: column;
  gap: 10px;
}

.cards-header h2 {
  font-size: 28px;
  text-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  font-weight: 700;
}

.card-progress {
  background: rgba(255, 255, 255, 0.2);
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  backdrop-filter: blur(10px);
  display: inline-block;
  align-self: flex-start;
}

.cards-header .el-button {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  backdrop-filter: blur(10px);
  height: 50px;
  padding: 0 30px;
  font-size: 16px;
  border-radius: 25px;
  transition: all 0.3s ease;
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.1);
}

.cards-header .el-button:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
}

/* 卡片容器样式 */
.card-container {
  perspective: 1000px;
  margin-bottom: 30px;
  min-height: 600px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

/* 田字格网格容器 */
.grid-container {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 30px;
  width: 100%;
  max-width: 1200px;
  padding: 20px;
}

/* 网格项样式 */
.grid-item {
  position: relative;
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 卡片复选框样式 */
.card-checkbox {
  position: absolute;
  top: -10px;
  right: 12px;
  z-index: 1000;
  cursor: pointer;
}

.checkbox-box {
  width: 20px;
  height: 20px;
  border: 2px solid #409EFF;
  border-radius: 4px;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.checkbox-box:hover {
  border-color: #66b1ff;
  transform: scale(1.1);
}

.checkbox-box.checked {
  background: #409EFF;
  border-color: #409EFF;
}

.checkmark {
  color: white;
  font-size: 14px;
  font-weight: bold;
  line-height: 1;
}

.generating-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 30px;
  box-shadow: 0 15px 45px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
  padding: 30px;
}

.stream-content {
  margin-top: 20px;
  width: 100%;
  max-width: 800px;
  max-height: 400px;
  overflow-y: auto;
  background: rgba(240, 245, 255, 0.6);
  border-radius: 15px;
  padding: 20px;
  text-align: left;
}

.stream-label {
  font-size: 14px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 10px;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.stream-text {
  font-size: 14px;
  line-height: 1.8;
  color: #303133;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: 'Courier New', monospace;
}

.flip-card {
  width: 100%;
  height: 100%;
  cursor: pointer;
  transition: transform 0.3s ease;
}

.flip-card:hover {
  transform: scale(1.02);
}

.flip-card-inner {
  position: relative;
  width: 100%;
  height: 100%;
  text-align: center;
  transition: transform 0.8s cubic-bezier(0.2, 0.85, 0.4, 1.275);
  transform-style: preserve-3d;
}

.flip-card.flipped .flip-card-inner {
  transform: rotateY(180deg);
}

.flip-card-front,
.flip-card-back {
  position: absolute;
  width: 100%;
  height: 100%;
  backface-visibility: hidden;
  border-radius: 30px;
  box-shadow: 0 15px 45px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
}

.flip-card-front {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: white;
}

.flip-card-back {
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
  color: #333;
  transform: rotateY(180deg);
}

.card-content {
  padding: 20px 20px;
  text-align: center;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  overflow: visible;
}

.card-label {
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 10px;
  opacity: 0.9;
  font-weight: 600;
}

.card-text {
  font-size: clamp(18px, 4vw, 36px);
  line-height: 1.6;
  margin-bottom: 10px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  word-wrap: break-word;
  word-break: normal;
  white-space: normal;
  overflow-wrap: break-word;
  hyphens: auto;
  flex: 1;
}

.card-image {
  margin-top: 10px;
  margin-bottom: 10px;
  padding: 8px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 12px;
  backdrop-filter: blur(5px);
  width: 100%;
  max-width: 600px;
  /* 移除固定宽高比限制，让图片能够完整显示 */
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
}

.card-img {
  max-width: 100%;
  max-height: 400px; /* 增加最大高度，确保图片能够完整显示 */
  width: auto;
  height: auto;
  border-radius: 10px;
  object-fit: contain; /* 保持图片比例，确保完全显示 */
}

.image-slot {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px; /* 增加占位区域高度 */
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
}

.image-description {
  font-size: 12px;
  line-height: 1.4;
  color: rgba(255, 255, 255, 0.95);
  font-style: italic;
  text-align: left;
  word-wrap: break-word;
}

.flip-card-back .image-description {
  color: #555;
}

.card-actions {
  margin-top: 32px;
  display: flex;
  justify-content: center;
  gap: 8px;
}

.card-actions .el-button {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  backdrop-filter: blur(10px);
  width: 45px;
  height: 45px;
  border-radius: 50%;
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.card-actions .el-button:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: scale(1.1);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
}

.play-button {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
  border: none !important;
  width: 50px !important;
  height: 50px !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  transition: all 0.3s ease !important;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4) !important;
}

.play-button:hover {
  background: linear-gradient(135deg, #764ba2 0%, #667eea 100%) !important;
  transform: scale(1.15) rotate(5deg) !important;
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.6) !important;
}

.play-button svg {
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.2));
}

.flip-hint {
  font-size: 11px;
  opacity: 0.7;
  margin-top: 10px;
  font-weight: 400;
}

/* 卡片控制按钮样式 */
.card-controls {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 30px;
}

.card-controls .el-button {
  width: 120px;
  height: 60px;
  font-size: 18px;
  border-radius: 30px;
  transition: all 0.3s ease;
  font-weight: 600;
}

.card-controls .el-button:not(.el-button--primary) {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: white;
  backdrop-filter: blur(10px);
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.1);
}

.card-controls .el-button:not(.el-button--primary):hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.3);
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
}

.card-controls .el-button--primary {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  border: none;
  box-shadow: 0 10px 30px rgba(79, 172, 254, 0.3);
}

.card-controls .el-button--primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 15px 35px rgba(79, 172, 254, 0.4);
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.card-controls .el-button:disabled {
  opacity: 0.6;
  transform: none;
}

/* 动画效果 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes float {
  0% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(-15px);
  }
  100% {
    transform: translateY(0px);
  }
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .main-content {
    flex-direction: row;
    gap: 20px;
  }
  
  .content-section {
    flex: 1;
    min-width: 0;
  }
  
  .left-section,
  .right-section {
    max-width: 50%;
  }
  
  .cards-section.full-width {
    position: static;
    transform: none;
    margin-top: 20px;
  }
  
  .question-bank {
    max-height: 500px;
  }
}

@media (max-width: 768px) {
  .home-container {
    padding: 20px 15px;
  }
  
  .header h1 {
    font-size: 36px;
  }
  
  .header p {
    font-size: 18px;
  }
  
  .logo {
    width: 80px;
    height: 80px;
  }
  
  .logo-icon {
    font-size: 36px;
  }
  
  .create-card-form {
    padding: 30px 20px;
    border-radius: 20px;
  }
  
  .form-options {
    grid-template-columns: 1fr;
    gap: 15px;
  }
  
  .form-item .el-input__wrapper,
  .form-select .el-select__wrapper {
    height: 50px;
  }
  
  .generate-button {
    height: 50px;
    font-size: 16px;
  }
  
  .question-bank {
    padding: 30px 20px;
    max-height: 400px;
  }
  
  .bank-header {
    flex-direction: column;
    gap: 15px;
    align-items: stretch;
  }
  
  .bank-search {
    width: 100%;
  }
  
  .card-container {
    height: 350px;
  }
  
  .card-content {
    padding: 40px 20px;
  }
  
  .card-text {
    font-size: 20px;
  }
  
  .cards-header {
    flex-direction: column;
    gap: 20px;
    text-align: center;
  }
  
  .header-left {
    align-items: center;
  }
  
  .card-controls {
    gap: 20px;
  }
  
  .card-controls .el-button {
    width: 120px;
    height: 50px;
    font-size: 16px;
  }
}

/* 选题模式样式 */
.card-checkbox {
  position: absolute;
  top: 30px;
  right: 30px;
  z-index: 10;
  transform: scale(1.5);
}

.card-checkbox :deep(.el-checkbox__inner) {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  border: 2px solid rgba(255, 255, 255, 0.8);
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
}

.card-checkbox :deep(.el-checkbox__inner::after) {
  width: 10px;
  height: 18px;
  left: 5px;
  top: 1px;
  border-width: 3px;
}

.card-checkbox :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  border-color: #4facfe;
}

/* 加载更多按钮样式 */
.load-more-container {
  text-align: center;
  margin: 30px 0;
}

.load-more-container .el-button {
  min-width: 200px;
  height: 48px;
  font-size: 16px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.5);
  color: #fff;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
}

.load-more-container .el-button:hover {
  background: rgba(255, 255, 255, 0.5);
  border-color: rgba(255, 255, 255, 0.8);
  transform: translateY(-2px);
}

/* 题库选择对话框样式 */
.bank-selection-dialog {
  max-height: 500px;
  overflow-y: auto;
  padding: 10px;
}

.create-bank-button-wrapper {
  margin-bottom: 20px;
}

.create-bank-button-wrapper .el-button {
  width: 100%;
  height: 44px;
  font-size: 15px;
  border-radius: 8px;
}

.bank-search-input {
  margin-bottom: 20px;
}

.bank-search-input :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.bank-loading,
.bank-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 60px 20px;
  color: #909399;
  font-size: 14px;
}

.bank-loading .el-icon,
.bank-empty .el-icon {
  font-size: 48px;
}

.bank-loading .el-icon {
  color: #409eff;
}

.bank-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;
}

.bank-category {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bank-category-title {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
  padding: 8px 12px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.bank-radio-item {
  width: 100%;
  margin: 0;
  padding: 20px;
  border: 2px solid #e0e0e0;
  border-radius: 12px;
  transition: all 0.3s ease;
  cursor: pointer;
  position: relative;
  display: flex;
  align-items: center;
  gap: 20px;
}

.bank-radio-item:hover {
  border-color: #4facfe;
  background: rgba(79, 172, 254, 0.05);
}

.bank-radio-item.is-selected {
  border-color: #4facfe;
  background: rgba(79, 172, 254, 0.08);
}

.radio-button {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.radio-inner {
  width: 18px;
  height: 18px;
  border: 2px solid #dcdfe6;
  border-radius: 50%;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.radio-inner:hover {
  border-color: #4facfe;
}

.radio-inner.is-checked {
  border-color: #4facfe;
  background: white;
}

.radio-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.bank-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.bank-name {
  font-size: 17px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.bank-meta {
  display: flex;
  gap: 10px;
  font-size: 13px;
  color: #666;
  flex-wrap: wrap;
}

.bank-meta span {
  padding: 6px 14px;
  background: rgba(79, 172, 254, 0.1);
  border-radius: 14px;
  font-weight: 500;
}

/* 对话框底部按钮样式 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.dialog-footer .el-button {
  min-width: 100px;
}

/* 创建题库表单样式 */
.create-bank-form {
  padding: 10px 20px;
}

.create-bank-form .el-form-item {
  margin-bottom: 24px;
}

.create-bank-form .el-input,
.create-bank-form .el-textarea {
  width: 100%;
}

/* 加载状态样式 */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  gap: 20px;
}

.loading-icon {
  color: #4facfe;
  animation: rotate 1.5s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.loading-text {
  font-size: 16px;
  color: #606266;
  font-weight: 500;
}

/* 思考过程样式 */
.thinking-process {
  margin-top: 25px;
  padding: 25px 30px;
  background: linear-gradient(135deg, rgba(79, 172, 254, 0.12) 0%, rgba(0, 242, 254, 0.08) 100%);
  border-left: 5px solid #4facfe;
  border-radius: 16px;
  max-width: 1000px;
  width: 100%;
  margin: 0 auto;
  box-shadow: 0 6px 20px rgba(79, 172, 254, 0.15);
  min-height: 180px;
  box-sizing: border-box;
}

.thinking-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 15px;
  color: #4facfe;
  font-weight: 600;
  font-size: 17px;
}

.thinking-header .el-icon {
  font-size: 22px;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.7;
    transform: scale(1.1);
  }
}

.thinking-content {
  font-size: 15px;
  line-height: 1.8;
  color: #303133;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  letter-spacing: 0.3px;
  max-height: 450px;
  overflow-y: auto;
  padding: 5px;
}

.typing-cursor {
  display: inline-block;
  margin-left: 2px;
  animation: blink 1s step-end infinite;
  color: #4facfe;
  font-weight: bold;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* 分页容器样式 */
.pagination-container {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 30px;
  padding: 20px 0;
}

.pagination-container .el-pagination {
  padding: 0;
}

.pagination-container .el-pagination button,
.pagination-container .el-pagination .el-pager li {
  background-color: white;
  border-radius: 8px;
  margin: 0 4px;
  transition: all 0.3s ease;
}

.pagination-container .el-pagination button:hover,
.pagination-container .el-pagination .el-pager li:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.pagination-container .el-pagination .el-pager li.is-active {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: white;
  font-weight: 600;
}
</style>