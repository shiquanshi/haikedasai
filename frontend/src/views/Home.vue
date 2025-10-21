<template>
  <div class="home-container">
    <!-- 顶部波浪装饰 -->
    <div class="wave-decoration"></div>
    
    <!-- 头部区域 -->
    <div class="header">
      <div class="header-actions">
        <el-button 
          v-if="!userStore.isLoggedIn"
          type="primary" 
          @click="$router.push('/login')"
          size="large"
          round
        >
          登录
        </el-button>
        <div class="user-info" v-if="userStore.isLoggedIn">
          <span class="username">{{ userStore.userInfo?.username }}</span>
          <el-button size="small" @click="handleLogout" type="danger" round>退出</el-button>
        </div>
      </div>
    </div>

    <!-- 主要内容区，包含两侧 -->
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
                      <el-button
                        type="danger"
                        size="small"
                        :icon="Delete"
                        @click.stop="handleDeleteBank(bank.id, bank.name)"
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
              
              <div v-if="!isLoadingBanks && systemBanks.length === 0" class="no-data">
                暂无推荐题库
              </div>
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
                        type="danger"
                        size="small"
                        :icon="Delete"
                        @click.stop="handleDeleteBank(bank.id)"
                        circle
                        title="删除题库"
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
              
              <div v-if="!isLoadingCustomBanks && customBanks.length === 0" class="no-data">
              暂无自定义题库
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
                    <el-tag size="small" type="info">{{ record.createTime }}</el-tag>
                  </div>
                </div>
              </template>
              <div class="bank-meta">
                <span class="bank-difficulty">{{ record.difficulty }}</span>
                <span class="bank-language">{{ record.language }}</span>
              </div>
            </el-card>
            
            <!-- 加载更多按钮 -->
            <div v-if="userStore.isLoggedIn && historyRecords.length > 0 && hasMoreHistory" class="load-more-container">
              <el-button 
                type="default" 
                @click="handleLoadMoreHistory"
                :loading="isLoadingHistory"
                size="large"
              >
                加载更多
              </el-button>
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
                @click="showImportDialog = true"
                class="import-bank-button"
                :icon="Upload"
                round
                style="margin-left: 10px"
              >
                从Excel导入
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
            <div class="loading-text">正在生成闪卡...</div>
          </div>
          
          <!-- 正常卡片展示 -->
          <template v-else-if="cards.length > 0">
            <div 
              v-if="isSelectionMode"
              class="card-checkbox"
              @click.stop="toggleCardSelection"
            >
              <div class="checkbox-box" :class="{ checked: isCurrentCardSelected }">
                <span v-if="isCurrentCardSelected" class="checkmark">✓</span>
              </div>
            </div>
            <div class="flip-card" :class="{ flipped: isFlipped }" @click="flipCard">
            <div class="flip-card-inner">
              <div class="flip-card-front">
                <div class="card-content">
                  <div class="card-label">问题</div>
                  <div class="card-text">{{ currentCard.question }}</div>
                  <div v-if="currentCard.questionImage" class="card-image">
                    <el-image :src="currentCard.questionImage" fit="cover" class="card-img">
                      <template #error>
                        <div class="image-slot">加载失败</div>
                      </template>
                    </el-image>
                  </div>
                  <div class="card-actions">
                    <el-button 
                      @click.stop="playQuestion" 
                      :loading="isPlayingQuestion"
                      circle
                      size="large"
                      class="play-button"
                      title="播放问题"
                    >
                      <template #icon v-if="!isPlayingQuestion">
                        <svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor">
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
                  <div class="card-text">{{ currentCard.answer }}</div>
                  <div v-if="currentCard.answerImage" class="card-image">
                    <el-image :src="currentCard.answerImage" fit="cover" class="card-img">
                      <template #error>
                        <div class="image-slot">加载失败</div>
                      </template>
                    </el-image>
                  </div>
                  <div class="card-actions">
                    <el-button 
                      @click.stop="playQuestion" 
                      :loading="isPlayingQuestion"
                      circle
                      size="large"
                      class="play-button"
                      title="播放答案"
                    >
                      <template #icon v-if="!isPlayingQuestion">
                        <svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor">
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
          </template>
        </div>

        <div class="card-controls">
          <el-button 
            @click="previousCard" 
            :disabled="currentCardIndex === 0"
            size="large"
          >
            上一题
          </el-button>
          <el-button 
            @click="handleDeleteCurrentCard" 
            type="danger"
            size="large"
            v-if="currentBank && currentBank.type === 'custom'"
            :disabled="cards.length <= 1"
            title="删除当前卡片"
          >
            <el-icon><Delete /></el-icon>
            删除卡片
          </el-button>
          <el-button 
            @click="nextCard" 
            :disabled="currentCardIndex === cards.length - 1"
            type="primary"
            size="large"
          >
            下一题
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
        <div v-if="isLoadingBanks || isLoadingCustomBanks" class="bank-loading">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>加载题库中...</span>
        </div>
        <div v-else-if="systemBanks.length === 0 && customBanks.length === 0" class="bank-empty">
          <el-icon><Search /></el-icon>
          <span>{{ bankSearchText ? '未找到匹配的题库' : '暂无题库，请先创建' }}</span>
        </div>
        <div v-else class="bank-list">
          <!-- 系统推荐题库 -->
          <div v-if="systemBanks.length > 0" class="bank-category">
            <div class="bank-category-title">系统推荐题库</div>
            <div
              v-for="bank in systemBanks"
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
          <div v-if="customBanks.length > 0" class="bank-category">
            <div class="bank-category-title">我的题库</div>
            <div
              v-for="bank in customBanks"
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Star, VideoPlay, Search, Loading, Plus, Download, Upload, Edit, Delete } from '@element-plus/icons-vue'
import { questionBankApi } from '../api/questionBank'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

