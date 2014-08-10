
function getMeasurements() {
    var ws = new WebSocket("ws://localhost:9000/test/socket/measurements")
    //var ws = $("#gce-operations").data("ws-url");

    var testNodeColors = {};
    var operationsData = loadOperationsData([], null);
    var latenciesData = loadOperationsData([], null);
    var operationsChart = nv.models.lineChart();
    var latenciesChart = nv.models.lineChart();
    nv.addGraph(function() {
        operationsChart
            .useInteractiveGuideline(true);

        operationsChart.xAxis
            .axisLabel("Time (seconds)")
            .tickFormat(d3.format(',r'));

        operationsChart.yAxis
            .axisLabel("Operations")
            .tickFormat(d3.format(',f'));

        d3.select('#operationsChart svg')
            .datum(operationsData)
            .transition().duration(500)
            .call(operationsChart);

        return operationsChart;
    });
    nv.addGraph(function() {
        latenciesChart
            .useInteractiveGuideline(true);

        latenciesChart.xAxis
            .axisLabel("Time (seconds)")
            .tickFormat(d3.format(',r'));

        latenciesChart.yAxis
            .axisLabel("Latency (ms)")
            .tickFormat(d3.format(',.02f'));

        d3.select('#latenciesChart svg')
            .datum(latenciesData)
            .transition().duration(500)
            .call(latenciesChart);

        return latenciesChart;
    });

    ws.onopen = function() {
        var message = {
            action: "retrieve"
        };
        ws.send(JSON.stringify(message));
    };

    ws.onmessage = function(event) {
        var message = JSON.parse(event.data);
        switch(message.type) {
            case "measure":
                operationsData = loadOperationsData(operationsData, message, testNodeColors);
                latenciesData = loadLatenciesData(latenciesData, message, testNodeColors);
                d3.select('#operationsChart svg')
                    .datum(operationsData)
                    .transition().duration(500)
                    .call(operationsChart);
                d3.select('#latenciesChart svg')
                    .datum(latenciesData)
                    .transition().duration(500)
                    .call(latenciesChart);
                nv.utils.windowResize(function() {
                   operationsChart.update();
                   latenciesChart.update();
                });
                break;
        }
    };

    ws.onclose = function() {
        removeMeasurementSpinner();
    };

    setInterval(function() {
        var message = {
            action: "retrieve"
        };
        ws.send(JSON.stringify(message))
    }, 1000);
}

function pickColor(testNodeColors, nodeAddress) {
    if(!testNodeColors[nodeAddress]) {
        testNodeColors[nodeAddress] = "#" + Math.floor(Math.random()*16777215).toString(16);
    }
    return testNodeColors[nodeAddress];
}

function loadOperationsData(data, message, testNodeColors) {
    var addTestNode = true;
    if(!data) {
        data = Array();
    } else {
        data = Array.prototype.slice.call(data);
    }
    if(!message) {
        return [];
    }
    data.forEach(function(o) {
        if(o.key == message.node) {
            o.values.push({ x: (message.time / 1000) , y: message.insertcount });
            addTestNode = false;
        }
    });
    if(addTestNode) {
        data.push({
            key: message.node,
            color: pickColor(testNodeColors, message.node),
            values: []
        });
        data[data.length - 1].values.push({ x: (message.time / 1000) , y: message.insertcount });
    }
    return data;
}

function loadLatenciesData(data, message, testNodeColors) {
    var addTestNode = true;
    if(!data) {
        data = Array();
    } else {
        data = Array.prototype.slice.call(data);
    }
    if(!message) {
        return [];
    }
    data.forEach(function(o) {
        if(o.key == message.node) {
            o.values.push({ x: (message.time / 1000) , y: message.insertaverage });
            addTestNode = false;
        }
    });
    if(addTestNode) {
        data.push({
            key: message.node,
            color: pickColor(testNodeColors, message.node),
            values: []
        });
        data[data.length - 1].values.push({ x: (message.time / 1000) , y: message.insertaverage });
    }
    return data;
}



function removeAllSpinner() {
    var spinner = document.getElementById("operations-spinner");
    if(spinner) {
        spinner.outerHTML = "";
        delete spinner;
    }
    spinner = document.getElementById("latencies-spinner");
    if(spinner) {
        spinner.outerHTML = "";
        delete spinner;
    }
}

