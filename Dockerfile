FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/ecosolicitud-0.0.1-SNAPSHOT.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
# CDS: el jar se extrae y una corrida de entrenamiento (arranca el contexto con
# H2 en memoria y sale en onRefresh, sin tocar la base real) deja las clases
# precargadas en app.jsa; cada arranque en frío las lee en vez de cargarlas
RUN java -Djarmode=tools -jar app.jar extract --destination extraida && rm app.jar
WORKDIR /app/extraida
RUN DATABASE_URL=jdbc:h2:mem:cds DATABASE_USER=sa DATABASE_PASSWORD=sa \
    java -XX:ArchiveClassesAtExit=app.jsa -XX:TieredStopAtLevel=1 -XX:+UseSerialGC \
    -Dspring.context.exit=onRefresh -jar app.jar
EXPOSE 8080
# Render free: 0,1 CPU y 512 MB. Sin opciones la JVM usa 1/4 de la RAM para el
# heap y gasta CPU compilando con C2; solo C1 + SerialGC arranca y responde mejor
ENTRYPOINT ["java", "-XX:SharedArchiveFile=app.jsa", "-XX:MaxRAMPercentage=70", "-XX:TieredStopAtLevel=1", "-XX:+UseSerialGC", "-jar", "app.jar"]
