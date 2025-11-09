import { useState } from "react"
import { useNavigate, Link } from "react-router-dom"
import { useAuth } from "../contexts/AuthContext"

const Register = () => {
  const [username, setUsername] = useState("")
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [error, setError] = useState("")
  const [loading, setLoading] = useState(false)
  const { register } = useAuth()
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError("")
    setLoading(true)

    try {
      await register(username, email, password)
      navigate("/chat-rooms")
    } catch (err) {
      setError(err.response?.data?.error || "회원가입에 실패했습니다.")
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={styles.container}>
      <div style={styles.content}>
        <div style={styles.logo}>
          <div style={styles.logoIcon}>💬</div>
        </div>
        <div style={styles.card}>
          <h1 style={styles.title}>회원가입</h1>
          <p style={styles.subtitle}>새로운 계정을 만들어 시작하세요</p>

          {error && <div style={styles.error}>{error}</div>}

          <form onSubmit={handleSubmit} style={styles.form}>
            <div style={styles.formGroup}>
              <label style={styles.label}>사용자명</label>
              <input
                type="text"
                placeholder="사용자명을 입력하세요"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
                style={styles.input}
              />
            </div>

            <div style={styles.formGroup}>
              <label style={styles.label}>이메일 (선택)</label>
              <input
                type="email"
                placeholder="이메일을 입력하세요"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                style={styles.input}
              />
            </div>

            <div style={styles.formGroup}>
              <label style={styles.label}>비밀번호</label>
              <input
                type="password"
                placeholder="비밀번호를 입력하세요 (최소 4자)"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                minLength={4}
                style={styles.input}
              />
            </div>

            <button type="submit" disabled={loading} style={styles.button}>
              {loading ? "가입 중..." : "회원가입"}
            </button>
          </form>

          <div style={styles.divider}></div>

          <Link to="/login" style={styles.link}>
            이미 계정이 있으신가요? <span style={styles.linkText}>로그인</span>
          </Link>
        </div>
      </div>
    </div>
  )
}

const styles = {
  container: {
    display: "flex",
    justifyContent: "center",
    alignItems: "center",
    minHeight: "100vh",
    padding: "20px",
  },
  content: {
    width: "100%",
    maxWidth: "400px",
  },
  logo: {
    textAlign: "center",
    marginBottom: "30px",
  },
  logoIcon: {
    fontSize: "48px",
    display: "inline-block",
  },
  card: {
    backgroundColor: "rgba(26, 31, 46, 0.8)",
    backdropFilter: "blur(10px)",
    padding: "40px",
    borderRadius: "12px",
    border: "1px solid rgba(59, 130, 246, 0.2)",
    boxShadow: "0 20px 60px rgba(0, 0, 0, 0.3)",
  },
  title: {
    fontSize: "28px",
    fontWeight: "700",
    marginBottom: "10px",
    color: "#ffffff",
    textAlign: "center",
  },
  subtitle: {
    fontSize: "14px",
    color: "#a0a0a0",
    textAlign: "center",
    marginBottom: "25px",
  },
  form: {
    display: "flex",
    flexDirection: "column",
    gap: "18px",
  },
  formGroup: {
    display: "flex",
    flexDirection: "column",
    gap: "8px",
  },
  label: {
    fontSize: "13px",
    fontWeight: "600",
    color: "#b0b0b0",
    textTransform: "uppercase",
    letterSpacing: "0.5px",
  },
  input: {
    padding: "12px 16px",
    border: "1px solid rgba(59, 130, 246, 0.3)",
    borderRadius: "8px",
    fontSize: "14px",
    backgroundColor: "rgba(15, 20, 25, 0.6)",
    color: "#e0e0e0",
    transition: "all 0.3s ease",
    outline: "none",
  },
  button: {
    padding: "12px 24px",
    backgroundColor: "linear-gradient(135deg, #10b981 0%, #059669 100%)",
    color: "white",
    border: "none",
    borderRadius: "8px",
    fontSize: "14px",
    fontWeight: "600",
    cursor: "pointer",
    marginTop: "10px",
    transition: "all 0.3s ease",
    background: "linear-gradient(135deg, #10b981 0%, #059669 100%)",
  },
  error: {
    color: "#ff6b6b",
    marginBottom: "15px",
    padding: "12px",
    backgroundColor: "rgba(255, 107, 107, 0.1)",
    borderRadius: "8px",
    fontSize: "13px",
    border: "1px solid rgba(255, 107, 107, 0.3)",
  },
  divider: {
    height: "1px",
    backgroundColor: "rgba(59, 130, 246, 0.1)",
    margin: "24px 0",
  },
  link: {
    display: "block",
    textAlign: "center",
    color: "#a0a0a0",
    textDecoration: "none",
    fontSize: "13px",
  },
  linkText: {
    color: "#3b82f6",
    fontWeight: "600",
  },
}

export default Register
