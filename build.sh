mvnw clean install
cd target
split -b 21M chatbot-0.0.1-SNAPSHOT.jar splited-app-
cd ..
rm build-file/splited-app-*
mv target/splited-app-* build-file/splited-app-*