interface Card {
  id: number
  question: string
  answer: string
}

interface QuestionBank {
  id: number
  name: string
  description: string
  cardCount: number
  difficulty: string
  language: string
  createTime: string
}

const topic = ref('')
const cardCount = ref('10')
const difficulty = ref('medium')
const language = ref('中文')  // 默认中文，与el-select的选项值保持一致
const withImages = ref(false)
const showCards = ref(false)
const cards = ref<Card[]>([])
const currentCardIndex = ref(0)
const isFlipped = ref(false)
const isPlayingQuestion = ref(false)
let audioElement: HTMLAudioElement | null = null

// 题库相关
const systemBanks = ref<QuestionBank[]>([])  // 系统推荐题库
const customBanks = ref<QuestionBank[]>([])  // 用户自定义题库
const isLoadingBanks = ref(false)  // 系统题库加载状态
const isLoadingCustomBanks = ref(false)  // 自定义题库加载状态
const bankSearchText = ref('')
const currentBankId = ref<number | null>(null) // 当前正在查看的题库ID

// 流式生成相关
const isGenerating = ref(false)
const streamContent = ref('')
let streamEventSource: EventSource | null = null

// 选题模式相关
const isSelectionMode = ref(false)
const selectedCardIds = ref<number[]>([])
const showBankDialog = ref(false)
const selectedTargetBankId = ref<number | null>(null)

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
  bankName: '',
  description: '',
  difficulty: 'medium',
  language: '中文'
})
const fileList = ref([])
const uploadRef = ref()
const isImporting = ref(false)

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

// 历史生成记录相关
const historyRecords = ref<QuestionBank[]>([])
const isLoadingHistory = ref(false)
const historyPage = ref(1)
const historyPageSize = ref(10)
const hasMoreHistory = ref(true)

const questionImageFileList = ref<any[]>([])
const answerImageFileList = ref<any[]>([])

