const API = "";
const tokenKey = "linguachat_token";
const userKey = "linguachat_user";
const hiddenUsersKey = "linguachat_hidden_users";

let stompClient = null;
let me = JSON.parse(localStorage.getItem(userKey) || "null");
let users = [];
let activeUser = null;
let messages = [];
let reactionMessageId = null;
let replyToMessage = null;
let typingTimer = null;
let peerConnection = null;
let localStream = null;
let incomingCall = null;
let currentCallUserId = null;
let hiddenUserIds = JSON.parse(localStorage.getItem(hiddenUsersKey) || "[]");

const reactionEmojis = ["❤️", "😂", "👍", "🔥", "😮", "😍", "👏", "🙏", "😢", "😡", "💯", "🎉"];
const messageEmojis = [
    "😀", "😃", "😄", "😁", "😆", "😊", "😍", "😘", "😎", "🤩", "🥳", "😇",
    "😂", "🤣", "😭", "🥹", "😅", "😉", "😋", "😜", "🤔", "🙄", "😴", "🤯",
    "😡", "😮", "😱", "😢", "👍", "👎", "👏", "🙏", "🤝", "💪", "👌", "✌️",
    "❤️", "💙", "💚", "💛", "🔥", "💯", "✨", "🎉", "🎂", "🌟", "✅", "😂"
];

document.addEventListener("DOMContentLoaded", () => {
    restoreTheme();
    setupAuthPages();
    setupChatPage();
});

function setupAuthPages() {
    const loginForm = document.getElementById("loginForm");
    const registerForm = document.getElementById("registerForm");
    document.querySelectorAll(".password-toggle").forEach(button => {
        button.addEventListener("click", togglePasswordVisibility);
    });

    if (loginForm) {
        loginForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            await auth("/api/auth/login", {
                usernameOrEmail: value("loginName"),
                password: value("loginPassword")
            }, "loginError");
        });
    }

    if (registerForm) {
        registerForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            await auth("/api/auth/register", {
                username: value("regUsername"),
                email: value("regEmail"),
                password: value("regPassword"),
                preferredLanguage: value("regLanguage")
            }, "registerError");
        });
    }
}

function togglePasswordVisibility(event) {
    const button = event.currentTarget;
    const input = document.getElementById(button.dataset.target);
    const show = input.type === "password";
    input.type = show ? "text" : "password";
    button.innerHTML = show ? "&#128584;" : "&#128065;";
    button.title = show ? "Hide password" : "Show password";
    input.focus();
}

async function auth(path, body, errorId) {
    setError(errorId, "");
    try {
        const data = await request(path, { method: "POST", body });
        localStorage.setItem(tokenKey, data.token);
        localStorage.setItem(userKey, JSON.stringify({
            id: data.id,
            username: data.username,
            email: data.email,
            preferredLanguage: data.preferredLanguage
        }));
        location.href = "chat.html";
    } catch (error) {
        setError(errorId, error.message);
    }
}

async function setupChatPage() {
    if (!document.getElementById("messageForm")) return;
    if (!getToken()) {
        location.href = "login.html";
        return;
    }

    bindChatEvents();
    await loadMe();
    connectSocket();
    await loadUsers();
}

