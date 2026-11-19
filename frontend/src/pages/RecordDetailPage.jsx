import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { apiRequest } from '../api/client.js'
import GoalSelection from '../components/GoalSelection.jsx'

export default function RecordDetailPage() {
    const { id } = useParams()
    const [record, setRecord] = useState(null)
    const [error, setError] = useState('')

    async function load() {
        setRecord(await apiRequest('/daily-records/' + id))
    }

    useEffect(() => {
        setRecord(null)
        setError('')
        load().catch(error => setError(error.message))
    }, [id])

    return (
        <main>
            <h1>실천 기록 상세</h1>
            {!record && !error && <p role="status">조회 중</p>}
            {record && (
                <article>
                    <h2>{record.goalDate}</h2>
                    <p>고민: {record.concernText}</p>
                    <p>상태: {record.status}</p>
                    {record.status === 'CHOICE_PENDING' ? (
                        <GoalSelection record={record} onSaved={load} />
                    ) : (
                        <ol>
                            {record.goals.map(goal => (
                                <li key={goal.id}>{goal.title}: {goal.actionText}</li>
                            ))}
                        </ol>
                    )}
                </article>
            )}
            {error && <p role="alert">{error}</p>}
            <p><Link to="/today">오늘의 목표</Link></p>
        </main>
    )
}