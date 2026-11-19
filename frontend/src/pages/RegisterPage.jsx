import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { apiRequest } from '../api/client.js'

export default function RegisterPage() {
    const navigate = useNavigate()
    const [loginId, setLoginId] = useState('')
    const [password, setPassword] = useState('')
    const [privacyConsent, setPrivacyConsent] = useState(false)
    const [aiConsent, setAiConsent] = useState(false)
    const [error, setError] = useState('')
    const [loading, setLoading] = useState(false)

    useEffect(() => {
        apiRequest('/auth/session')
            .then(({ authenticated }) => {
                if (authenticated) navigate('/today', { replace: true })
            })
            .catch(() => { })
    }, [navigate])

    async function handleSubmit(event) {
        event.preventDefault()
        setError('')
        setLoading(true)

        try {
            await apiRequest('/auth/register', {
                method: 'POST',
                body: JSON.stringify({ loginId, password, privacyConsent, aiConsent }),
            })
            navigate('/login', {
                replace: true,
                state: { message: '회원가입이 완료되었습니다. 로그인해 주세요.' },
            })
        } catch (error) {
            setError(error.message)
        } finally {
            setLoading(false)
        }
    }

    return (
        <main>
            <h1>회원가입</h1>
            <form onSubmit={handleSubmit}>
                <p>
                    <label htmlFor="registerId">아이디</label>
                    <input id="registerId" name="loginId" autoComplete="username"
                        required minLength={4} maxLength={20} pattern="[a-z0-9_]{4,20}"
                        aria-describedby="idHelp" value={loginId}
                        onChange={event => setLoginId(event.target.value)} />
                    <small id="idHelp">영문 소문자, 숫자, 밑줄 4~20자</small>
                </p>
                <p>
                    <label htmlFor="registerPassword">비밀번호</label>
                    <input id="registerPassword" name="password" type="password"
                        autoComplete="new-password" required minLength={8} maxLength={64}
                        aria-describedby="passwordHelp" value={password}
                        onChange={event => setPassword(event.target.value)} />
                    <small id="passwordHelp">공백 없는 영문, 숫자, 특수문자 8~64자</small>
                </p>
                <fieldset>
                    <legend>필수 동의</legend>
                    <p>
                        <label>
                            <input type="checkbox" name="privacyConsent" required
                                checked={privacyConsent}
                                onChange={event => setPrivacyConsent(event.target.checked)} />
                            개인정보 수집 및 저장 동의
                        </label>
                    </p>
                    <p>
                        <label>
                            <input type="checkbox" name="aiConsent" required
                                checked={aiConsent}
                                onChange={event => setAiConsent(event.target.checked)} />
                            고민 내용의 외부 AI API 전송 동의
                        </label>
                    </p>
                </fieldset>
                <p>비밀번호를 잊으면 현재 서비스에서 복구할 수 없습니다.</p>
                <button type="submit" disabled={loading}>
                    {loading ? '가입 처리 중' : '가입하기'}
                </button>
            </form>
            {error && <p role="alert">{error}</p>}
            <p><Link to="/login">로그인</Link></p>
        </main>
    )
}