FROM node:24.16.0-alpine

WORKDIR /app

COPY gis-framework/arcgis-node/package*.json .

RUN npm install

COPY gis-framework/src/main/proto /src/main/proto

COPY gis-framework/arcgis-node/ ./
RUN npm run copy:core && npm run proto-gen

EXPOSE 8082 3000

ENTRYPOINT ["npx", "tsx", "src/grpc-server.ts"]