const currentCard = computed(() => cards.value[currentCardIndex.value] || { question: '', answer: '' })
const isCurrentCardSelected = computed(() => {
  const currentId = cards.value[currentCardIndex.value]?.id
  return selectedCardIds.value.includes(currentId)
})
const currentBank = computed(() => {
  if (!currentBankId.value) return null
  // 从系统题库和自定义题库中查找当前题库
  return [...systemBanks.value, ...customBanks.value].find(bank => bank.id === currentBankId.value) || null
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
    showCards.value = true
    cards.value = [] // 清空现有卡片
    
    // 判断是否为外语内容（非中文），使用批量接口，否则使用流式接口
    const isNonChinese = language.value !== '中文'
    
    if (isNonChinese) {
      // 外语内容：使用批量接口（非流式）
      console.log('[生成模式] 检测到外语内容，使用批量接口')
      const response = await questionBankApi.generateAIBankBatch({
        topic: topic.value,
        cardCount: parseInt(cardCount.value),
        difficulty: difficulty.value,
        language: language.value,
        withImages: withImages.value
      })
      
      console.log('[批量生成] 接收到响应:', response)
      
      if (response.code === 200 && response.data) {
        console.log('[批量生成] 响应数据类型:', typeof response.data)
        console.log('[批量生成] 响应数据内容:', response.data)
        
        try {
          const generatedCards = JSON.parse(response.data)
          console.log('[批量生成] 解析后的卡片数组:', generatedCards)
          
          if (!Array.isArray(generatedCards)) {
            throw new Error('响应数据不是数组格式')
          }
          
          cards.value = generatedCards.map((card: any, index: number) => ({
            ...card,
            id: Date.now() + index
          }))
          console.log(`[批量生成] 成功生成${cards.value.length}张卡片`)
          ElMessage.success(`成功生成 ${cards.value.length} 张卡片`)
        } catch (parseError) {
          console.error('[批量生成] JSON解析失败:', parseError)
          console.error('[批量生成] 原始数据:', response.data)
          throw new Error('卡片数据解析失败')
        }
      } else {
        console.error('[批量生成] 响应错误:', response)
        throw new Error(response.message || '生成失败')
      }
      
      isGenerating.value = false
      return
    }
    
    // 中文内容：使用流式接口
    console.log('[生成模式] 检测到中文内容，使用流式接口')
    // 使用闭包变量记录上次已显示的卡片数量，避免每次回调重置
    let lastCardCount = 0
    
    // 使用流式API生成闪卡
    streamEventSource = questionBankApi.generateAIBankStream(
      {
        topic: topic.value,
        cardCount: parseInt(cardCount.value),
        difficulty: difficulty.value,
        language: language.value,
        withImages: withImages.value
      },
      // onMessage: 接收流式内容,在卡片内部逐字展示
      (content: string) => {
        const timestamp = new Date().toISOString()
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
          
          // 处理图片描述数据
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
                if (parsed.question) card.question = addSmartSpaces(parsed.question)
                if (parsed.answer) card.answer = addSmartSpaces(parsed.answer)
                if (parsed.difficulty) card.difficulty = parsed.difficulty
                if (parsed.questionImage) card.questionImage = parsed.questionImage
                if (parsed.answerImage) card.answerImage = parsed.answerImage
                console.log(`[前端解析] 块${index}完整解析成功`)
              } catch (e) {
                console.log(`[前端解析] 块${index}JSON解析失败，使用正则提取`)
                // 使用正则提取
                const questionMatch = block.match(/"question"\s*:\s*"((?:[^"\\]|\\.*)*)"/)  
                if (questionMatch) {
                  card.question = addSmartSpaces(questionMatch[1].replace(/\\n/g, '\n').replace(/\\"/g, '"'))
                }
                
                const answerMatch = block.match(/"answer"\s*:\s*"((?:[^"\\]|\\.*)*)"/)  
                if (answerMatch) {
                  card.answer = addSmartSpaces(answerMatch[1].replace(/\\n/g, '\n').replace(/\\"/g, '"'))
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
        ElMessage.error(error || '生成闪卡失败，请重试')
      },
      // onComplete: 完成处理
      () => {
        isGenerating.value = false
        if (cards.value.length > 0) {
          ElMessage.success(`闪卡生成成功！共生成${cards.value.length}张卡片`)
          // 刷新历史生成记录列表
          if (userStore.isLoggedIn && userStore.userInfo) {
            loadHistoryRecords()
          }
        } else {
          ElMessage.warning('未能解析生成的卡片，请重试')
        }
      }
    )
  } catch (error) {
    isGenerating.value = false
    ElMessage.error('生成闪卡失败，请重试')
  }
}

// 加载推荐题库
const loadSystemBanks = async () => {
  try {
    isLoadingBanks.value = true
    // 使用bankSearchText作为搜索关键词
    const response = await questionBankApi.getSystemBanks(bankSearchText.value || '')
    systemBanks.value = response.data
  } catch (error) {
    ElMessage.error('加载题库失败')
  } finally {
    isLoadingBanks.value = false
  }
}

// 加载用户自定义题库
const loadCustomBanks = async () => {
  try {
    isLoadingCustomBanks.value = true
    const response = await questionBankApi.getUserCustomBanks()
    customBanks.value = response.data
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
      sortBy: 'create_time', // Changed from 'createTime' to 'create_time' to match database column name
      sortOrder: 'desc',
      userId: userStore.userInfo.id
    })
    
    // 适配实际的API返回格式
    // 根据request.ts的响应拦截器，这里的response已经是response.data了
    const records = response
    
    if (loadMore) {
      historyRecords.value = [...historyRecords.value, ...records]
    } else {
      historyRecords.value = records
    }
    
    // 判断是否还有更多记录
    hasMoreHistory.value = false // 简化处理，实际项目中可以根据返回的总数判断
  } catch (error) {
    console.error('加载历史记录失败:', error)
    ElMessage.error('加载历史记录失败')
  } finally {
    isLoadingHistory.value = false
  }
}

