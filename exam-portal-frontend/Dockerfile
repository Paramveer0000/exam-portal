# ============================= build stage =============================
FROM node:18-alpine AS build
WORKDIR /app

# Install deps first (leverages layer caching). --legacy-peer-deps is required
# by this project's peer-dependency graph.
COPY package.json package-lock.json ./
RUN npm ci --legacy-peer-deps

# Build the static production bundle (CRA sets NODE_ENV=production itself).
COPY . .
RUN npm run build

# ============================= run stage ===============================
FROM nginx:1.27-alpine

# SPA + /api reverse-proxy config.
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/build /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost:80/ >/dev/null 2>&1 || exit 1
