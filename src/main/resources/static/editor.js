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

let stompClient = null;
let isApplyingRemote = false;   

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

// WebSocket
function connectWebSocket(username) {
    const socket = new SockJS('/ws');
    stompClient = StompJs.Stomp.over(socket);
    const headers = { username: username };

    stompClient.connect(headers, function(frame) {
        console.log('Подключено: ' + frame);

        stompClient.subscribe('/topic/document/' + docId, function(message) {
            const patch = JSON.parse(message.body);
            
            pendingPatches = [];
            if (patchTimer) {
                clearTimeout(patchTimer);
                patchTimer = null;
            }
            isApplyingRemote = true;
            applyPatchToEditor(patch);
            isApplyingRemote = false;
        });

        stompClient.subscribe('/topic/document/' + docId + '/users', function(message) {
            const users = JSON.parse(message.body);
            renderUserList(users);
        });
    });
}

let pendingPatches = [];
let patchTimer = null;

function addPatch(patch) {
    console.log('Добавлен патч:', patch, 'Текущая длина текста:', editor.value.length);
    pendingPatches.push(patch);
    if (patchTimer) clearTimeout(patchTimer);
    patchTimer = setTimeout(sendPatches, 300);
}

function sendPatches() {
    if (pendingPatches.length === 0) return;
    const batch = [...pendingPatches];
    pendingPatches = [];
    const json = JSON.stringify(batch);
    console.log('Отправляем патч. Длина редактора:', editor.value.length, 'Пакет:', json);
    stompClient.send('/app/patch/' + docId, {}, json);
}

// Генерация патча из beforeinput
editor.addEventListener('beforeinput', function(event) {
    if (isApplyingRemote) return;

    const start = editor.selectionStart;
    const end = editor.selectionEnd;
    const inputType = event.inputType;

    // Вставка текста
    if (inputType === 'insertText' || inputType === 'insertFromPaste' ||
        inputType === 'insertFromDrop' || inputType === 'insertFromComposition') {
        event.preventDefault();  // отменяем стандартное действие, мы сами вставим
        const text = event.data || '';
        addPatch({ op: 'insert', pos: start, text: text });
        // Применяем локально сразу же, чтобы видеть свой ввод
        editor.setRangeText(text, start, end, 'end');
        // Устанавливаем курсор после вставленного
        editor.selectionStart = editor.selectionEnd = start + text.length;
    }
    // Удаление назад
    else if (inputType === 'deleteContentBackward') {
        event.preventDefault();
        if (start === end && start > 0) {
            const removed = editor.value.substring(start - 1, start);
            addPatch({ op: 'delete', pos: start - 1, length: 1, text: removed });
            editor.setRangeText('', start - 1, start, 'end');
        } else if (start !== end) {
            const removed = editor.value.substring(start, end);
            addPatch({ op: 'delete', pos: start, length: end - start, text: removed });
            editor.setRangeText('', start, end, 'end');
        }
    }
    // Удаление вперёд
    else if (inputType === 'deleteContentForward') {
        event.preventDefault();
        if (start === end && start < editor.value.length) {
            const removed = editor.value.substring(start, start + 1);
            addPatch({ op: 'delete', pos: start, length: 1, text: removed });
            editor.setRangeText('', start, start + 1, 'end');
        } else if (start !== end) {
            const removed = editor.value.substring(start, end);
            addPatch({ op: 'delete', pos: start, length: end - start, text: removed });
            editor.setRangeText('', start, end, 'end');
        }
    }
});

// Обработка вставки из контекстного меню/мышкой
editor.addEventListener('paste', function(event) {
    if (isApplyingRemote) return;
    event.preventDefault();
    const text = event.clipboardData.getData('text/plain');
    const start = editor.selectionStart;
    const end = editor.selectionEnd;
    addPatch({ op: 'insert', pos: start, text: text });
    editor.setRangeText(text, start, end, 'end');
    editor.selectionStart = editor.selectionEnd = start + text.length;
});

// Обработка вырезания
editor.addEventListener('cut', function(event) {
    if (isApplyingRemote) return;
    event.preventDefault();
    const start = editor.selectionStart;
    const end = editor.selectionEnd;
    if (start !== end) {
        const text = editor.value.substring(start, end);
        event.clipboardData.setData('text/plain', text);
        addPatch({ op: 'delete', pos: start, length: end - start, text: text });
        editor.setRangeText('', start, end, 'end');
    }
});

// Применение пришедшего от сервера патча
function applyPatchToEditor(patches) {
    const list = Array.isArray(patches) ? patches : [patches];
    let shift = 0;  
    for (const p of list) {
        const pos = p.pos + shift;
        if (p.op === 'insert') {
            editor.setRangeText(p.text, pos, pos, 'end');
            shift += p.text.length;
        } else if (p.op === 'delete') {
            editor.setRangeText('', pos, pos + p.length, 'end');
            shift -= p.length;
        }
    }
}

function renderUserList(users) {
    userList.innerHTML = '';
    users.forEach(function(user) {
        const li = document.createElement('li');
        li.textContent = user;
        userList.appendChild(li);
    });
}