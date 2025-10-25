import request from './request'

export interface ImageGenerateParams {
  prompt: string
  n?: number
  size?: string
}

export const imageApi = {
  /**
   * 生成图片
   */
  generateImage(params: ImageGenerateParams) {
    return request({
      url: '/image/generate',
      method: 'post',
      data: params
    })
  }
}