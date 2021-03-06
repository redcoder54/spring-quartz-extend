const schedNameList = document.querySelector("#schedNames");
const jobTbody = document.querySelector(".job-tbody");

loadSchedNameList();
schedNameList.addEventListener("click", loadJobTable);

function loadSchedNameList() {
    let xhr = new XMLHttpRequest();
    let url = baseUrl + '/api/job/sched-names';
    xhr.open("get", url);
    xhr.withCredentials = true;
    xhr.send();
    xhr.onload = function () {
        let result = JSON.parse(xhr.responseText);
        let status = result.status;
        if (status === 0) {
            let data = result.data;
            for (let i = 0; i < data.length; i++) {
                let optionEle = document.createElement("option");
                optionEle.textContent = data[i];
                schedNameList.appendChild(optionEle);
            }
        } else {
            handleError(result);
        }
    }
}

function loadJobTable() {
    let index = schedNameList.selectedIndex;
    if (index <= 0) {
        return;
    }

    jobTbody.innerHTML = '';
    let url = baseUrl + '/api/job/list';
    let xhr = new XMLHttpRequest();
    let params = "schedName=" + schedNameList.options[index].value;
    url = url + "?" + params
    xhr.open("get", url);
    xhr.withCredentials = true;
    xhr.send();
    xhr.onload = function () {
        let result = JSON.parse(xhr.responseText);
        let status = result.status;
        if (status === 0) {
            let data = result.data;
            for (let i = 0; i < data.length; i++) {
                let model = data[i];
                let tr = document.createElement('tr');

                let jobNameTd = document.createElement("td");
                jobNameTd.textContent = model.jobName;
                tr.appendChild(jobNameTd);

                let jobDescTd = document.createElement("td");
                jobDescTd.textContent = model.jobDesc;
                tr.appendChild(jobDescTd);

                let prevFireTimeTd = document.createElement("td");
                prevFireTimeTd.textContent = model.prevFireTime;
                tr.appendChild(prevFireTimeTd);

                let nextFireTime = document.createElement("td");
                nextFireTime.textContent = model.nextFireTime;
                tr.appendChild(nextFireTime);

                let triggerStateTd = document.createElement("td");
                triggerStateTd.textContent = model.triggerState;
                tr.appendChild(triggerStateTd);

                // ????????????????????????????????????/???????????????
                let operationTd = document.createElement("td");
                // ????????????
                let removeBtn = document.createElement("button");
                removeBtn.textContent = '??????';
                removeBtn.onclick = function () {
                    removeJob(model, tr);
                };
                // ??????/????????????
                let pauseOrResumeBtn = document.createElement("button");
                if (model.triggerState === 'PAUSED') {
                    pauseOrResumeBtn.textContent = '??????';
                } else {
                    pauseOrResumeBtn.textContent = '??????';
                }
                pauseOrResumeBtn.onclick = function () {
                    pauseOrResumeJob(pauseOrResumeBtn, model, jobNameTd, jobDescTd, prevFireTimeTd, nextFireTime, triggerStateTd);
                };
                // ????????????
                let deleteBtn = document.createElement("button");
                deleteBtn.textContent = '??????';
                deleteBtn.onclick = function () {
                    deleteJob(model, tr);
                };

                operationTd.appendChild(removeBtn);
                operationTd.appendChild(pauseOrResumeBtn);
                operationTd.appendChild(deleteBtn);
                tr.appendChild(operationTd);

                jobTbody.appendChild(tr);
            }
        } else {
            handleError(result);
        }
    }
}

// ??????job
function removeJob(model, tr) {
    let url = baseUrl + "/api/job/removeLocal"
    let xhr = new XMLHttpRequest();
    xhr.open("delete", url);
    xhr.withCredentials = true;
    let body = {
        'schedName': model.schedName,
        'triggerName': model.triggerName,
        'triggerGroup': model.triggerGroup
    }
    xhr.setRequestHeader('Content-Type', 'application/json');
    xhr.send(JSON.stringify(body));
    xhr.onload = function () {
        let result = JSON.parse(xhr.responseText);
        let status = result.status;
        if (status === 0 && result.data) {
            tr.remove();
        } else {
            handleError(result);
        }
    }
}

// ???????????????job
function pauseOrResumeJob(pauseOrResumeBtn, model, jobNameTd, jobDescTd, prevFireTimeTd, nextFireTime, triggerStateTd) {
    let url;
    if (pauseOrResumeBtn.textContent === "??????") {
        url = baseUrl + '/api/job/pause';
    } else {
        url = baseUrl + '/api/job/resume';
    }
    let xhr = new XMLHttpRequest();
    xhr.open("post", url);
    xhr.withCredentials = true;
    let body = {
        'schedName': model.schedName,
        'jobName': model.jobName,
        'jobGroup': model.jobGroup
    }
    xhr.setRequestHeader('Content-Type', 'application/json');
    xhr.send(JSON.stringify(body));
    xhr.onload = function () {
        let result = JSON.parse(xhr.responseText);
        let status = result.status;
        if (status === 0) {
            if (pauseOrResumeBtn.textContent === '??????') {
                pauseOrResumeBtn.textContent = '??????';
            } else {
                pauseOrResumeBtn.textContent = '??????';
            }
            // ????????????job??????
            refreshJob(model, jobNameTd, jobDescTd, prevFireTimeTd, nextFireTime, triggerStateTd)
        } else {
            handleError(result);
        }
    }
}

// ??????job
function refreshJob(model, jobNameTd, jobDescTd, prevFireTimeTd, nextFireTime, triggerStateTd) {
    let url = baseUrl + '/api/job/refresh';
    let xhr = new XMLHttpRequest();
    xhr.open("post", url);
    xhr.withCredentials = true;
    let body = {
        'schedName': model.schedName,
        'triggerName': model.triggerName,
        'triggerGroup': model.triggerGroup
    }
    xhr.setRequestHeader('Content-Type', 'application/json');
    xhr.send(JSON.stringify(body));
    xhr.onload = function () {
        let result = JSON.parse(xhr.responseText);
        let status = result.status;
        if (status === 0) {
            let model = result.data;
            jobNameTd.textContent = model.jobName;
            jobDescTd.textContent = model.jobDesc;
            prevFireTimeTd.textContent = model.prevFireTime;
            nextFireTime.textContent = model.nextFireTime;
            triggerStateTd.textContent = model.triggerState;
        } else {
            handleError(result);
        }
    }
}

// ??????job
function deleteJob(model, tr) {
    let url = baseUrl + '/api/job/delete';
    let xhr = new XMLHttpRequest();
    xhr.open("post", url);
    xhr.withCredentials = true;
    let body = {
        'schedName': model.schedName,
        'jobName': model.jobName,
        'jobGroup': model.jobGroup
    }
    xhr.setRequestHeader('Content-Type', 'application/json');
    xhr.send(JSON.stringify(body));
    xhr.onload = function () {
        let result = JSON.parse(xhr.responseText);
        let status = result.status;
        if (status === 0) {
            tr.remove();
        } else {
            handleError(result);
        }
    }
}