# 复用官方 Node 镜像；前端先由构建脚本产出 dist，再打包静态服务。
FROM node:22-bookworm
WORKDIR /app
COPY ydl-ui/dist ./dist
COPY deploy/server.cjs ./server.cjs
EXPOSE 8080
CMD ["node", "server.cjs"]
