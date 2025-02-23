// src/main/resources/static/js/charts.js
function createCharts(tempTimes, tempValues, moistureTimes, moistureValues) {
    console.log('Creating charts with:', tempTimes, tempValues, moistureTimes, moistureValues);

    const tempCtx = document.getElementById('tempChart').getContext('2d');
    new Chart(tempCtx, {
        type: 'line',
        data: {
            labels: tempTimes,
            datasets: [{
                label: 'Temperature (°C)',
                data: tempValues,
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
            labels: moistureTimes,
            datasets: [{
                label: 'Soil Moisture (%)',
                data: moistureValues,
                borderColor: '#4CAF50',
                fill: false
            }]
        },
        options: {
            scales: { y: { beginAtZero: true, max: 100 } }
        }
    });
}