function bindChatEvents() {
    renderEmojiControls();
    document.getElementById("logoutBtn").addEventListener("click", logout);
    document.getElementById("deleteAccountBtn").addEventListener("click", deleteMyAccount);
    document.getElementById("darkToggle").addEventListener("click", toggleTheme);
    document.getElementById("backBtn").addEventListener("click", () => {
        document.querySelector(".app-shell").classList.remove("chat-open");
    });
    document.getElementById("searchUsers").addEventListener("input", debounce(loadUsers, 250));
    document.getElementById("messageForm").addEventListener("submit", sendMessage);
    document.getElementById("messageInput").addEventListener("input", sendTyping);
    document.getElementById("emojiBtn").addEventListener("click", toggleEmojiPicker);
    document.getElementById("attachBtn").addEventListener("click", () => document.getElementById("fileInput").click());
    document.getElementById("fileInput").addEventListener("change", sendAttachment);
    document.getElementById("cancelReplyBtn").addEventListener("click", clearReply);
    document.getElementById("voiceCallBtn").addEventListener("click", () => startCall("voice"));
    document.getElementById("videoCallBtn").addEventListener("click", () => startCall("video"));
    document.getElementById("clearChatBtn").addEventListener("click", clearCurrentChat);
    document.getElementById("removeUserBtn").addEventListener("click", removeCurrentUser);
    document.getElementById("acceptCallBtn").addEventListener("click", acceptIncomingCall);
    document.getElementById("rejectCallBtn").addEventListener("click", rejectIncomingCall);
    document.getElementById("endCallBtn").addEventListener("click", endCall);
    document.getElementById("reactionPopup").addEventListener("click", reactFromPopup);
    document.getElementById("emojiPicker").addEventListener("click", addEmojiFromPicker);
    document.addEventListener("click", (event) => {
        if (!event.target.closest(".message") && !event.target.closest("#reactionPopup")) {
            hideReactionPopup();
        }
        if (!event.target.closest("#emojiBtn") && !event.target.closest("#emojiPicker")) {
            hideEmojiPicker();
        }
    });
}

function renderEmojiControls() {
    document.getElementById("reactionPopup").innerHTML = reactionEmojis
            .map(emoji => `<button type="button">${emoji}</button>`)
            .join("");
    document.getElementById("emojiGrid").innerHTML = messageEmojis
            .map(emoji => `<button type="button">${emoji}</button>`)
            .join("");
}

async function loadMe() {
    me = await request("/api/users/me");
    localStorage.setItem(userKey, JSON.stringify(me));
    document.getElementById("meName").textContent = me.username;
    document.getElementById("meLanguage").textContent = prettyLanguage(me.preferredLanguage);
}

async function loadUsers() {
    const search = document.getElementById("searchUsers").value.trim();
    users = await request(`/api/users${search ? `?search=${encodeURIComponent(search)}` : ""}`);
    renderUsers();
}

function renderUsers() {
    const list = document.getElementById("userList");
    list.innerHTML = "";
    users.filter(user => !hiddenUserIds.includes(user.id)).forEach(user => {
        const row = document.createElement("div");
        row.className = `user-item ${activeUser && activeUser.id === user.id ? "active" : ""}`;
        row.innerHTML = `
            <div class="avatar">${escapeHtml(user.username[0].toUpperCase())}</div>
            <div class="user-meta">
                <strong>${escapeHtml(user.username)}</strong>
                <small>${prettyLanguage(user.preferredLanguage)} · ${user.online ? "online" : "offline"}</small>
            </div>
            <span class="online-dot ${user.online ? "on" : ""}"></span>
        `;
        row.addEventListener("click", () => openChat(user));
        list.appendChild(row);
    });
}

async function openChat(user) {
    activeUser = user;
    messages = await request(`/api/messages/${user.id}`);
    document.getElementById("chatTitle").textContent = user.username;
    document.getElementById("chatStatus").textContent = user.online ? "online" : "offline";
    document.getElementById("chatAvatar").textContent = user.username[0].toUpperCase();
    document.getElementById("messageInput").disabled = false;
    document.getElementById("sendBtn").disabled = false;
    document.getElementById("voiceCallBtn").disabled = false;
    document.getElementById("videoCallBtn").disabled = false;
    document.getElementById("clearChatBtn").disabled = false;
    document.getElementById("removeUserBtn").disabled = false;
    document.querySelector(".app-shell").classList.add("chat-open");
    renderUsers();
    renderMessages();
    sendSocket("/app/chat.delivered", { otherUserId: user.id });
    sendSocket("/app/chat.seen", { otherUserId: user.id });
}

