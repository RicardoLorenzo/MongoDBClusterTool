function uploadFile(url, file, deleteUrl) {
    var xhr = new XMLHttpRequest();
    var fd = new FormData();

    xhr.open("POST", url, true);
    xhr.onreadystatechange = function() {
        if (xhr.readyState == 4 && xhr.status == 200) {
            var response = JSON.parse(xhr.responseText);
            var progress = document.getElementById("progress");
            if(progress) {
                var panel = document.getElementById("files-panel");
                panel.removeChild(progress);
            }
            if(response.result == "ok") {
                var files = document.getElementById("files");
                for(var i in response.files) {
                    var file = document.createElement('tr');
                    var fileName = document.createElement('td');
                    var fileLink = document.createElement('td');
                    var a = document.createElement('a');
                    fileName.innerHTML = response.files[i];
                    a.setAttribute('href', deleteUrl + response.files[i]);
                    a.innerHTML = '<i class="icon-trash"></i>';
                    fileLink.appendChild(a);
                    file.appendChild(fileName);
                    file.appendChild(fileLink);
                    files.appendChild(file);
                }
            } else {
                alert(response.message);
                // TODO Improve the error handling
            }
        }
    };
    xhr.upload.addEventListener("progress", function(e) {
        if(e.lengthComputable) {
            var progress = document.getElementById("progress");
            if(!progress) {
                var panel = document.getElementById("files-panel");
                progress = document.createElement('div');
                var progressbar = document.createElement('div');
                progress.setAttribute('id', 'progress');
                progress.setAttribute('class', 'progress');
                progressbar.setAttribute('id', 'progressbar');
                progressbar.setAttribute('class', 'bar');
                progress.appendChild(progressbar);
                panel.appendChild(progress);
            }
            progress.style.visibility = 'visible';
            var percentComplete = e.loaded / e.total;
            percentComplete = parseInt(percentComplete * 100);
            var progressbar = document.getElementById('progressbar');
            if(progressbar) {
                progressbar.setAttribute('style', 'width: ' + percentComplete + '%');
                progressbar.innerHTML = percentComplete + '%';
            }
            if(percentComplete === 100) {
                // none
            }
        }
    }, false);

    xhr.upload.addEventListener("load", function(e) {
        // none
    }, false);
    fd.append("file", file, file.name);
    // multipart/form-data upload
    xhr.send(fd);
}


function activateUploadDropArea(uploadurl, deleteUrl) {
    var dropzone = document.getElementById("droparea");
    dropzone.ondragover = dropzone.ondragenter = function(event) {
        event.stopPropagation();
        event.preventDefault();
    }

    dropzone.click(function(event) {
        event.stopPropagation();
        event.preventDefault();
        $("#fileselect").trigger('click');
    });

    dropzone.ondrop = function(event) {
        event.stopPropagation();
        event.preventDefault();

        var filesArray = event.dataTransfer.files;
        for (var i = 0; i < filesArray.length; i++) {
            uploadFile(uploadurl, filesArray[i], deleteUrl);
        }
    }
}