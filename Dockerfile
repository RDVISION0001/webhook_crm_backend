FROM openjdk:17
EXPOSE 8080
ADD target/springboot-image-file.jar springboot-image-file.jar
ENTRYPOINT ["java","-jar","/springboot-image-file.jar"]