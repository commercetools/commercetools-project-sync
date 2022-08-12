FROM adoptopenjdk/openjdk11:jre
LABEL maintainer="PS Team Munich [ps-dev@commercetools.com]"
WORKDIR /app
COPY ./build/libs libs/
COPY ./build/resources resources/
COPY ./build/classes classes/
ENTRYPOINT ["java", "-cp", "/app/resources:/app/classes:/app/libs/*", "com.commercetools.project.sync.SyncerApplication"]
