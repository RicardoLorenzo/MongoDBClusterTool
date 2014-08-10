var nodeData = new Array();

function getOperations() {
    var ws = new WebSocket("ws://localhost:9000/gce/socket/operations")
    //var ws = $("#gce-operations").data("ws-url");
    var lastOperationDate = null;
    fillNodeData();
    var nodesChart = nv.models.discreteBarChart();
    nv.addGraph(function() {
        nodesChart
            .x(function(d) { return d.label })
            .y(function(d) { return d.value })
            //.staggerLabels(true)
            .tooltips(false)
            .showValues(true);

        nodesChart.yAxis
                    .axisLabel("Nodes")
                    .tickFormat(d3.format(',f'));
        d3.select('#nodesChart svg')
            .datum(nodeData)
            .transition().duration(500)
            .call(nodesChart);
        nv.utils.windowResize(nodesChart.update);
        return nodesChart;
    });

    ws.onopen = function() {
        lastOperationDate = new Date().toISOString();
        var message = {
            action: "retrieve"
        };
        ws.send(JSON.stringify(message));
    };
    ws.onmessage = function(event) {
        var message = JSON.parse(event.data);
        switch(message.type) {
            case "operation":
                addOperationToList(message);
                break;
        }
    };
    ws.onclose = function() {
        removeOperationsSpinner();
    };

    setInterval(function() {
            var operations = document.getElementById("operations");
            var message = {
                action: "retrieve"
            };
            var spinner = document.getElementById('spinner');
            if((lastOperationDate != null) && spinner == null) {
                message.lastOperationDate = new Date(lastOperationDate).toISOString();
            }
            ws.send(JSON.stringify(message))
            lastOperationDate = new Date().toISOString();
            fillNodeData();
            d3.select('#nodesChart svg')
                .datum(nodeData)
                .transition().duration(500)
                .call(nodesChart);
            nv.utils.windowResize(nodesChart.update);
        }, 4000);
}

function addOperationToList(message) {
    var operations = document.getElementById("operations");

    var progress = document.createElement('div');
    var progressbar = document.createElement('div');
    progress.setAttribute('class', 'progress operations-progress');
    progressbar.setAttribute('class', 'bar');
    progressbar.setAttribute('style', 'width: ' + message.progress + '%');
    progressbar.innerHTML = message.progress + '%';
    progress.appendChild(progressbar);

    var status = document.createElement('span');
    if(message.status == 'DONE') {
        status.setAttribute('class', 'label label-success');
        status.innerHTML = 'done';
    } else if(message.status == 'RUNNING') {
        status.setAttribute('class', 'label label-info');
        status.innerHTML = 'running';
    } else if(message.status == 'PENDING') {
        status.setAttribute('class', 'label label-warning');
        status.innerHTML = 'pending';
    } else {
        status.setAttribute('class', 'label label-important');
        status.innerHTML = message.status;
    }

    var user = document.createElement('p');
    user.setAttribute('class', 'muted');
    user.innerHTML = message.user;

    var date = document.createElement('p');
    date.setAttribute('class', 'muted');
    date.innerHTML = message.date;

    var operation = document.createElement('p');
    operation.setAttribute('class', 'text-info');
    operation.innerHTML = message.optype + ' ' + message.target;

    var item = document.getElementById(message.name);
    if(item == null) {
        item = document.createElement('tr');
        item.setAttribute('id', message.name);
    } else {
        while(item.firstChild) {
            item.removeChild(item.firstChild);
        }
    }

    var col1 = document.createElement('td');
    var col2 = document.createElement('td');
    var col3 = document.createElement('td');
    var col4 = document.createElement('td');
    var col5 = document.createElement('td');
    col1.appendChild(operation);
    col2.appendChild(date);
    col3.appendChild(status);
    col4.appendChild(user);
    col5.appendChild(progress);
    item.appendChild(col1);
    item.appendChild(col2);
    item.appendChild(col3);
    item.appendChild(col4);
    item.appendChild(col5);

    operations.insertBefore(item, operations.firstChild);
}

function fillNodeData() {
    var deferred = $.Deferred();
    createRequest().success(function(data) {
        var nodeData = new Array();
        var num_shards = 0, num_running_shards = 0, num_config = 0, num_running_config = 0,
            num_puppet = 0, num_running_puppet = 0;
        $.each(data.instances, function(key, val) {
            if($.inArray("shard", val.tags) != -1) {
                num_shards++;
                if(val.status == "RUNNING") {
                    num_running_shards++;
                }
            }
            if($.inArray("config", val.tags) != -1) {
                num_config++;
                if(val.status == "RUNNING") {
                    num_running_config++;
                }
            }
            if($.inArray("puppet", val.tags) != -1) {
                num_puppet++;
                if(val.status == "RUNNING") {
                    num_running_puppet++;
                }
            }
        });
        var labelData = new Array();
        labelData.push({
            label: "Shard nodes",
            color: "#cccccc",
            value: num_shards
        });
        labelData.push({
            label: "Running shard nodes",
            color: "#777777",
            value: num_running_shards
        });
        labelData.push({
            label: "Config nodes",
            color: "#cccccc",
            value: num_config
        });
        labelData.push({
            label: "Running config nodes",
            color: "#777777",
            value: num_running_config
        });
        labelData.push({
            label: "Puppet nodes",
            color: "#cccccc",
            value: num_puppet
        });
        labelData.push({
            label: "Running puppet nodes",
            color: "#777777",
            value: num_running_puppet
        });
        nodeData.push({
            key: "-",
            values: labelData
        });
        deferred.resolve(Array.prototype.slice.call(nodeData));
    },
    function() {
        deferred.reject();
    });
    var promise = deferred.promise();
    return promise.done(function(data) {
        nodeData = data;
    });
}

function createRequest() {
    return $.ajax({
        dataType: "json",
        url: '/gce/ws/instances'
    });
}