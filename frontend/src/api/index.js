import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 响应拦截器 - 直接返回后端返回的 data，401 时跳转登录
api.interceptors.response.use(
    response => response.data,
    error => {
      if (error.response && error.response.status === 401) {
        localStorage.removeItem('userInfo')
        window.location.href = '/login'
      }
      return Promise.reject(error)
    }
)

// 请求拦截器 - 自动添加 JWT Authorization 请求头
api.interceptors.request.use(config => {
  const userInfo = localStorage.getItem('userInfo')
  if (userInfo) {
    const user = JSON.parse(userInfo)
    if (user.token) {
      config.headers.Authorization = `Bearer ${user.token}`
    }
  }
  return config
})

export default {
  // 用户
  login: (data) => api.post('/user/login', data),
  register: (data) => api.post('/user/register', data),
  changePassword: (data) => api.put('/user/changePassword', null, { params: data }),
  changeUsername: (data) => api.put('/user/changeUsername', null, { params: data }),
  logout: (data) => api.post('/user/logout', data),

  // 目录
  createCategory: (data) => api.post('/category/create', data),
  listCategory: (data) => api.get('/category/list', { params: data }),
  updateCategory: (data) => api.put('/category/update', null, { params: data }),
  deleteCategory: (data) => api.delete('/category/delete', { params: data }),
  restoreCategory: (data) => api.put('/category/restore', null, { params: data }),
  getCategoryTree: () => api.get('/category/tree'),
  listTrashCategories: () => api.get('/category/trash'),

  // 文档
  createDocument: (data) => api.post('/document/create', data),
  getDocumentDetail: (data) => api.get('/document/detail', { params: data }),
  listDocuments: (data) => api.get('/document/list', {
    params: { ...data, page: data.page || 1, size: data.size || 10 }
  }),
  updateDocument: (data) => api.put('/document/update', null, { params: data }),
  deleteDocument: (data) => api.delete('/document/delete', { params: data }),
  restoreDocument: (data) => api.put('/document/restore', null, { params: data }),
  listTrashDocuments: () => api.get('/document/trash'),

  // 标签
  createTag: (data) => api.post('/tag/create', data),
  listTags: () => api.get('/tag/list'),
  deleteTag: (data) => api.delete('/tag/delete', { params: data }),

  // 文件上传
  uploadFile: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/file/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  // 个人资料
  getUserProfile: () => api.get('/user/profile/get'),
  getUserProfileById: (userId) => api.get('/user/profile/getById', { params: { userId } }),
  updateUserProfile: (data) => api.put('/user/profile/update', data),

  // ========== 社区 ==========
  // 帖子
  createPost: (data) => api.post('/community/post/create', data),
  getPostDetail: (data) => api.get('/community/post/detail', { params: data }),
  deletePost: (data) => api.delete('/community/post/delete', { params: data }),
  getFeed: (data) => api.get('/community/post/feed', { params: data }),

  // 评论
  createComment: (data) => api.post('/community/comment/create', data),
  deleteComment: (data) => api.delete('/community/comment/delete', { params: data }),
  listComments: (data) => api.get('/community/comment/list', { params: data }),

  // 点赞
  toggleLike: (data) => api.post('/community/like-record/toggle', data),
  getLikeCount: (data) => api.get('/community/like-record/count', { params: data }),

  // 收藏
  toggleFavorite: (data) => api.post('/community/favorite/toggle', data),
  getFavoriteCount: (data) => api.get('/community/favorite/count', { params: data }),
  listFavorites: (data) => api.get('/community/favorite/list', { params: data }),

  // 关注
  toggleFollow: (data) => api.post('/community/follow/toggle', data),
  listFollowee: (data) => api.get('/community/follow/listFollowee', { params: data }),
  listFollower: (data) => api.get('/community/follow/listFollower', { params: data }),

  // 消息
  sendMessage: (data) => api.post('/community/message/send', data),
  listConversations: (data) => api.get('/community/message/list-conversations', { params: data }),
  getMessageHistory: (data) => api.get('/community/message/history', { params: data }),
  markMessageRead: (data) => api.put('/community/message/mark-read', data),
  getUnreadMessageCount: () => api.get('/community/message/unread-count'),

  // 通知
  listNotifications: (data) => api.get('/community/notification/list', { params: data }),
  readNotification: (data) => api.get('/community/notification/read', { params: data }),
  getUnreadNotificationCount: () => api.get('/community/notification/unreadCount'),

  // 设置
  getSettings: (data) => api.get('/user/setting/get', { params: data }),
  updateSetting: (data) => api.put('/user/setting/update', null, { params: data }),

  // 黑名单
  blockUser: (data) => api.post('/user-block/block', data),
  unblockUser: (data) => api.delete('/user-block/unblock', { params: data }),
  listBlockedUsers: (data) => api.get('/user-block/list', { params: data }),
  isBlocked: (data) => api.get('/user-block/is-blocked', { params: data })
}