CREATE DATABASE IF NOT EXISTS linguachat;
USE linguachat;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(40) NOT NULL UNIQUE,
    email VARCHAR(120) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    preferred_language ENUM('TAMIL', 'ENGLISH', 'HINDI', 'TELUGU', 'MALAYALAM') NOT NULL,
    created_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    original_text TEXT NOT NULL,
    translated_text TEXT NOT NULL,
    message_type ENUM('TEXT', 'IMAGE', 'FILE') NOT NULL DEFAULT 'TEXT',
    attachment_name VARCHAR(255),
    attachment_data LONGTEXT,
    reply_to_id BIGINT,
    reply_preview VARCHAR(500),
    source_language ENUM('TAMIL', 'ENGLISH', 'HINDI', 'TELUGU', 'MALAYALAM') NOT NULL,
    target_language ENUM('TAMIL', 'ENGLISH', 'HINDI', 'TELUGU', 'MALAYALAM') NOT NULL,
    status ENUM('SENT', 'DELIVERED', 'SEEN') NOT NULL,
    edited BOOLEAN NOT NULL DEFAULT FALSE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users(id),
    CONSTRAINT fk_messages_receiver FOREIGN KEY (receiver_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS translations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id BIGINT NOT NULL UNIQUE,
    original_text TEXT NOT NULL,
    translated_text TEXT NOT NULL,
    source_language ENUM('TAMIL', 'ENGLISH', 'HINDI', 'TELUGU', 'MALAYALAM') NOT NULL,
    target_language ENUM('TAMIL', 'ENGLISH', 'HINDI', 'TELUGU', 'MALAYALAM') NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_translations_message FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS reactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    emoji VARCHAR(10) NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_reactions_message FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_reactions_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uq_reaction_user_message_emoji UNIQUE (message_id, user_id, emoji)
);
