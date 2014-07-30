function activatePuppetFileSelect() {
    var fileName = document.getElementById('fileName');
    fileName.onchange = function() {
        getPuppetManifest(fileName.value);
    }
    if(fileName.value) {
        getPuppetManifest(fileName.value);
    }
}

function getPuppetManifest(fileName) {
    if(fileName) {
        $.ajax({
            beforeSend: function() {
                showSpinner();
            },
            complete: function() {
                hideSpinner();
            },
            dataType: "text",
            type: 'GET',
            data: { fileName: fileName },
            url: '/gce/ws/manifest',
            success: function(data) {
                var fileContent = document.getElementById('fileContent');
                fileContent.value = data;
            },
            error: function(data) {
                //None;
            }
        });
    } else {
        var fileContent = document.getElementById('fileContent');
        fileContent.value = "";
    }
}

function showSpinner() {
    var spinner = document.getElementById('spinner');
    if(spinner) {
        spinner.style.visibility = 'visible';
    }
}

function hideSpinner() {
    var spinner = document.getElementById('spinner');
    if(spinner) {
        spinner.style.visibility = 'hidden';
    }
}