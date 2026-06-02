# LinguaChat

LinguaChat is a full stack realtime multilingual chat application inspired by WhatsApp Web.

## Repository

https://github.com/Bhuvana9787/linguachat

## Live Demo

Deployment URL will be added here after Railway deployment.

## Features

- Register and login
- JWT authentication
- BCrypt password encryption
- MySQL database
- One-to-one realtime chat
- WebSocket + STOMP messaging
- Chat history
- Automatic message translation based on receiver language
- Original and translated message storage
- Emoji picker and emoji reactions
- Typing indicator
- Online/offline status
- Delivered and seen status
- Edit and delete message
- Clear chat
- Delete account and register again with the same username
- Image/file attachment support
- Reply to message
- Voice/video call signaling with WebRTC
- Dark mode
- Responsive mobile layout

## Tech Stack

- Frontend: HTML, CSS, JavaScript
- Backend: Java 17, Spring Boot, Spring Security, JWT, WebSocket, STOMP
- Database: MySQL

## Project Structure

```text
linguachat/
+-- pom.xml
+-- Procfile
+-- system.properties
+-- src/main/java/com/linguachat
|   +-- config
|   +-- controller
|   +-- dto
|   +-- entity
|   +-- exception
|   +-- repository
|   +-- security
|   +-- service
+-- src/main/resources
    +-- application.properties
    +-- schema.sql
    +-- static
        +-- login.html
        +-- register.html
        +-- chat.html
        +-- style.css
        +-- app.js
```

## Local Setup

1. Start XAMPP MySQL.
2. Create the database:

```sql
CREATE DATABASE IF NOT EXISTS linguachat;
```

3. Run from Command Prompt:

```cmd
cd /d "C:\Users\acer\OneDrive\Desktop\chat application"
mvnw.cmd clean spring-boot:run
```

4. Open:

```text
http://localhost:8084/login.html
```

## Fresh Database

To remove old usernames, passwords, chats, messages, translations, and reactions:

```text
Clear-LinguaChat-Database.cmd
```

Run it after starting XAMPP MySQL.

## Test Users

Use two browser sessions:

```text
Normal Chrome window:
username: Bhuvana

Incognito Chrome window:
username: Thamo
```

Choose different languages to test translation.

## Environment Variables

For cloud hosting, set these variables:

```text
PORT
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
APP_JWT_KEY
APP_JWT_EXPIRATION_MS
APP_TRANSLATION_URL
```

Example:

```text
SPRING_DATASOURCE_URL=jdbc:mysql://host:3306/database?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=myuser
SPRING_DATASOURCE_PASSWORD=mypassword
APP_JWT_KEY=UseASecretWithAtLeastThirtyTwoCharacters
```

## GitHub Upload

Repository:

```text
https://github.com/Bhuvana9787/linguachat
```

Run:

```cmd
cd /d "C:\Users\acer\OneDrive\Desktop\chat application"
git init
git add .
git commit -m "Initial LinguaChat full stack app"
git branch -M main
git remote add origin https://github.com/Bhuvana9787/linguachat.git
git push -u origin main
```

If `origin` already exists, use:

```cmd
git remote set-url origin https://github.com/Bhuvana9787/linguachat.git
git push -u origin main
```

## Hosting

GitHub Pages cannot host this complete app because LinguaChat needs Java Spring Boot, WebSocket, JWT, and MySQL. GitHub Pages only hosts static frontend files.

Use a backend hosting platform such as vercel or Render, then connect it to your GitHub repository.

### Railway Basic Steps

1. Push this project to GitHub.
2. Open Railway.
3. Create a new project from your GitHub repository.
4. Add a MySQL database service.
5. Add environment variables for datasource URL, username, password, and JWT secret.
6. Deploy the Spring Boot service.
7. Open the generated Railway public URL.

### Render Basic Steps

1. Push this project to GitHub.
2. Create a Web Service from your GitHub repository.
3. Use Java 17.
4. Build command:

```text
mvn clean package -DskipTests
```

5. Start command:

```text
java -jar target/linguachat-1.0.0.jar
```

6. Add a MySQL database or connect an external MySQL database.
7. Add environment variables.
8. Deploy and use the generated Render URL.

## How Translation Works

Each user selects a preferred language during registration. When a sender sends a message, the backend checks the receiver language, calls the configured translation API, stores both original and translated text, and sends the translated message to the receiver in realtime.

Supported languages:

- Tamil
- English
- Hindi
- Telugu
- Malayalam

## Notes

- Keep the JWT secret private.
- Do not upload real passwords or `.env` files.
- WebRTC voice/video calls require browser camera/microphone permission.
- For production, use a managed MySQL database and a strong JWT secret.
