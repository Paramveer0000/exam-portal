# ============================= build stage =============================
FROM node:18-alpine AS build
WORKDIR /app
ENV NODE_ENV=production

# Install deps first (leverages layer caching). --legacy-peer-deps is required
# by this project's peer-dependency graph. npm cache is cleaned in the same
# layer so it never ends up in an image layer.
COPY package.json package-lock.json ./
RUN npm ci --legacy-peer-deps && npm cache clean --force

# Build the static production bundle (CRA also sets NODE_ENV=production itself
# during `npm run build`; the ENV above covers any other tooling that reads it).
COPY . .
RUN npm run build

# ============================= run stage ===============================
# Unprivileged nginx image: the master process itself runs as a non-root
# user (not just the workers), listening on 8080 instead of 80.
FROM nginxinc/nginx-unprivileged:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/build /usr/share/nginx/html

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/ >/dev/null 2>&1 || exit 1