function connectSocket() {
    stompClient = new MiniStompClient(`${location.protocol === "https:" ? "wss" : "ws"}://${location.host}/ws`);
    stompClient.connect({ Authorization: `Bearer ${getToken()}` }, () => {
        stompClient.subscribe("/user/queue/events", body => handleSocketEvent(JSON.parse(body)));
        stompClient.subscribe("/topic/presence", body => handlePresence(JSON.parse(body)));
    });
}

function handleSocketEvent(event) {
    if (event.type === "MESSAGE") {
        upsertMessage(event.payload);
        if (activeUser && event.fromUserId === activeUser.id) {
            sendSocket("/app/chat.delivered", { otherUserId: activeUser.id });
            sendSocket("/app/chat.seen", { otherUserId: activeUser.id });
        }
    }
    if (["EDIT", "DELETE", "REACTION"].includes(event.type)) {
        upsertMessage(event.payload);
    }
    if (event.type === "TYPING" && activeUser && event.fromUserId === activeUser.id) {
        document.getElementById("typingIndicator").textContent = event.payload ? `${activeUser.username} is typing...` : "";
    }
    if (event.type === "CALL_SIGNAL") {
        handleCallSignal(event.fromUserId, event.payload);
    }
    if (event.type === "DELIVERED" || event.type === "SEEN") {
        messages = messages.map(message => {
            if (message.senderId === me.id && activeUser && message.receiverId === activeUser.id) {
                return { ...message, status: event.type };
            }
            return message;
        });
        renderMessages();
    }
    if (event.type === "CHAT_CLEARED" && activeUser && (event.payload === activeUser.id || event.fromUserId === activeUser.id)) {
        messages = [];
        renderMessages();
    }
}

function handlePresence(event) {
    users = users.map(user => user.id === event.fromUserId ? { ...user, online: event.type === "ONLINE" } : user);
    if (activeUser && activeUser.id === event.fromUserId) {
        activeUser.online = event.type === "ONLINE";
        document.getElementById("chatStatus").textContent = activeUser.online ? "online" : "offline";
    }
    renderUsers();
}

function upsertMessage(message) {
    const isActive = activeUser && (message.senderId === activeUser.id || message.receiverId === activeUser.id);
    if (!isActive) return;
    const index = messages.findIndex(item => item.id === message.id);
    if (index >= 0) messages[index] = message;
    else messages.push(message);
    renderMessages();
}

function renderMessages() {
    const container = document.getElementById("messages");
    container.innerHTML = "";
    if (!activeUser) {
        container.innerHTML = `<div class="empty-state">Pick a user from the left to start chatting.</div>`;
        return;
    }

    messages.forEach(message => {
        const mine = message.senderId === me.id;
        const bubble = document.createElement("div");
        bubble.className = `message ${mine ? "mine" : "theirs"} ${message.deleted ? "deleted" : ""}`;
        bubble.dataset.id = message.id;
        const text = mine ? message.originalText : message.translatedText;
        const attachment = renderAttachment(message);
        const reply = message.replyPreview ? `<div class="reply-preview">${escapeHtml(message.replyPreview)}</div>` : "";
        const original = !mine && message.originalText !== message.translatedText
                ? `<div class="message-original">Original: ${escapeHtml(message.originalText)}<br>Translated to ${prettyLanguage(message.targetLanguage)}</div>` : "";
        bubble.innerHTML = `
            ${reply}
            ${attachment}
            <div class="message-text">${escapeHtml(text)}</div>
            ${original}
            <div class="reactions">${renderReactions(message.reactions || [])}</div>
            <div class="message-meta">
                ${message.edited ? "<span>edited</span>" : ""}
                <span>${formatTime(message.createdAt)}</span>
                ${mine ? `<span>${statusMark(message.status)}</span>` : ""}
            </div>
            ${mine && !message.deleted ? `
                <div class="message-actions">
                    <button data-action="edit">Edit</button>
                    <button data-action="reply">Reply</button>
                    <button data-action="delete">Delete</button>
                </div>` : `
                <div class="message-actions always-actions">
                    <button data-action="reply">Reply</button>
                </div>`}
        `;
        bubble.addEventListener("click", showReactionPopup);
        bubble.querySelectorAll("[data-action]").forEach(button => {
            button.addEventListener("click", (event) => handleMessageAction(event, message));
        });
        container.appendChild(bubble);
    });
    container.scrollTop = container.scrollHeight;
}

