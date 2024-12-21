let ws = null;
let isQueryRunning = false;

function connectWebSocket() {
    ws = new WebSocket('ws://localhost:8080/ws/query');

    ws.onopen = function() {
        console.log('WebSocket connected');
        updateStatus('Connected', true);
    };

    ws.onmessage = function(event) {
        console.log('Received message:', event.data);
        try {
            const result = JSON.parse(event.data);
            handleQueryResult(result);
        } catch (e) {
            console.error('Error parsing message:', e);
        }
    };

    ws.onclose = function() {
        console.log('WebSocket disconnected');
        updateStatus('Disconnected', false);
        // 3秒后尝试重连
        setTimeout(connectWebSocket, 3000);
    };

    ws.onerror = function(error) {
        console.error('WebSocket error:', error);
        updateStatus('Error: ' + error.message, false);
    };
}

function updateStatus(message, isConnected) {
    const statusDiv = document.getElementById('status');
    statusDiv.textContent = message;
    statusDiv.className = isConnected ? 'connected' : 'disconnected';
}

function sendQueryRequest(action, queryName) {
    if (!ws || ws.readyState !== WebSocket.OPEN) {
        alert('WebSocket not connected! Please wait for reconnection...');
        return false;
    }

    const request = {
        action: action,
        queryName: queryName
    };

    console.log(`Sending ${action} request:`, request);
    ws.send(JSON.stringify(request));
    return true;
}