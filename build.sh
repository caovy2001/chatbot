./mvnw clean install
cd target
split -b 21M chatbot-0.0.1-SNAPSHOT.jar splited-app-
cd ..
rm build-file/splited-app-*
mv target/splited-app-* build-file/
git add .
git commit -m 'update'
git push origin main
 sshpass -p 'Thisispassword123@' ssh root@14.225.207.19 'cd chatbot && git pull origin main && docker compose up -d --build && echo y | docker system prune -a'