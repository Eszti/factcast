#!/bin/bash

mvn clean install -o -DskipTests && \
java \
-Dfactcast.store.pgsql.user=tester \
-Dfactcast.store.pgsql.password=test_password \
-Dfactcast.store.pgsql.dbname=test1 \
-Dspring.profiles.active=localtest \
-Dlogging.level.org.factcast=DEBUG \
-jar factcast-server/target/factcast.jar 


#-Dlogging.level.org.factcast=TRACE \
