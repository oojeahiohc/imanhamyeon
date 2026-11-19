import { useState } from 'react'
import { apiRequest } from '../api/client.js'

export default function GoalSelection({ record, onSaved }) {
    const [chosen, setChosen] = useState([])
    const [error, setError] = useState('')
    const [loading, setLoading] = useState(false)

    function change(goalId, checked) {
        setChosen(current =>
            checked ? [...current, goalId] : current.filter(id => id !== goalId)
        )
    }

    async function submit(event) {
        event.preventDefault()
        setError('')
        setLoading(true)
        try {
            await apiRequest('/daily-records/' + record.id + '/selection', {
                method: 'POST',
                body: JSON.stringify({ goalIds: chosen }),
            })
            await onSaved()
        } catch (error) {
            setError(error.message)
        } finally {
            setLoading(false)
        }
    }

    return (
        <form onSubmit={submit}>
            <fieldset>
                <legend>실천할 목표를 1개에서 3개 선택</legend>
                {record.goals.map(goal => (
                    <p key={goal.id}>
                        <label>
                            <input type="checkbox" checked={chosen.includes(goal.id)}
                                disabled={loading}
                                onChange={event => change(goal.id, event.target.checked)} />
                            {goal.title}: {goal.actionText}
                        </label>
                    </p>
                ))}
            </fieldset>
            <button type="submit" disabled={loading || chosen.length === 0}>
                {loading ? '선택 확정 중' : '선택 확정'}
            </button>
            {error && <p role="alert">{error}</p>}
        </form>
    )
}