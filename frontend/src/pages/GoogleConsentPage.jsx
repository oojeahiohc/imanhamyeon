import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { apiRequest } from '../api/client.js'

export default function GoogleConsentPage() {
    const navigate = useNavigate()
    const [ready, setReady] = useState(false)
    const [privacyConsent, setPrivacyConsent] = useState(false)
    const [aiConsent, setAiConsent] = useState(false)
    const [error, setError] = useState('')
    const [loading, setLoading] = useState(false)

    useEffect(() => {
        apiRequest('/auth/session')
            .then(({ authenticated, googleSignupPending }) => {
                if (authenticated) {
                    navigate('/today', { replace: true })
                } else if (googleSignupPending) {
                    setReady(true)
                } else {
                    navigate('/login', {
                        replace: true,
                        state: { message: 'Google 인증을 다시 진행해 주세요.' },
                    })
                }
            })
            .catch(() => setError('인증 상태를 확인할 수 없습니다. 다시 접속해 주세요.'))
    }, [navigate])

    async function handleComplete(event) {
        event.preventDefault()
        setError('')
        setLoading(true)
        try {
            await apiRequest('/auth/google/complete', {
                method: 'POST',
                body: JSON.stringify({ privacyConsent, aiConsent }),
            })
            navigate('/today', { replace: true })
        } catch (error) {
            setError(error.message)
            setLoading(false)
        }
    }

    async function handleCancel() {
        setError('')
        setLoading(true)
        try {
            await apiRequest('/auth/google/cancel', { method: 'POST' })
            navigate('/login', {
                replace: true,
                state: { message: 'Google 회원가입이 취소되었습니다.' },
            })
        } catch (error) {
            setError(error.message)
            setLoading(false)
        }
    }

    if (!ready) {
        return (
            <main>
                <h1>Google 회원가입</h1>
                {error
                    ? <p role="alert">{error}</p>
                    : <p role="status">Google 인증 상태 확인 중</p>}
            </main>
        )
    }

    return (
        <main>
            <h1>Google 회원가입</h1>
            <p>Google 계정 인증이 완료되었습니다.</p>
            <p>서비스 이용을 위해 다음 필수 동의 내용을 확인해 주세요.</p>
            <form onSubmit={handleComplete}>
                <fieldset>
                    <legend>필수 동의</legend>
                    <p>
                        <label>
                            <input type="checkbox" required checked={privacyConsent}
                                onChange={event => setPrivacyConsent(event.target.checked)} />
                            개인정보 수집 및 저장 동의
                        </label>
                    </p>
                    <p>
                        <label>
                            <input type="checkbox" required checked={aiConsent}
                                onChange={event => setAiConsent(event.target.checked)} />
                            고민 내용의 외부 AI API 전송 동의
                        </label>
                    </p>
                </fieldset>
                <p>동의하지 않으면 회원 정보가 생성되지 않습니다.</p>
                <button type="submit" disabled={loading}>
                    {loading ? '처리 중' : '동의하고 가입 완료'}
                </button>
            </form>
            <button type="button" disabled={loading} onClick={handleCancel}>
                가입 취소
            </button>
            {error && <p role="alert">{error}</p>}
        </main>
    )
}