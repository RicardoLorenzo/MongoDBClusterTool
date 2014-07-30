
function getOperations() {
    var ws = new WebSocket("ws://localhost:9000/gce/socket/operations")
    //var ws = $("#gce-operations").data("ws-url");
    var lastOperationDate = null;

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
        // Not implemented
    };

    setInterval(function() {
            var operations = document.getElementById("operations");
            var message = {
                action: "retrieve"
            };
            if((lastOperationDate != null) && (operations.hasChildNodes())) {
                message.lastOperationDate = new Date(lastOperationDate).toISOString();
            }
            ws.send(JSON.stringify(message))
            lastOperationDate = new Date().toISOString();
        }, 2000);
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

function graphInstancesData() {
    var nodesData, runningData;
    var num_shards = 0, num_running_shards = 0, num_config = 0, num_running_config = 0, num_puppet = 0, num_running_puppet = 0, a = 0;

    window.onload = $.ajax({
          dataType: "json",
          url: '/gce/ws/instances',
          success: function(data) {
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
                nodesData = [
                    {
                        value: num_shards,
                        color:"#F7464A",
                        highlight: "#FF5A5E",
                        label: "Shards"
                    },
                    {
                        value: num_config,
                        color: "#46BFBD",
                        highlight: "#5AD3D1",
                        label: "Config"
                    },
                    {
                        value: num_puppet,
                        color: "#FDB45C",
                        highlight: "#FFC870",
                        label: "Puppet"
                    }

                ];
                runningData = {
                    labels : ["Shard nodes","Config nodes","Puppet nodes"],
                    datasets : [
                        {
                            fillColor : "rgba(220,220,220,0.5)",
                            strokeColor : "rgba(220,220,220,0.8)",
                            highlightFill: "rgba(220,220,220,0.75)",
                            highlightStroke: "rgba(220,220,220,1)",
                            data : [num_shards, num_config, num_puppet]
                        },
                        {
                            fillColor : "rgba(151,187,205,0.5)",
                            strokeColor : "rgba(151,187,205,0.8)",
                            highlightFill : "rgba(151,187,205,0.75)",
                            highlightStroke : "rgba(151,187,205,1)",
                            data : [num_running_shards, num_running_config, num_running_puppet]
                        }
                    ]
                }

                var nodectx = document.getElementById("nodes");
                if(nodectx) {
                    window.nodeData = new Chart(nodectx.getContext("2d")).Pie(nodesData);
                }

                var runningctx = document.getElementById("running");
                if(runningctx) {
                    window.runningDataChart = new Chart(runningctx.getContext("2d")).Bar(runningData, {
                        responsive : true
                    });
                }
          },
          error: function(data) {
                //None;
          }
    });
}

