export async function apiRequest(path, options = {}) {
    const method = (options.method || 'GET').toUpperCase()
    const headers = { ...options.headers }

    if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
        const session = await apiRequest('/auth/session')
        headers['X-CSRF-TOKEN'] = session.csrfToken
        if (options.body) headers['Content-Type'] = 'application/json'
    }

    const response = await fetch(`/api${path}`, {
        ...options,
        credentials: 'same-origin',
        headers,
    })
    const data = response.status === 204
        ? null
        : await response.json().catch(() => null)

    if (!response.ok) {
        throw new Error(data?.message || '요청을 처리할 수 없습니다.')
    }

    return data
}