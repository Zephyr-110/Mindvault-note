# ============================================
# 阶段一：Node.js 构建前端
# ============================================
FROM node:20-alpine AS builder

WORKDIR /build

# 先复制依赖文件，利用缓存
COPY package.json ./
RUN npm install

# 复制源码并构建
COPY . .
RUN npm run build

# ============================================
# 阶段二：Nginx 提供静态服务
# ============================================
FROM nginx:alpine

# 用我们自己的 nginx 配置替换默认配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 从构建阶段复制打包好的静态文件
COPY --from=builder /build/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]