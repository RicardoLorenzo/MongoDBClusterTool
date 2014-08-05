
function getMeasurements() {
    var ws = new WebSocket("ws://localhost:9000/test/socket/measurements")
    //var ws = $("#gce-operations").data("ws-url");
    var measurementsCtx = document.getElementById("measurements");
    var data = loadData({}, {
        name: "-",
        time: "-1",
        insert: 0

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
                removeMeasurementSpinner();
                data = loadData(data, message);
                new Chart(measurementsCtx.getContext("2d")).Line(data, { responsive : true });
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

function loadData(data, message) {
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
            ds_index = -1;
        }
    }
    for(var i = 0; i < data.labels.length; i++) {
        if(data.labels[i] != message.time) {
            continue;
        }
        l_index = i;
        break;
    }
    for(var i = 0; i < data.datasets.length; i++) {
        if(data.datasets[i].label != message.name) {
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
            label: message.name,
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
    data.datasets[ds_index].data.push(message.insert);
    return data;
}

function removeMeasurementSpinner() {
    var spinner = document.getElementById('spinner');
    if(spinner) {
        var spinner = document.getElementById("spinner");
        spinner.outerHTML = "";
        delete spinner;
    }
}

