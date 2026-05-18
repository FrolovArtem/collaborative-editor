const params = new URLSearchParams(window.location.search);
const docId = params.get('id') || 'default';

document.getElementById('docIdDisplay').textContent = docId;

const loginBlock = document.getElementById('loginBlock');
const editorBlock = document.getElementById('editorBlock');
const usernameInput = document.getElementById('usernameInput');
const loginBtn = document.getElementById('loginBtn');
const currentUserDisplay = document.getElementById('currentUserDisplay');
const userList = document.getElementById('userList');
const editor = document.getElementById('editor');

let isReceiving = false;
let stompClient = null;

let username = sessionStorage.getItem('username');

if (username) {
    showEditor(username);
} else {
    loginBlock.style.display = 'block';
    editorBlock.style.display = 'none';
    loginBtn.addEventListener('click', function() {
        const name = usernameInput.value.trim();
        if (name) {
            username = name;
            sessionStorage.setItem('username', username);
            showEditor(username);
        }
    });
}

function showEditor(username) {
    loginBlock.style.display = 'none';
    editorBlock.style.display = 'block';
    currentUserDisplay.textContent = username;

    connectWebSocket(username);
}

function connectWebSocket(username) {
    const socket = new SockJS('/ws');
    stompClient = StompJs.Stomp.over(socket);

    const headers = { username: username };

    stompClient.connect(headers, function(frame) {
        console.log('Подключено: ' + frame);

        stompClient.subscribe('/topic/document/' + docId, function(message) {
            console.log('Получено сообщение (текст):', message.body);
            isReceiving = true;
            editor.value = message.body;
            isReceiving = false;
        });

        stompClient.subscribe('/topic/document/' + docId + '/users', function(message) {
            console.log('Получен список пользователей:', message.body);
            const users = JSON.parse(message.body);
            renderUserList(users);
        });
    });

    editor.addEventListener('input', function() {
        if (isReceiving) return;
        const text = editor.value;
        stompClient.send('/app/edit/' + docId, {}, text);
    });
}

function renderUserList(users) {
    userList.innerHTML = '';
    users.forEach(function(user) {
        const li = document.createElement('li');
        li.textContent = user;
        userList.appendChild(li);
    });
}