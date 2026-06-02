FROM ubuntu:22.04

RUN apt-get update && \
    apt-get install -y wget unzip openjdk-17-jdk && \
    wget https://github.com/JetBrains/kotlin/releases/download/v1.9.0/kotlin-compiler-1.9.0.zip && \
    unzip kotlin-compiler-1.9.0.zip -d /opt/ && \
    rm kotlin-compiler-1.9.0.zip && \
    ln -s /opt/kotlinc/bin/kotlinc /usr/local/bin/kotlinc && \
    apt-get clean

WORKDIR /app
COPY src/ ./src/
RUN kotlinc src/compiler/*.kt src/vm/*.kt src/Main.kt -include-runtime -d out.jar

ENTRYPOINT ["java", "-jar", "out.jar"]
CMD ["src/Primes.o"]