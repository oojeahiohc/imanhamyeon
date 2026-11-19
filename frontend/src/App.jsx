import { BrowserRouter, Route, Routes } from 'react-router-dom'
import LandingPage from './pages/LandingPage.jsx'
import LoginPage from './pages/LoginPage.jsx'
import RegisterPage from './pages/RegisterPage.jsx'
import GoogleConsentPage from './pages/GoogleConsentPage.jsx'
import TodayPage from './pages/TodayPage.jsx'
import RecordDetailPage from './pages/RecordDetailPage.jsx'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/google/consent" element={<GoogleConsentPage />} />
        <Route path="/today" element={<TodayPage />} />
        <Route path="/records/:id" element={<RecordDetailPage />} />
      </Routes>
    </BrowserRouter>
  )
}