/**
 * 检测当前设备是否为移动设备
 * @returns {boolean} 如果是移动设备返回 true,否则返回 false
 */
export function isMobileDevice(): boolean {
  // 检查 navigator.userAgent
  const userAgent = navigator.userAgent || navigator.vendor || (window as any).opera
  
  // 移动设备的正则表达式
  const mobileRegex = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini|Mobile|mobile|CriOS/i
  
  // 检查是否匹配移动设备
  if (mobileRegex.test(userAgent)) {
    return true
  }
  
  // 检查屏幕宽度(作为备用判断)
  const screenWidth = window.innerWidth || document.documentElement.clientWidth || document.body.clientWidth
  
  // 如果屏幕宽度小于等于768px,也认为是移动设备
  if (screenWidth <= 768) {
    return true
  }
  
  // 检查触摸支持(作为辅助判断)
  const hasTouch = 'ontouchstart' in window || navigator.maxTouchPoints > 0
  
  // 如果有触摸支持且屏幕较小,认为是移动设备
  if (hasTouch && screenWidth <= 1024) {
    return true
  }
  
  return false
}

/**
 * 获取设备类型
 * @returns {'mobile' | 'desktop'} 设备类型
 */
export function getDeviceType(): 'mobile' | 'desktop' {
  return isMobileDevice() ? 'mobile' : 'desktop'
}