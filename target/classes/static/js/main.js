document.addEventListener('DOMContentLoaded', function() {
    connectWebSocket();

    initChart();

    document.getElementById('startQueryBtn').addEventListener('click', startQuery);
    document.getElementById('stopQueryBtn').addEventListener('click', stopQuery);
});

function startQuery() {
    const querySelect = document.getElementById('querySelect');
    const startBtn = document.getElementById('startQueryBtn');
    const stopBtn = document.getElementById('stopQueryBtn');

    if (sendQueryRequest('start', querySelect.value)) {
        isQueryRunning = true;
        startBtn.disabled = true;
        stopBtn.disabled = false;
        querySelect.disabled = true;
        clearChart();
    }
}

function stopQuery() {
    const startBtn = document.getElementById('startQueryBtn');
    const stopBtn = document.getElementById('stopQueryBtn');
    const querySelect = document.getElementById('querySelect');

    if (sendQueryRequest('stop')) {
        isQueryRunning = false;
        startBtn.disabled = false;
        stopBtn.disabled = true;
        querySelect.disabled = false;
    }
}

function handleQueryResult(result) {
    if (result.error) {
        console.error('Query error:', result.message);
        stopQuery();
        alert('Query error: ' + result.message);
        return;
    }

    updateChart(result);
}

window.addEventListener('beforeunload', function() {
    if (ws && ws.readyState === WebSocket.OPEN) {
        ws.close();
    }
});