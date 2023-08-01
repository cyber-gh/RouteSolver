# Step 1: Use a base image with Java 11 pre-installed
FROM adoptopenjdk:11-jdk-hotspot

# Step 2: Install sbt and other dependencies
ENV SBT_VERSION=1.6.2
RUN apt-get update && \
    apt-get install -y curl unzip && \
    curl -L -o sbt-$SBT_VERSION.zip https://github.com/sbt/sbt/releases/download/v$SBT_VERSION/sbt-$SBT_VERSION.zip && \
    unzip sbt-$SBT_VERSION.zip && \
    mv sbt /usr/local/sbt && \
    echo 'export PATH=/usr/local/sbt/bin:$PATH' >> /root/.bashrc && \
    rm sbt-$SBT_VERSION.zip

# Step 3: Install Scala
ENV SCALA_VERSION=2.13.5
RUN curl -L -o scala-$SCALA_VERSION.tgz https://downloads.typesafe.com/scala/$SCALA_VERSION/scala-$SCALA_VERSION.tgz && \
    tar -xzvf scala-$SCALA_VERSION.tgz && \
    mv scala-$SCALA_VERSION /usr/share/scala && \
    ln -s /usr/share/scala/bin/scala /usr/bin/scala && \
    ln -s /usr/share/scala/bin/scalac /usr/bin/scalac && \
    ln -s /usr/share/scala/bin/scaladoc /usr/bin/scaladoc && \
    ln -s /usr/share/scala/bin/scalap /usr/bin/scalap && \
    rm scala-$SCALA_VERSION.tgz


# Step 5: Add sbt to the PATH environment variable
ENV PATH="/usr/local/sbt/bin:${PATH}"

ENV APPLICATION_SECRET=abcdefghijk

# copy code
COPY . /root/app/
WORKDIR /root/app

RUN sbt dist

RUN cp target/universal/routesolver-1.0.zip .
RUN unzip routesolver-1.0.zip

EXPOSE 9000

CMD ["./routesolver-1.0/bin/routesolver"]