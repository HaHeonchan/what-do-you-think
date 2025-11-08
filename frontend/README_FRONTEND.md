# 프론트엔드 가이드

## 프로젝트 구조

```
src/
├── services/
│   └── api.js              # API 통신 설정 및 함수
├── contexts/
│   └── AuthContext.js      # 인증 상태 관리
├── components/
│   └── ProtectedRoute.js   # 인증이 필요한 라우트 보호
├── pages/
│   ├── Login.js            # 로그인 페이지
│   ├── Register.js         # 회원가입 페이지
│   ├── ChatRoomList.js     # 대화방 목록
│   ├── ChatRoomDetail.js   # 대화방 상세 (대화, 노트, 통계)
│   └── CreateChatRoom.js   # 새 대화방 생성
├── App.js                  # 메인 앱 컴포넌트 (라우팅)
└── index.js                # 진입점
```

## 설치 및 실행

```bash
# 의존성 설치
npm install

# 개발 서버 실행
npm start

# 빌드
npm run build
```

## 주요 기능

### 1. 인증
- 로그인/회원가입
- JWT 토큰 자동 관리 (localStorage)
- 인증 상태에 따른 자동 리다이렉트

### 2. 대화방 관리
- 대화방 목록 조회
- 새 대화방 생성
- 대화방 상세 보기

### 3. 대화 기능
- 질문 입력 및 전송
- 대화 기록 실시간 표시
- 대화 라운드 수 설정

### 4. 노트 기능
- 자동 생성된 요약 확인
- 노트 직접 수정 및 저장

### 5. 통계
- 역할별 참여 횟수
- 토큰 사용량
- 생성/수정 날짜

## 코드 특징

- **간결함**: 복잡한 라이브러리 없이 React 기본 기능만 사용
- **인라인 스타일**: CSS 파일 최소화, 컴포넌트 내부에 스타일 정의
- **명확한 구조**: 각 파일이 단일 책임을 가짐
- **쉬운 수정**: 로직이 명확하고 간단하여 수정이 용이

## API 연동

모든 API 호출은 `src/services/api.js`에 정의되어 있습니다.
- axios 인스턴스로 기본 설정 관리
- JWT 토큰 자동 추가
- 401 에러 시 자동 로그아웃

## 환경 설정

백엔드 URL은 `src/services/api.js`의 `API_BASE_URL`에서 변경할 수 있습니다.

```javascript
const API_BASE_URL = 'http://localhost:8080/api';
```

