// src/api.js

// 后端基础地址，请根据你的实际地址修改
const BASE_URL = 'http://localhost:2800';

/**
 * 通用请求函数
 * @param {string} path - 请求路径（会拼接 BASE_URL）
 * @param {object} options
 * @param {string} [options.method=GET]
 * @param {object|FormData|Blob|string} [options.body] - 支持 JSON、FormData、Blob、字符串
 * @param {object} [options.headers] - 自定义头部
 */
async function request(path, { method = 'GET', body, headers = {}} = {}) {
    const opts = {
        method,
        credentials: 'include',  // 自动带上跨域 Session Cookie
        headers: {},
    };

    // 根据 body 类型来设置 headers 和 body
    if (body instanceof FormData) {
        // 上传表单或文件时，让浏览器自动设置 multipart/form-data
        opts.body = body;
    } else if (body instanceof Blob || typeof body === 'string') {
        // 二进制或纯文本
        opts.body = body;
        opts.headers['Content-Type'] = 'application/octet-stream';
    } else if (body != null) {
        // 其他当作 JSON 处理
        opts.body = JSON.stringify(body);
        opts.headers['Content-Type'] = 'application/json';
    }

    // 合并用户传入的 headers（覆盖默认）
    opts.headers = { ...opts.headers, ...headers };

    const res = await fetch(`${BASE_URL}${path}`, opts);

    // 全局错误处理
    if (!res.ok) {
        let errMsg;
        try {
            const err = await res.json();
            errMsg = err.msg || err.error || res.statusText;
        } catch {
            errMsg = res.statusText;
        }
        throw new Error(errMsg);
    }

    // 根据响应 Content-Type 决定是否解析 JSON
    const ct = res.headers.get('Content-Type') || '';
    if (ct.includes('application/json')) {
        return res.json();
    }
    return null;
}

export default {
    get:    (path, body)       => request(path, { method: 'GET', body}),
    post:   (path, body) => request(path, { method: 'POST', body}),
    put:    (path, body) => request(path, { method: 'PUT', body}),
    delete: (path)       => request(path, { method: 'DELETE'}),
};
