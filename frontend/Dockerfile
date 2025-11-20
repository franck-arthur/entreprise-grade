# Multi-stage Dockerfile for Angular frontend

# Build stage
FROM node:20-alpine AS build
WORKDIR /app

# Disable Husky git hooks during Docker build
ENV HUSKY=0

# Copy package files and install dependencies (cached layer)
COPY package*.json ./
RUN npm ci --legacy-peer-deps

# Copy source code and build
COPY . .
RUN npm run build:prod

# Runtime stage with Nginx
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Remove default nginx website
RUN rm -rf ./*

# Copy built application
COPY --from=build /app/dist/enterprise-app-frontend/browser .

# Copy nginx configuration
COPY nginx.conf /etc/nginx/nginx.conf

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1

# Expose port
EXPOSE 80

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
