const editor = document.getElementById('editor');
let isReceiving = false;

const socket = new SockJS('/ws');
const stompClient = StompJs.Stomp.over(socket);

stompClient.connect({}, function (frame) {
    console.log('Подключено: ' + frame);
    stompClient.subscribe('/topic/document', function (message) {
        isReceiving = true;
        editor.value = message.body;
        isReceiving = false;
    });
});

editor.addEventListener('input', function() {
    console.log('input event fired, isReceiving:', isReceiving);
    if (isReceiving) return;
    const text = editor.value;
    console.log('Sending text:', text);
    stompClient.send('/app/edit', {}, text);
});