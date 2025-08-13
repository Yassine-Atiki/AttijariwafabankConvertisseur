@echo off
echo Démarrage de l'application AttijariConverter...
echo.
echo Vérification de MongoDB...
echo Assurez-vous que MongoDB est démarré sur localhost:27017
echo.
echo Démarrage de l'application Spring Boot...
java -cp "target/classes;%USERPROFILE%\.m2\repository\org\springframework\boot\spring-boot-starter-data-mongodb\3.5.4\*;%USERPROFILE%\.m2\repository\org\springframework\boot\spring-boot-starter-web\3.5.4\*;%USERPROFILE%\.m2\repository\*" v1.attijariconverter.AttijariConverterApplication
pause

