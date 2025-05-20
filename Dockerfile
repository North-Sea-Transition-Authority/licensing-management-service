FROM eclipse-temurin:21-alpine
COPY ./build/libs/xyz.jar app.jar

RUN apk update && apk upgrade && apk add curl && rm -rf /var/cache/apk/*

ENV TZONE="Europe/London"
RUN apk add --update tzdata \
&& echo "${TZONE}" > /etc/timezone \
&& ln -sf /usr/share/zoneinfo/${TZONE} /etc/localtime

ENV SPRING_PROFILES_ACTIVE=production

ENTRYPOINT exec java $JAVA_OPTS -jar app.jar