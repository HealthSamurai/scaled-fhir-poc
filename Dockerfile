FROM openjdk:11-jre

RUN wget https://github.com/rapidloop/pgmetrics/releases/download/v1.7.1/pgmetrics_1.7.1_linux_amd64.tar.gz \
  && tar -xzf pgmetrics_1.7.1_linux_amd64.tar.gz \
  && cp pgmetrics_1.7.1_linux_amd64/pgmetrics /usr/local/bin/pgmetrics \
  && rm -rf pgmetrics_1.7.1_linux_amd64*

CMD java -XX:-OmitStackTraceInFastThrow \
  -Dcom.sun.management.jmxremote \
  -Dcom.sun.management.jmxremote.host=127.0.0.1 \
  -Djava.rmi.server.hostname=127.0.0.1 \
  -Dcom.sun.management.jmxremote.port=9099 \
  -Dcom.sun.management.jmxremote.authenticate=false \
  -Dcom.sun.management.jmxremote.ssl=false\
  -jar /app.jar -m scaled-fhir.core

ADD target/scaled-fhir-1.0.0-standalone.jar /app.jar

COPY migrations.sh /migrations/migrations.sh
