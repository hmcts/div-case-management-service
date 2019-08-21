ARG APP_INSIGHTS_AGENT_VERSION=2.3.1
FROM hmctspublic.azurecr.io/base/java:openjdk-8-distroless-1.0

ENV APP div-case-maintenance-service.jar

COPY lib/applicationinsights-agent-2.3.1.jar lib/AI-Agent.xml /opt/app/
COPY build/libs/$APP /opt/app/

CMD ["div-case-maintenance-service.jar"]