function renderReactions(reactions) {
    return reactions.map(reaction =>
            `<span class="reaction-count">${escapeHtml(reaction.emoji)} ${reaction.count}</span>`).join("");
}

function renderAttachment(message) {
    if (!message.attachmentData) return "";
    if (message.messageType === "IMAGE") {
        return `<img class="chat-image" src="${message.attachmentData}" alt="${escapeHtml(message.attachmentName || "image")}">`;
    }
    return `<a class="file-bubble" href="${message.attachmentData}" download="${escapeHtml(message.attachmentName || "file")}">${escapeHtml(message.attachmentName || "Download file")}</a>`;
}

function setReply(message) {
    replyToMessage = message;
    document.getElementById("replyText").textContent = message.originalText || message.attachmentName || "Message";
    document.getElementById("replyBar").classList.remove("hidden");
    document.getElementById("messageInput").focus();
}

function clearReply() {
    replyToMessage = null;
    document.getElementById("replyBar").classList.add("hidden");
    document.getElementById("replyText").textContent = "";
}

async function sendMessage(event) {
    event.preventDefault();
    if (!activeUser) return;
    const input = document.getElementById("messageInput");
    const text = input.value.trim();
    if (!text) return;
    input.value = "";
    sendSocket("/app/chat.send", { receiverId: activeUser.id, text, messageType: "TEXT", replyToId: replyToMessage ? replyToMessage.id : null });
    clearReply();
    sendSocket("/app/chat.typing", { receiverId: activeUser.id, typing: false });
}

function sendAttachment(event) {
    const file = event.target.files[0];
    event.target.value = "";
    if (!activeUser || !file) return;
    if (file.size > 900000) {
        alert("Please choose a file below 900 KB for this demo database storage.");
        return;
    }
    const reader = new FileReader();
    reader.onload = () => {
        const isImage = file.type.startsWith("image/");
        sendSocket("/app/chat.send", {
            receiverId: activeUser.id,
            text: isImage ? "" : file.name,
            messageType: isImage ? "IMAGE" : "FILE",
            attachmentName: file.name,
            attachmentData: reader.result,
            replyToId: replyToMessage ? replyToMessage.id : null
        });
        clearReply();
    };
    reader.readAsDataURL(file);
}

function sendTyping() {
    if (!activeUser) return;
    sendSocket("/app/chat.typing", { receiverId: activeUser.id, typing: true });
    clearTimeout(typingTimer);
    typingTimer = setTimeout(() => {
        sendSocket("/app/chat.typing", { receiverId: activeUser.id, typing: false });
    }, 900);
}

async function handleMessageAction(event, message) {
    event.stopPropagation();
    const action = event.target.dataset.action;
    if (action === "edit") {
        const newText = prompt("Edit message", message.originalText);
        if (newText && newText.trim()) {
            await request(`/api/messages/${message.id}`, { method: "PUT", body: { text: newText.trim() } });
        }
    }
    if (action === "reply") {
        setReply(message);
    }
    if (action === "delete" && confirm("Delete this message?")) {
        await request(`/api/messages/${message.id}`, { method: "DELETE" });
    }
}

async function clearCurrentChat() {
    if (!activeUser) return;
    if (!confirm(`Clear chat with ${activeUser.username}?`)) return;
    await request(`/api/messages/conversation/${activeUser.id}`, { method: "DELETE" });
    messages = [];
    renderMessages();
}

