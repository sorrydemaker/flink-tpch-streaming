let chart = null;

function initChart() {
    const ctx = document.getElementById('chartContainer').getContext('2d');
    chart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [{
                label: 'Query Results',
                data: [],
                borderColor: 'rgb(75, 192, 192)',
                tension: 0.1
            }]
        },
        options: {
            responsive: true,
            scales: {
                y: {
                    beginAtZero: true
                }
            },
            animation: {
                duration: 0 // 禁用动画以提高性能
            }
        }
    });
}

function updateChart(data) {
    if (!chart) {
        console.error('Chart not initialized');
        return;
    }

    const timestamp = new Date().toLocaleTimeString();

    if (chart.data.labels.length > 100) {
        chart.data.labels.shift();
        chart.data.datasets[0].data.shift();
    }

    chart.data.labels.push(timestamp);
    chart.data.datasets[0].data.push(data.value);

    chart.update('none');
}

function clearChart() {
    if (chart) {
        chart.data.labels = [];
        chart.data.datasets[0].data = [];
        chart.update();
    }
}