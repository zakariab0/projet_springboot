// src/main/resources/static/js/charts.js
function createCharts(temperatureData, moistureData) {
    console.log('Creating charts with:', temperatureData, moistureData); // Debug

    const tempCtx = document.getElementById('tempChart').getContext('2d');
    new Chart(tempCtx, {
        type: 'line',
        data: {
            labels: temperatureData.map(d => d.time),
            datasets: [{
                label: 'Temperature (°C)',
                data: temperatureData.map(d => d.value),
                borderColor: '#FF5722',
                fill: false
            }]
        },
        options: {
            scales: { y: { beginAtZero: true, max: 50 } }
        }
    });

    const moistureCtx = document.getElementById('moistureChart').getContext('2d');
    new Chart(moistureCtx, {
        type: 'line',
        data: {
            labels: moistureData.map(d => d.time),
            datasets: [{
                label: 'Soil Moisture (%)',
                data: moistureData.map(d => d.value),
                borderColor: '#4CAF50',
                fill: false
            }]
        },
        options: {
            scales: { y: { beginAtZero: true, max: 100 } }
        }
    });
}