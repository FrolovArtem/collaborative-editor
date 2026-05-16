const params = new URLSearchParams(window.location.search);
const docId = params.get('id') || 'default';

const docIdDisplay = document.getElementById('docIdDisplay');
if (docIdDisplay) {
    docIdDisplay.textContent = docId;
}

const editor = document.getElementById('editor');
let isReceiving = false;

const socket = new SockJS('/ws');
const stompClient = StompJs.Stomp.over(socket);

stompClient.connect({}, function (frame) {
    console.log('Подключено: ' + frame);

    stompClient.subscribe('/topic/document/' + docId, function (message) {
        console.log('Получено сообщение:', message.body);
        isReceiving = true;
        editor.value = message.body;
        isReceiving = false;
    });
});  

editor.addEventListener('input', function() {
    console.log('input event fired, isReceiving:', isReceiving);
    if (isReceiving) return;
    const text = editor.value;
    console.log('Sending text to', '/app/edit/' + docId, text);
    stompClient.send('/app/edit/' + docId, {}, text);
});