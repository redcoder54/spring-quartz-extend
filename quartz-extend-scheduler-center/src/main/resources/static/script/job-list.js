const schedNameList = document.querySelector("#schedNames");
const jobTbody = document.querySelector(".job-tbody");

loadJobTable();
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
    jobTbody.innerHTML = '';

    let url = baseUrl + '/api/job/list';
    let xhr = new XMLHttpRequest();
    let index = schedNameList.selectedIndex;
    if (index > 0) {
        let params = "schedName=" + schedNameList.options[index].value;
        url = url + "?" + params
    }
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

                let schedNameTd = document.createElement("td");
                schedNameTd.textContent = model.schedName;
                tr.appendChild(schedNameTd);

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

                let updateTimeTd = document.createElement("td");
                updateTimeTd.textContent = model.updateTime;
                tr.appendChild(updateTimeTd);

                // ????????????button??????????????????
                let operationTd = document.createElement("td");
                // ????????????
                let triggerJobBtn = document.createElement("button");
                triggerJobBtn.textContent = '??????';
                triggerJobBtn.onclick = function () {
                    triggerJob(model);
                };
                // ????????????
                let refreshBtn = document.createElement("button");
                refreshBtn.textContent = '??????';
                refreshBtn.onclick = function () {
                    refreshJob(model, jobNameTd, jobDescTd, prevFireTimeTd, nextFireTime, triggerStateTd);
                };

                operationTd.appendChild(triggerJobBtn);
                operationTd.appendChild(refreshBtn);
                tr.appendChild(operationTd);

                jobTbody.appendChild(tr);
            }
        } else {
            handleError(result);
        }
    }
}

// ??????job
function triggerJob(model) {
    let url = baseUrl + '/api/job/trigger';
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
        let success = result.data;
        if (status === 0 && success) {
            alert('??????????????????');
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