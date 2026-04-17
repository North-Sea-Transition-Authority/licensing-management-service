FROM node:22-alpine

WORKDIR /app

COPY gis-framework/arcgis-node/package*.json .

RUN npm install

COPY src/main/proto /src/main/proto

COPY gis-framework/arcgis-node/ ./
RUN npm run copy:core && npm run proto-gen

EXPOSE 8082 3000

ENTRYPOINT exec npx tsx grpc-server.ts
