FROM java:openjdk-8-jre-alpine
LABEL maintainer="PS Team Munich [ps-dev@commercetools.com]"
WORKDIR /app
COPY libs libs/
COPY resources resources/
COPY classes classes/
ENTRYPOINT ["java", "-cp", "/app/resources:/app/classes:/app/libs/*", "com.commercetools.project.sync.SyncerApplication"]
