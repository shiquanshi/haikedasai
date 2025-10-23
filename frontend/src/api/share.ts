import request from './request'

// 分享相关接口
export interface CreateShareParams {
  bankId: number
  isPublic: boolean
  shareTitle?: string
  shareDescription?: string
}

export interface SharedBank {
  id: number
  bankId: number
  userId: number
  shareCode: string
  isPublic: boolean
  shareTitle?: string
  shareDescription?: string
  viewCount: number
  uniqueViewCount: number
  favoriteCount: number
  copyCount: number
  status: number
  createdAt: string
  updatedAt?: string
  expireAt?: string
}

export interface PlazaItem {
  id: number
  bankId: number
  shareCode: string
  shareTitle?: string
  shareDescription?: string
  viewCount: number
  uniqueViewCount: number
  favoriteCount: number
  copyCount: number
  bankName: string
  bankDescription?: string
  topic?: string
  difficulty?: string
  language?: string
  cardCount: number
  tags?: string
  creatorName: string
  creatorAvatar?: string
  createdAt: string
}

export interface PlazaResponse {
  list: PlazaItem[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

// 创建或更新分享
export const createShare = (params: CreateShareParams) => {
  return request.post<SharedBank>('/api/share/create', params)
}

// 获取分享大厅列表
export const getPlaza = (params: {
  keyword?: string
  orderBy?: string
  page?: number
  pageSize?: number
}) => {
  return request.get<PlazaResponse>('/api/share/plaza', { params })
}

// 根据分享码获取详情
export const getShareDetail = (shareCode: string) => {
  return request.get<SharedBank>(`/api/share/detail/${shareCode}`)
}

// 获取浏览统计
export const getStatistics = (sharedBankId: number) => {
  return request.get<any>(`/api/share/statistics/${sharedBankId}`)
}

// 按日期获取浏览统计
export const getStatisticsByDate = (sharedBankId: number, startDate: string, endDate: string) => {
  return request.get<any>(`/api/share/statistics/${sharedBankId}/by-date`, {
    params: { startDate, endDate }
  })
}

// 获取我的分享列表
export const getMyShares = () => {
  return request.get<any[]>('/api/share/my-shares')
}

// 更新分享
export const updateShare = (params: CreateShareParams & { id: number }) => {
  return request.post<SharedBank>('/api/share/create', params)
}

// 删除分享
export const deleteShare = (shareId: number) => {
  return request.delete<string>(`/api/share/delete/${shareId}`)
}

// 增加导入/复制次数
export const incrementCopyCount = (shareCode: string) => {
  return request.post<void>(`/api/share/increment-copy/${shareCode}`)
}