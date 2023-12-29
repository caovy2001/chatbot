./mvnw clean install
cd target
split -b 21M chatbot-0.0.1-SNAPSHOT.jar splited-app-
cd ..
rm build-file/splited-app-*
mv target/splited-app-* build-file/
git add .
git commit -m 'update'
git push origin 'gpt-for-predict'
sshpass -p 'Thisispassword123@' ssh root@14.225.207.19 'cd chatbot && git checkout gpt-for-predict && git pull origin gpt-for-predict && docker rm -f chatbot-api-1 && docker compose up -d --build && echo y | docker system prune -a'