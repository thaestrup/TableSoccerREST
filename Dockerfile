FROM eclipse-temurin:11-jdk

WORKDIR /usr/src/app

COPY . .

EXPOSE 5050

CMD ["./gradlew", "run"]
