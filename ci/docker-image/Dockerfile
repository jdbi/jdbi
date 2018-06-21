FROM openjdk:8-jdk

RUN apt-get update && apt-get install -y \
    sudo \
 && rm -rf /var/lib/apt/lists/*

RUN useradd -ms /bin/bash test
