import { useEffect, useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { apiRequest } from '../api/client.js'
import GoalSelection from '../components/GoalSelection.jsx'

export default function TodayPage() {
    const navigate = useNavigate()
    const [session, setSession] = useState(null)
    const [today, setToday] = useState(null)
    const [concernText, setConcernText] = useState('')
    const [confirmed, setConfirmed] = useState(false)
    const [error, setError] = useState('')
    const [loading, setLoading] = useState(false)

    async function load() {
        const [account, result] = await Promise.all([
            apiRequest('/auth/session'),
            apiRequest('/daily-records/today'),
        ])
        setSession(account)
        setToday(result)
    }

    useEffect(() => {
        load().catch(error => {
            if (error.message === '로그인이 필요합니다.') {
                navigate('/login', { replace: true })
            } else {
                setError(error.message)
            }
        })
    }, [navigate])

    async function generate(event) {
        event.preventDefault()
        if (!window.confirm('작성한 고민으로 오늘의 목표를 생성하시겠습니까?')) return
        setLoading(true)
        setError('')
        try {
            await apiRequest('/daily-records', {
                method: 'POST',
                body: JSON.stringify({ concernText, confirmed }),
            })
            await load()
        } catch (error) {
            setError(error.message)
        } finally {
            setLoading(false)
        }
    }

    async function logout() {
        setLoading(true)
        setError('')
        try {
            await apiRequest('/auth/logout', { method: 'POST' })
            navigate('/', { replace: true })
        } catch (error) {
            setError(error.message)
            setLoading(false)
        }
    }

    if (!session || !today) {
        return <main><h1>오늘의 목표</h1><p role="status">조회 중</p>
            {error && <p role="alert">{error}</p>}</main>
    }
    if (!session.authenticated) return <Navigate to="/login" replace />

    return (
        <main>
            <header>
                <h1>오늘의 목표</h1>
                <p>{session.member.authProvider === 'GOOGLE'
                    ? 'Google 회원'
                    : session.member.loginId}님, 로그인하셨습니다.</p>
                <button type="button" disabled={loading} onClick={logout}>로그아웃</button>
            </header>

            {today.inProgress && <p role="status">오늘의 목표 생성 진행 중</p>}
            {today.canGenerate && !today.inProgress && (
                <form onSubmit={generate}>
                    <p>
                        <label htmlFor="concernText">오늘의 고민</label>
                        <textarea id="concernText" name="concernText" rows={6}
                            value={concernText} maxLength={1000} required
                            aria-describedby="concernHelp"
                            onChange={event => setConcernText(event.target.value)} />
                        <small id="concernHelp">공백 제외 10자 이상, 전체 1000자 이하</small>
                    </p>
                    <p>
                        <label>
                            <input type="checkbox" checked={confirmed} required
                                onChange={event => setConfirmed(event.target.checked)} />
                            입력한 고민을 최종 확인함
                        </label>
                    </p>
                    <button type="submit" disabled={loading}>
                        {loading ? '목표 생성 중' : '목표 3개 생성'}
                    </button>
                </form>
            )}

            {today.record && (
                <section>
                    <h2>오늘 생성된 실천 목표</h2>
                    <p>상태: {today.record.status}</p>
                    {today.record.status === 'CHOICE_PENDING' ? (
                        <GoalSelection record={today.record} onSaved={load} />
                    ) : (
                        <ol>
                            {today.record.goals.map(goal => (
                                <li key={goal.id}>
                                    <h3>{goal.title}</h3>
                                    <p>{goal.actionText}</p>
                                </li>
                            ))}
                        </ol>
                    )}
                    <p>
                        <Link to={'/records/' + today.record.id}>기록 상세 보기</Link>
                    </p>
                </section>
            )}

            {!today.canGenerate && !today.inProgress && !today.record &&
                <p>오늘 목표를 이미 생성하여 재생성할 수 없습니다.</p>}
            {error && <p role="alert">{error}</p>}
            <p><Link to="/">처음으로</Link></p>
        </main>
    )
}