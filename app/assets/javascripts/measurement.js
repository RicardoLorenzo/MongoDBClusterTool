
function getMeasurements() {
    var ws = new WebSocket("ws://localhost:9000/test/socket/measurements")
    //var ws = $("#gce-operations").data("ws-url");

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

function removeMeasurementSpinner() {
    var spinner = document.getElementById('spinner');
    if(spinner) {
        var spinner = document.getElementById("spinner");
        spinner.outerHTML = "";
        delete spinner;
    }
}

