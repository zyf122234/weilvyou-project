ARG SKYWALKING_AGENT_IMAGE
ARG JAVA_RUNTIME_IMAGE
FROM ${SKYWALKING_AGENT_IMAGE} AS skywalking-agent
FROM ${JAVA_RUNTIME_IMAGE}
ARG TRAVEL_AI_PORT

RUN apt-get update && apt-get install -y openjdk-17-jdk && rm -rf /var/lib/apt/lists/*

WORKDIR /app

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:MaxMetaspaceSize=256m -XX:+UseContainerSupport -XX:+ExitOnOutOfMemoryError"

COPY --from=skywalking-agent /skywalking/agent /skywalking/agent
COPY target/travel-ai-1.0.0.jar app.jar

EXPOSE ${TRAVEL_AI_PORT}

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -javaagent:/skywalking/agent/skywalking-agent.jar -jar app.jar"]