function removeCurrentUser() {
    if (!activeUser) return;
    if (!confirm(`Remove ${activeUser.username} from your chat list?`)) return;
    hiddenUserIds = [...new Set([...hiddenUserIds, activeUser.id])];
    localStorage.setItem(hiddenUsersKey, JSON.stringify(hiddenUserIds));
    activeUser = null;
    messages = [];
    document.getElementById("chatTitle").textContent = "Select a chat";
    document.getElementById("chatStatus").textContent = "Online status appears here";
    document.getElementById("chatAvatar").textContent = "L";
    document.getElementById("messageInput").disabled = true;
    document.getElementById("sendBtn").disabled = true;
    document.getElementById("voiceCallBtn").disabled = true;
    document.getElementById("videoCallBtn").disabled = true;
    document.getElementById("clearChatBtn").disabled = true;
    document.getElementById("removeUserBtn").disabled = true;
    renderUsers();
    renderMessages();
}

function restoreHiddenUsers() {
    hiddenUserIds = [];
    localStorage.setItem(hiddenUsersKey, JSON.stringify(hiddenUserIds));
    renderUsers();
}

function showReactionPopup(event) {
    const messageEl = event.currentTarget;
    reactionMessageId = Number(messageEl.dataset.id);
    const popup = document.getElementById("reactionPopup");
    popup.style.left = `${Math.min(event.clientX, window.innerWidth - 360)}px`;
    popup.style.top = `${Math.max(event.clientY - 62, 8)}px`;
    popup.classList.remove("hidden");
    hideEmojiPicker();
}

function hideReactionPopup() {
    document.getElementById("reactionPopup").classList.add("hidden");
    reactionMessageId = null;
}

function reactFromPopup(event) {
    if (event.target.tagName !== "BUTTON" || !reactionMessageId) return;
    sendSocket("/app/chat.reaction", { messageId: reactionMessageId, emoji: event.target.textContent });
    hideReactionPopup();
}

function toggleEmojiPicker(event) {
    event.stopPropagation();
    const picker = document.getElementById("emojiPicker");
    const button = document.getElementById("emojiBtn");
    const rect = button.getBoundingClientRect();
    picker.style.left = `${Math.max(8, rect.left)}px`;
    picker.style.bottom = `${window.innerHeight - rect.top + 8}px`;
    picker.classList.toggle("hidden");
    hideReactionPopup();
}

function hideEmojiPicker() {
    document.getElementById("emojiPicker").classList.add("hidden");
}

function addEmojiFromPicker(event) {
    if (event.target.tagName !== "BUTTON") return;
    const input = document.getElementById("messageInput");
    input.value += event.target.textContent;
    input.focus();
    sendTyping();
}

async function startCall(callType) {
    if (!activeUser) return;
    currentCallUserId = activeUser.id;
    openCallModal(`${callType === "video" ? "Video" : "Voice"} call with ${activeUser.username}`, "Calling...");
    await prepareMedia(callType);
    createPeerConnection(currentCallUserId);
    localStream.getTracks().forEach(track => peerConnection.addTrack(track, localStream));
    const offer = await peerConnection.createOffer();
    await peerConnection.setLocalDescription(offer);
    sendSocket("/app/call.signal", {
        receiverId: currentCallUserId,
        signalType: "OFFER",
        callType,
        sdp: JSON.stringify(offer)
    });
}

async function handleCallSignal(fromUserId, signal) {
    if (signal.signalType === "OFFER") {
        incomingCall = { fromUserId, signal };
        const caller = users.find(user => user.id === fromUserId);
        openCallModal(`${signal.callType === "video" ? "Video" : "Voice"} call from ${caller ? caller.username : "user"}`, "Incoming call");
        document.getElementById("acceptCallBtn").classList.remove("hidden");
        document.getElementById("endCallBtn").classList.add("hidden");
        return;
    }
    if (signal.signalType === "ANSWER" && peerConnection) {
        await peerConnection.setRemoteDescription(JSON.parse(signal.sdp));
    }
    if (signal.signalType === "ICE" && peerConnection && signal.candidate) {
        await peerConnection.addIceCandidate(JSON.parse(signal.candidate));
    }
    if (signal.signalType === "END") {
        closeCall("Call ended");
    }
}

