FROM adoptopenjdk/openjdk11-openj9:jdk-11.0.1.13-alpine-slim as build
WORKDIR /code
COPY . .
RUN ./gradlew build -x test


FROM adoptopenjdk/openjdk11-openj9:jre-11.0.4_11_openj9-0.15.1-alpine
COPY --from=build /code/app/build/libs/app*-all.jar app.jar
EXPOSE 8080
CMD java -Dcom.sun.management.jmxremote -noverify ${JAVA_OPTS} -jar app.jar