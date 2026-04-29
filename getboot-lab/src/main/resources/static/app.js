const state = {
    capabilities: []
};

const checks = {
    trace: {
        method: "GET",
        url: "/api/lab/checks/trace",
        body: null
    },
    web: {
        method: "GET",
        url: "/api/lab/checks/web-success",
        body: null
    },
    echo: {
        method: "POST",
        url: "/api/lab/checks/echo",
        body: {
            content: "GetBoot 验证台请求回显",
            attributes: {
                source: "console",
                manual: true
            }
        }
    },
    exception: {
        method: "POST",
        url: "/api/lab/checks/business-exception",
        body: null
    }
};

document.addEventListener("DOMContentLoaded", () => {
    bindEvents();
    loadOverview();
});

function bindEvents() {
    document.getElementById("refreshBtn").addEventListener("click", loadOverview);
    document.getElementById("traceBtn").addEventListener("click", () => runCheck("trace"));
    document.getElementById("clearBtn").addEventListener("click", clearResponse);
    document.getElementById("categoryFilter").addEventListener("change", renderCapabilities);
    document.querySelectorAll(".check-card").forEach((button) => {
        button.addEventListener("click", () => runCheck(button.dataset.check));
    });
    document.querySelectorAll(".nav-item").forEach((button) => {
        button.addEventListener("click", () => scrollToSection(button));
    });
}

async function loadOverview() {
    try {
        const result = await request("/api/lab/overview", "GET");
        const data = result.body.data;
        state.capabilities = data.capabilities;
        document.getElementById("appName").textContent = data.application;
        document.getElementById("profiles").textContent = data.profiles.join(", ");
        document.getElementById("capabilityCount").textContent = data.capabilities.length;
        document.getElementById("readyCount").textContent = data.capabilities.filter((item) => item.status === "可手测").length;
        renderCategoryFilter();
        renderCapabilities();
        showToast("能力清单已刷新");
    } catch (error) {
        showResult({error: error.message});
        showToast("刷新失败");
    }
}

function renderCategoryFilter() {
    const filter = document.getElementById("categoryFilter");
    const currentValue = filter.value || "all";
    const categories = Array.from(new Set(state.capabilities.map((item) => item.category)));
    filter.innerHTML = `<option value="all">全部分组</option>${categories.map((category) => {
        return `<option value="${escapeHtml(category)}">${escapeHtml(category)}</option>`;
    }).join("")}`;
    filter.value = categories.includes(currentValue) ? currentValue : "all";
}

function renderCapabilities() {
    const filterValue = document.getElementById("categoryFilter").value;
    const rows = state.capabilities
        .filter((item) => filterValue === "all" || item.category === filterValue)
        .map((item) => {
            const tagClass = item.status === "可手测" ? "ready" : "wait";
            const action = item.actionPath
                ? `<button class="table-action" data-path="${escapeHtml(item.actionPath)}">执行验证</button>`
                : `<button class="table-action" disabled>等待接入</button>`;
            return `
                <tr>
                    <td>
                        <div class="module-name">${escapeHtml(item.module)}</div>
                        <div class="desc">${escapeHtml(item.description)}</div>
                    </td>
                    <td>${escapeHtml(item.name)}</td>
                    <td>${escapeHtml(item.category)}</td>
                    <td><span class="tag ${tagClass}">${escapeHtml(item.status)}</span></td>
                    <td>${escapeHtml(item.dependency)}</td>
                    <td>${action}</td>
                </tr>
            `;
        })
        .join("");
    document.getElementById("capabilityTable").innerHTML = rows;
    document.querySelectorAll(".table-action[data-path]").forEach((button) => {
        button.addEventListener("click", () => runPath(button.dataset.path));
    });
}

async function runCheck(name) {
    const check = checks[name];
    if (!check) {
        showToast("验证项不存在");
        return;
    }
    await runRequest(check.url, check.method, check.body);
}

async function runPath(path) {
    const method = path.includes("business-exception") ? "POST" : "GET";
    await runRequest(path, method, null);
}

async function runRequest(url, method, body) {
    try {
        const result = await request(url, method, body);
        showResult(result);
        showToast("验证完成");
        document.getElementById("response").scrollIntoView({behavior: "smooth", block: "start"});
    } catch (error) {
        showResult({error: error.message});
        showToast("请求失败");
    }
}

async function request(url, method, body) {
    const traceId = createTraceId();
    const options = {
        method,
        headers: {
            "X-Trace-Id": traceId
        }
    };
    if (body) {
        options.headers["Content-Type"] = "application/json";
        options.body = JSON.stringify(body);
    }
    const startedAt = performance.now();
    const response = await fetch(url, options);
    const text = await response.text();
    const elapsed = Math.round(performance.now() - startedAt);
    let responseBody;
    try {
        responseBody = text ? JSON.parse(text) : null;
    } catch (error) {
        responseBody = text;
    }
    return {
        request: {
            method,
            url,
            traceId,
            body
        },
        response: {
            status: response.status,
            traceId: response.headers.get("X-Trace-Id"),
            elapsed,
            headers: headersToObject(response.headers)
        },
        body: responseBody
    };
}

function showResult(result) {
    const meta = document.getElementById("responseMeta");
    if (result.error) {
        meta.innerHTML = `<span>请求失败</span>`;
        document.getElementById("responseBody").textContent = JSON.stringify(result, null, 2);
        return;
    }
    meta.innerHTML = `
        <span>${escapeHtml(result.request.method)} ${escapeHtml(result.request.url)}</span>
        <span>HTTP ${result.response.status}</span>
        <span>请求 tid ${escapeHtml(result.request.traceId)}</span>
        <span>响应 tid ${escapeHtml(result.response.traceId || "-")}</span>
        <span>${result.response.elapsed} ms</span>
    `;
    document.getElementById("responseBody").textContent = JSON.stringify(result, null, 2);
}

function clearResponse() {
    document.getElementById("responseMeta").innerHTML = "<span>等待验证</span>";
    document.getElementById("responseBody").textContent = "点击上方按钮开始验证";
}

function headersToObject(headers) {
    const result = {};
    headers.forEach((value, key) => {
        result[key] = value;
    });
    return result;
}

function createTraceId() {
    return Math.random().toString(36).slice(2, 10) + Math.random().toString(36).slice(2, 6);
}

function scrollToSection(button) {
    document.querySelectorAll(".nav-item").forEach((item) => item.classList.remove("active"));
    button.classList.add("active");
    document.getElementById(button.dataset.target).scrollIntoView({behavior: "smooth", block: "start"});
}

function showToast(message) {
    const toast = document.getElementById("toast");
    toast.textContent = message;
    toast.classList.add("show");
    window.clearTimeout(showToast.timer);
    showToast.timer = window.setTimeout(() => toast.classList.remove("show"), 1600);
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}
