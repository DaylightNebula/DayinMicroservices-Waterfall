FROM openjdk:19-alpine
ADD https://api.papermc.io/v2/projects/waterfall/versions/1.20/builds/536/downloads/waterfall-1.20-536.jar /opt/waterfall/waterfall.jar
WORKDIR /opt/waterfall/
EXPOSE 25565
CMD [ "java", "-jar", "waterfall.jar" ]