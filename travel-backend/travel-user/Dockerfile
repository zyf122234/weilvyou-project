ARG SKYWALKING_AGENT_IMAGE
ARG JAVA_RUNTIME_IMAGE
FROM ${SKYWALKING_AGENT_IMAGE} AS skywalking-agent
FROM ${JAVA_RUNTIME_IMAGE}
ARG TRAVEL_USER_PORT

RUN apt-get update && apt-get install -y openjdk-17-jdk && rm -rf /var/lib/apt/lists/*

WORKDIR /app

ENV LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"

COPY --from=skywalking-agent /skywalking/agent /skywalking/agent
COPY target/travel-user-1.0.0.jar app.jar

EXPOSE ${TRAVEL_USER_PORT}

ENTRYPOINT ["java", "-javaagent:/skywalking/agent/skywalking-agent.jar", "-jar", "app.jar"]
