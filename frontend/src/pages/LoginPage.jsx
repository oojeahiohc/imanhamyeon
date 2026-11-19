import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { apiRequest } from '../api/client.js'

export default function LoginPage() {
    const navigate = useNavigate()
    const location = useLocation()
    const [loginId, setLoginId] = useState('')
    const [password, setPassword] = useState('')
    const [error, setError] = useState('')
    const [loading, setLoading] = useState(false)

    useEffect(() => {
        apiRequest('/auth/session')
            .then(({ authenticated, googleSignupPending }) => {
                if (authenticated) navigate('/today', { replace: true })
                else if (googleSignupPending) navigate('/google/consent', { replace: true })
            })
            .catch(() => { })
    }, [navigate])

    async function handleLogin(event) {
        event.preventDefault()
        setError('')
        setLoading(true)
        try {
            await apiRequest('/auth/login', {
                method: 'POST',
                body: JSON.stringify({ loginId, password }),
            })
            navigate('/today', { replace: true })
        } catch (error) {
            setError(error.message)
        } finally {
            setLoading(false)
        }
    }

    async function handleGoogleLogin() {
        setError('')
        setLoading(true)
        try {
            const { authorizationUrl } = await apiRequest('/auth/google/start', {
                method: 'POST',
            })
            window.location.assign(authorizationUrl)
        } catch (error) {
            setError(error.message)
            setLoading(false)
        }
    }

    const googleError = new URLSearchParams(location.search).get('googleError')

    return (
        <main>
            <h1>로그인</h1>
            {location.state?.message && <p role="status">{location.state.message}</p>}
            {googleError && <p role="alert">Google 인증에 실패했습니다. 다시 시도해 주세요.</p>}

            <section aria-labelledby="localLoginTitle">
                <h2 id="localLoginTitle">일반 로그인</h2>
                <form onSubmit={handleLogin}>
                    <p>
                        <label htmlFor="loginId">아이디</label>
                        <input id="loginId" name="loginId" autoComplete="username"
                            required value={loginId} onChange={event => setLoginId(event.target.value)} />
                    </p>
                    <p>
                        <label htmlFor="password">비밀번호</label>
                        <input id="password" name="password" type="password"
                            autoComplete="current-password" required value={password}
                            onChange={event => setPassword(event.target.value)} />
                    </p>
                    <button type="submit" disabled={loading}>
                        {loading ? '처리 중' : '로그인'}
                    </button>
                </form>
            </section>

            <section aria-labelledby="googleLoginTitle">
                <h2 id="googleLoginTitle">Google 로그인</h2>
                <button type="button" disabled={loading} onClick={handleGoogleLogin}>
                    {loading ? '처리 중' : 'Google 계정으로 로그인'}
                </button>
            </section>

            {error && <p role="alert">{error}</p>}
            <p><Link to="/register">회원가입</Link></p>
            <p><Link to="/">처음으로</Link></p>
        </main>
    )
}