async function acceptIncomingCall() {
    if (!incomingCall) return;
    currentCallUserId = incomingCall.fromUserId;
    const signal = incomingCall.signal;
    document.getElementById("acceptCallBtn").classList.add("hidden");
    document.getElementById("endCallBtn").classList.remove("hidden");
    document.getElementById("callState").textContent = "Connecting...";
    await prepareMedia(signal.callType);
    createPeerConnection(currentCallUserId);
    localStream.getTracks().forEach(track => peerConnection.addTrack(track, localStream));
    await peerConnection.setRemoteDescription(JSON.parse(signal.sdp));
    const answer = await peerConnection.createAnswer();
    await peerConnection.setLocalDescription(answer);
    sendSocket("/app/call.signal", {
        receiverId: currentCallUserId,
        signalType: "ANSWER",
        callType: signal.callType,
        sdp: JSON.stringify(answer)
    });
    incomingCall = null;
}

function rejectIncomingCall() {
    const receiverId = incomingCall ? incomingCall.fromUserId : currentCallUserId;
    if (receiverId) {
        sendSocket("/app/call.signal", { receiverId, signalType: "END", callType: "voice" });
    }
    closeCall("Call rejected");
}

function endCall() {
    if (currentCallUserId) {
        sendSocket("/app/call.signal", { receiverId: currentCallUserId, signalType: "END", callType: "voice" });
    }
    closeCall("Call ended");
}

async function prepareMedia(callType) {
    localStream = await navigator.mediaDevices.getUserMedia({
        audio: true,
        video: callType === "video"
    });
    document.getElementById("localVideo").srcObject = localStream;
}

function createPeerConnection(receiverId) {
    peerConnection = new RTCPeerConnection({
        iceServers: [{ urls: "stun:stun.l.google.com:19302" }]
    });
    peerConnection.ontrack = event => {
        document.getElementById("remoteVideo").srcObject = event.streams[0];
        document.getElementById("callState").textContent = "Connected";
    };
    peerConnection.onicecandidate = event => {
        if (event.candidate) {
            sendSocket("/app/call.signal", {
                receiverId,
                signalType: "ICE",
                candidate: JSON.stringify(event.candidate)
            });
        }
    };
}

function openCallModal(title, state) {
    document.getElementById("callTitle").textContent = title;
    document.getElementById("callState").textContent = state;
    document.getElementById("acceptCallBtn").classList.add("hidden");
    document.getElementById("endCallBtn").classList.remove("hidden");
    document.getElementById("callModal").classList.remove("hidden");
}

function closeCall(state) {
    document.getElementById("callState").textContent = state;
    if (peerConnection) peerConnection.close();
    if (localStream) localStream.getTracks().forEach(track => track.stop());
    peerConnection = null;
    localStream = null;
    incomingCall = null;
    currentCallUserId = null;
    document.getElementById("remoteVideo").srcObject = null;
    document.getElementById("localVideo").srcObject = null;
    setTimeout(() => document.getElementById("callModal").classList.add("hidden"), 700);
}

function sendSocket(destination, body) {
    if (stompClient && stompClient.connected) {
        stompClient.send(destination, JSON.stringify(body));
    }
}

class MiniStompClient {
    constructor(url) {
        this.url = url;
        this.socket = null;
        this.connected = false;
        this.subscriptions = new Map();
        this.nextId = 1;
    }

    connect(headers, onConnect) {
        this.headers = headers;
        this.onConnect = onConnect;
        this.socket = new WebSocket(this.url);
        this.socket.onopen = () => {
            this.writeFrame("CONNECT", {
                "accept-version": "1.2",
                "heart-beat": "10000,10000",
                ...this.headers
            }, "");
        };
        this.socket.onmessage = event => this.handleData(event.data);
        this.socket.onclose = () => {
            this.connected = false;
            setTimeout(() => this.connect(this.headers, this.onConnect), 3000);
        };
    }

