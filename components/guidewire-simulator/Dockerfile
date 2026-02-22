# Stage 1: Build
FROM docker.io/library/node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npx ng build --configuration=production

# Stage 2: Serve with nginx
FROM docker.io/library/nginx:1.27-alpine
LABEL maintainer="guidewire-poc"
LABEL description="Guidewire Simulator - Angular Frontend"

# Remove default nginx content
RUN rm -rf /usr/share/nginx/html/*

# Copy built Angular app
COPY --from=build /app/dist/guidewire-simulator/browser /usr/share/nginx/html

# Custom nginx config for SPA routing
COPY nginx.conf /etc/nginx/conf.d/default.conf

# OpenShift runs with random UID - ensure proper permissions
RUN chmod -R g+rwX /usr/share/nginx/html && \
    chgrp -R 0 /usr/share/nginx/html && \
    chmod -R g+rwX /var/cache/nginx && \
    chgrp -R 0 /var/cache/nginx && \
    chmod -R g+rwX /var/log/nginx && \
    chgrp -R 0 /var/log/nginx && \
    chmod -R g+rwX /etc/nginx/conf.d && \
    chgrp -R 0 /etc/nginx/conf.d && \
    # nginx.pid needs to be writable
    touch /var/run/nginx.pid && \
    chmod g+rw /var/run/nginx.pid && \
    chgrp 0 /var/run/nginx.pid

EXPOSE 8080

CMD ["nginx", "-g", "daemon off;"]