// 加载指定题库的卡片
const loadBankCards = async (bankId: number) => {
  try {
    isLoadingBanks.value = true
    currentBankId.value = bankId // 保存当前题库ID
    const response = await questionBankApi.getBankCards(bankId)
    cards.value = response.data
    showCards.value = true
    ElMessage.success('题库加载成功！')
  } catch (error) {
    ElMessage.error('加载题库卡片失败')
  } finally {
    isLoadingBanks.value = false
  }
}

const flipCard = () => {
  isFlipped.value = !isFlipped.value
}

const nextCard = () => {
  if (currentCardIndex.value < cards.value.length - 1) {
    currentCardIndex.value++
    isFlipped.value = false
  }
}

const previousCard = () => {
  if (currentCardIndex.value > 0) {
    currentCardIndex.value--
    isFlipped.value = false
  }
}

const backToHome = () => {
  showCards.value = false
  currentCardIndex.value = 0
  isFlipped.value = false
  stopAudio()
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
    
    // 创建下载链接
    const url = `http://localhost:8080/api/question-bank/${bankId}/export`
    const link = document.createElement('a')
    link.href = url
    link.download = `${bankName}.xlsx`
    
    // 使用fetch下载文件
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
    
    if (!response.ok) {
      throw new Error('导出失败')
    }
    
    // 创建blob并触发下载
    const blob = await response.blob()
    const downloadUrl = window.URL.createObjectURL(blob)
    link.href = downloadUrl
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
const toggleCardSelection = () => {
  const currentId = cards.value[currentCardIndex.value]?.id
  if (!currentId) return
  
  const index = selectedCardIds.value.indexOf(currentId)
  if (index > -1) {
    selectedCardIds.value.splice(index, 1)
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
  // 打开对话框前加载所有题库
  await loadSystemBanks()
  await loadCustomBanks()
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
    
    // 刷新题库列表
    await loadSystemBanks()
    await loadCustomBanks()
    
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

// 导入题库
const handleImport = async () => {
  if (fileList.value.length === 0) {
    ElMessage.warning('请选择要导入的Excel文件')
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
    if (importForm.value.bankName) {
      formData.append('bankName', importForm.value.bankName)
    }
    if (importForm.value.description) {
      formData.append('description', importForm.value.description)
    }
    formData.append('difficulty', importForm.value.difficulty)
    formData.append('language', importForm.value.language)

    const response = await questionBankApi.importBankFromExcel(formData)
    
    if (response.success) {
      ElMessage({
        message: '导入成功！',
        type: 'success',
        showClose: false,
        duration: 2000
      })
      showImportDialog.value = false
      // 重置表单
      importForm.value = {
        bankName: '',
        description: '',
        difficulty: 'medium',
        language: '中文'
      }
      fileList.value = []
      // 刷新题库列表
      await loadCustomBanks()
      await loadSystemBanks()
    } else {
      ElMessage.error(response.message || '导入失败')
    }
  } catch (error: any) {
    console.error('导入失败:', error)
    ElMessage.error(error.message || '导入失败，请重试')
  } finally {
    isImporting.value = false
  }
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

    const response = await questionBankApi.deleteBank(bankId)
    
    if (response.success) {
      ElMessage.success('删除成功！')
      // 刷新题库列表
      await loadCustomBanks()
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error(error.message || '删除失败，请重试')
    }
  }
}

const handleDeleteCurrentCard = async () => {
  if (!currentCard.value || !currentCard.value.id) {
    ElMessage.warning('无效的卡片')
    return
  }

  try {
    await ElMessageBox.confirm(
      '确定要删除这张卡片吗？删除后将无法恢复。',
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const cardId = currentCard.value.id
    const response = await questionBankApi.deleteCard(cardId)
    
    ElMessage.success('删除成功！')
    
    // 从卡片列表中移除当前卡片
    cards.value = cards.value.filter(card => card.id !== cardId)
    
    // 刷新题库列表
    await loadSystemBanks()
    await loadCustomBanks()
    
    // 如果删除后还有卡片，调整当前卡片索引
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
      console.error('删除卡片失败:', error)
      ElMessage.error(error.message || '删除卡片失败，请重试')
    }
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

const playQuestion = async () => {
  if (isPlayingQuestion.value) {
    stopAudio()
    return
  }

  try {
    isPlayingQuestion.value = true
    const response = await questionBankApi.textToSpeech(currentCard.value.answer)
    
    if (response.success && response.audioData) {
      // 创建音频元素并播放
      const audioData = response.audioData
      const audioBlob = base64ToBlob(audioData, 'audio/mpeg')
      const audioUrl = URL.createObjectURL(audioBlob)
      
      stopAudio() // 停止之前的播放
      audioElement = new Audio(audioUrl)
      
      audioElement.onended = () => {
        isPlayingQuestion.value = false
        URL.revokeObjectURL(audioUrl)
      }
      
      audioElement.onerror = () => {
        isPlayingQuestion.value = false
        ElMessage.error('音频播放失败')
        URL.revokeObjectURL(audioUrl)
      }
      
      await audioElement.play()
    } else {
      throw new Error('获取音频数据失败')
    }
  } catch (error) {
    isPlayingQuestion.value = false
    ElMessage.error('播放失败')
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
let searchTimer: NodeJS.Timeout | null = null
watch(bankSearchText, () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    loadSystemBanks()
    loadCustomBanks()
  }, 300)
})

// 加载更多历史记录
const handleLoadMoreHistory = async () => {
  if (isLoadingHistory.value || !hasMoreHistory.value) return
  historyPage.value++
  await loadHistoryRecords(historyPage.value, true)
}

// 页面加载时获取题库列表
onMounted(() => {
  loadSystemBanks()
  loadCustomBanks()
  
  // 如果用户已登录，加载历史生成记录
  if (userStore.isLoggedIn) {
    // 尝试获取用户信息
    userStore.fetchUserInfo().then(() => {
      loadHistoryRecords()
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
  padding: 40px 20px;
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
  max-width: 1200px;
  display: flex;
  gap: 40px;
  position: relative;
  z-index: 1;
}

.content-section {
  flex: 1;
}

/* 左侧区域样式 */
.left-section {
  flex: 1.5;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

/* 右侧区域样式 */
.right-section {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

/* 波浪装饰 */
.wave-decoration {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 40px;
  background: url("data:image/svg+xml,%3Csvg width='100%25' height='100%25' xmlns='http://www.w3.org/2000/svg'%3E%3Cdefs%3E%3ClinearGradient id='a' x1='0' y1='0' x2='0' y2='1' gradientTransform='rotate(-45 0.5 0.5)'%3E%3Cstop offset='0' stop-color='%23ffffff' stop-opacity='0.1'/%3E%3Cstop offset='1' stop-color='%23ffffff' stop-opacity='0'/%3E%3C/linearGradient%3E%3C/defs%3E%3Cpath fill='url(%23a)' d='M0 0h100v100H0z'/%3E%3C/svg%3E");
  background-size: cover;
  background-position: center;
}

/* 头部样式 */
.header {
  text-align: center;
  margin-bottom: 15px;
  color: white;
  position: relative;
  z-index: 1;
}



.header h1 {
  font-size: 48px;
  margin-bottom: 16px;
  text-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  font-weight: 700;
}

.header p {
  font-size: 20px;
}

.header-actions {
  position: fixed;
  top: 10px;
  right: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
  z-index: 1000;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.95);
  padding: 8px 16px;
  border-radius: 20px;
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
  margin-bottom: 50px;
  height: 900px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
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
  font-size: 18px;
  line-height: 1.5;
  margin-bottom: 10px;
  font-weight: 500;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  overflow-y: auto;
  padding: 15px 10px;
  max-height: 150px;
  word-wrap: break-word;
  word-break: normal;
  white-space: normal;
  overflow-wrap: break-word;
  hyphens: auto;
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
  aspect-ratio: 1 / 1;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
}

.card-img {
  max-width: 100%;
  max-height: 100%;
  width: auto;
  height: auto;
  border-radius: 10px;
  object-fit: contain;
}

.image-slot {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100px;
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
    flex-direction: column;
    gap: 30px;
  }
  
  .content-section {
    width: 100%;
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
</style>