    subscribe(destination, callback) {
        const id = `sub-${this.nextId++}`;
        this.subscriptions.set(id, callback);
        this.writeFrame("SUBSCRIBE", { id, destination, ack: "auto" }, "");
    }

    send(destination, body) {
        this.writeFrame("SEND", { destination, "content-type": "application/json" }, body);
    }

    handleData(data) {
        data.split("\0").filter(Boolean).forEach(rawFrame => {
            const frame = this.parseFrame(rawFrame);
            if (frame.command === "CONNECTED") {
                this.connected = true;
                this.onConnect();
            }
            if (frame.command === "MESSAGE") {
                const callback = this.subscriptions.get(frame.headers.subscription);
                if (callback) callback(frame.body);
            }
        });
    }

    parseFrame(rawFrame) {
        const clean = rawFrame.replace(/^\n+/, "");
        const parts = clean.split("\n\n");
        const headerLines = parts[0].split("\n");
        const command = headerLines.shift();
        const headers = {};
        headerLines.forEach(line => {
            const index = line.indexOf(":");
            if (index > -1) headers[line.slice(0, index)] = line.slice(index + 1);
        });
        return { command, headers, body: parts.slice(1).join("\n\n") };
    }

    writeFrame(command, headers, body) {
        if (!this.socket || this.socket.readyState !== WebSocket.OPEN) return;
        const headerText = Object.entries(headers)
                .map(([key, val]) => `${key}:${val}`)
                .join("\n");
        this.socket.send(`${command}\n${headerText}\n\n${body}\0`);
    }
}

async function request(path, options = {}) {
    const init = {
        method: options.method || "GET",
        headers: { "Content-Type": "application/json" }
    };
    if (getToken() && !path.startsWith("/api/auth/")) {
        init.headers.Authorization = `Bearer ${getToken()}`;
    }
    if (options.body) init.body = JSON.stringify(options.body);

    const response = await fetch(API + path, init);
    if (!response.ok) {
        const text = await response.text();
        try {
            const error = JSON.parse(text);
            throw new Error(error.message || text || "Request failed");
        } catch (parseError) {
            throw new Error(parseError.message === text ? text : (text || `Request failed with status ${response.status}`));
        }
    }
    if (response.status === 204) return null;
    const text = await response.text();
    return text ? JSON.parse(text) : null;
}

function logout() {
    localStorage.removeItem(tokenKey);
    localStorage.removeItem(userKey);
    location.href = "login.html";
}

async function deleteMyAccount() {
    if (!confirm("Delete your account permanently? You can register again with the same username after this.")) return;
    await request("/api/users/me", { method: "DELETE" });
    localStorage.removeItem(tokenKey);
    localStorage.removeItem(userKey);
    location.href = "register.html";
}

function getToken() {
    return localStorage.getItem(tokenKey);
}

function value(id) {
    return document.getElementById(id).value.trim();
}

function setError(id, message) {
    const el = document.getElementById(id);
    if (el) el.textContent = message;
}

function prettyLanguage(language) {
    return (language || "").toLowerCase().replace(/^\w/, c => c.toUpperCase());
}

function formatTime(dateText) {
    return new Date(dateText).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
}

function statusMark(status) {
    if (status === "SEEN") return "✓✓";
    if (status === "DELIVERED") return "✓✓";
    return "✓";
}

function escapeHtml(text) {
    const div = document.createElement("div");
    div.textContent = text || "";
    return div.innerHTML;
}

function debounce(fn, wait) {
    let timer;
    return (...args) => {
        clearTimeout(timer);
        timer = setTimeout(() => fn(...args), wait);
    };
}

function toggleTheme() {
    document.body.classList.toggle("dark");
    localStorage.setItem("linguachat_theme", document.body.classList.contains("dark") ? "dark" : "light");
}

function restoreTheme() {
    if (localStorage.getItem("linguachat_theme") === "dark") {
        document.body.classList.add("dark");
    }
}
