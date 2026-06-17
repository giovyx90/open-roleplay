# Frontend statico — build React servito da nginx. In produzione meglio su CDN
# (Vercel/Netlify/Cloudflare Pages); questa immagine è per chi tiene tutto sul VPS.
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm install --no-audit --no-fund
COPY . .
# L'URL dell'API è risolto a build time da Vite.
ARG VITE_API_URL=http://localhost:3000
ENV VITE_API_URL=$VITE_API_URL
RUN npm run build

FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
