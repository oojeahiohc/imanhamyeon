import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiRequest } from '../api/client.js'

export default function LandingPage() {
    const [status, setStatus] = useState('서버 연결 확인 중')

    useEffect(() => {
        apiRequest('/auth/session')
            .then(({ authenticated }) => {
                setStatus(authenticated ? '로그인 상태' : '서버 연결 확인, 현재 비로그인 상태')
            })
            .catch(() => setStatus('서버 연결 실패, 개발 서버 상태 확인 필요'))
    }, [])

    return (
        <>
            <header>
                <h1>이만하면</h1>
                <p>완벽보다는 완성을, 오늘도 시작한 당신 이만하면 충분해요.</p>
            </header>
            <main>
                <section aria-labelledby="intro-heading">
                    <h2 id="intro-heading">오늘의 작은 시작을 만들어 보세요.</h2>
                    <ol>
                        <li>오늘의 고민 작성</li>
                        <li>AI 목표 3개 추천</li>
                        <li>목표 선택 및 실천</li>
                    </ol>
                    <nav aria-label="계정 시작">
                        <Link to="/login">로그인</Link>{' '}
                        <Link to="/register">회원가입</Link>
                    </nav>
                </section>
                <section aria-labelledby="notice-heading">
                    <h2 id="notice-heading">서비스 이용 안내</h2>
                    <p>이 서비스는 의료적 진단이나 치료 목적이 아닌 일상 습관 개선 서비스입니다.</p>
                    <p>회원의 고민은 서비스 제공을 위해 저장되며, AI 목표 생성 시 외부 API로 전송됩니다. 이용 전 관련 안내와 필수 동의 확인이 필요합니다.</p>
                </section>
                <p role="status">{status}</p>
            </main>
            <footer>
                <p>이만하면</p>
            </footer>
        </>
    )
}