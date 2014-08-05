
function getMeasurements() {
    var ws = new WebSocket("ws://localhost:9000/test/socket/measurements")
    //var ws = $("#gce-operations").data("ws-url");
    var operationsCtx = document.getElementById("operations");
    var latenciesCtx = document.getElementById("latencies");
    var operationsData = loadOperationsData({}, {
        node: "-",
        time: "-1",
        insertcount: 0
    });
    var latenciesData = loadOperationsData({}, {
        node: "-",
        time: "-1",
        insertaverage: 0
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
                removeAllSpinner();
                operationsData = loadOperationsData(operationsData, message);
                latenciesData = loadLatenciesData(latenciesData, message);
                new Chart(operationsCtx.getContext("2d")).Line(operationsData, { responsive : true });
                new Chart(latenciesCtx.getContext("2d")).Line(latenciesData, { responsive : true });
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

function loadOperationsData(data, message) {
    var l_index = -1;
    var ds_index = -1;
    if(!data.datasets) {
        data = {
            labels: [],
            datasets: []
        }
    }
    if(data.datasets.length >= 2) {
        for(var i = 0; i < data.datasets.length; i++) {
            if(data.datasets[i].label != "-") {
                continue;
            }
            ds_index = i;
            break;
        }
        if(ds_index != -1) {
            data.datasets.splice(ds_index, 1);
        }
    }
    for(var i = 0; i < data.labels.length; i++) {
        if(data.labels[i] != message.time) {
            continue;
        }
        l_index = i;
        break;
    }
    ds_index = -1;
    for(var i = 0; i < data.datasets.length; i++) {
        if(data.datasets[i].label != message.node) {
            continue;
        }
        ds_index = i;
        break;
    }
    if(l_index == -1) {
        data.labels.push("" + message.time);
    }
    if(ds_index == -1) {
        var color = "" +
          Math.floor(Math.random()*256) + "," +
          Math.floor(Math.random()*256) + "," +
          Math.floor(Math.random()*256);
        data.datasets.push({
            label: message.node,
            fillColor: "rgba(" + color + ",0.2)",
            strokeColor: "rgba(" + color + ",1)",
            pointColor: "rgba(" + color + ",1)",
            pointStrokeColor: "#fff",
            pointHighlightFill: "#fff",
            pointHighlightStroke: "rgba(" + color + ",1)",
            data: [ 0 ]
        });
        ds_index = data.datasets.length - 1;
    }
    data.datasets[ds_index].data.push(message.insertcount);
    return data;
}

function loadLatenciesData(data, message) {
    var l_index = -1;
    var ds_index = -1;
    if(!data.datasets) {
        data = {
            labels: [],
            datasets: []
        }
    }
    if(data.datasets.length >= 2) {
        for(var i = 0; i < data.datasets.length; i++) {
            if(data.datasets[i].label != "-") {
                continue;
            }
            ds_index = i;
            break;
        }
        if(ds_index != -1) {
            data.datasets.splice(ds_index, 1);
        }
    }
    for(var i = 0; i < data.labels.length; i++) {
        if(data.labels[i] != message.time) {
            continue;
        }
        l_index = i;
        break;
    }
    ds_index = -1;
    for(var i = 0; i < data.datasets.length; i++) {
        if(data.datasets[i].label != message.node) {
            continue;
        }
        ds_index = i;
        break;
    }
    if(l_index == -1) {
        data.labels.push("" + message.time);
    }
    if(ds_index == -1) {
        var color = "" +
          Math.floor(Math.random()*256) + "," +
          Math.floor(Math.random()*256) + "," +
          Math.floor(Math.random()*256);
        data.datasets.push({
            label: message.node,
            fillColor: "rgba(" + color + ",0.2)",
            strokeColor: "rgba(" + color + ",1)",
            pointColor: "rgba(" + color + ",1)",
            pointStrokeColor: "#fff",
            pointHighlightFill: "#fff",
            pointHighlightStroke: "rgba(" + color + ",1)",
            data: [ 0 ]
        });
        ds_index = data.datasets.length - 1;
    }
    data.datasets[ds_index].data.push(message.insertaverage);
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

