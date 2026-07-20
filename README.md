# VIVIZIP

> 외국인 유학생을 위한 AI 기반 부동산 계약 안전 플랫폼입니다.
> VIVIZIP Backend는 Spring Boot 기반 API 서버와 CLOVA OCR + OpenAI 기반 문서 분석 파이프라인으로 구성됩니다.

---
## 📌 Backend Developers
<table>
  <tr>
    <th align="center">Backend</th>
    <th align="center">Backend</th>
  </tr>
  <tr>
    <td align="center"><img src="https://github.com/Seona12.png" width="120"/></td>
    <td align="center"><img src="https://github.com/gyuri.png" width="120"/></td>
  </tr>
  <tr>
    <td align="center"><a href="https://github.com/Seona12">선아</a></td>
    <td align="center"><a href="https://github.com/gyuri">규리</a></td>
  </tr>
  <tr>
    <td valign="top">
      <ul>
        <li>AWS 기반 배포,github CI/CD 환경 구축</li>
        <li>프로젝트 세팅 및 전역 예외 처리 구현</li>
        <li>WebSocket(STOMP) 기반 실시간 채팅</li>
        <li>S3 이미지 업로드</li>
        <li>카카오 로컬 API 연동 장소 검색 기능 구현</li>
        <li>부동산 방문 약속 및 입주 기록 CRUD 기능 구현</li>
        <li>CLOVA OCR 연동 및 좌표,텍스트 추출 API 구현</li>
        <li>등기부등본/임대차계약서 AI 분석 기능 구현</li>
      </ul>
    </td>
    <td valign="top">
      <ul>
        <li>카카오 로그인 구현(JWT, Spring Security)</li>
        <li>학교 이메일 인증 기능 구현</li>
        <li>서포터즈, 학생 온보딩 CRUD API 구현</li>
        <li>매칭 관련 API 구현</li>
        <li>알림 기능 및 FCM 푸시 구현</li>
        <li>공동/개별주택 공시가격 조회 및 보증금 위험비율 계산 로직 구현</li>
        <li>중개대상물 확인·설명서/건축물대장 AI 분석 API 구현</li>
      </ul>
    </td>
  </tr>
</table>

---
## 🚀 Backend Core Features

- 🔐 JWT + 카카오 OAuth2 기반 인증
- 🤝 서포터즈 ↔ 학생 매칭 시스템
- 💬 WebSocket(STOMP) 기반 실시간 채팅 및 읽음 처리
- 🔔 아티클 / 매칭 / 채팅 알림 및 FCM 푸시
- 📄 AI 문서 분석 (등기부등본 / 건축물대장 / 중개대상물확인서 / 임대차계약서)
- 🏠 카카오 로컬 API 기반 부동산 방문 약속(장소) 관리
- 📸 입주 기록(하자 사진 및 코멘트) 관리
- 🚀 Docker 기반 CI/CD 자동 배포

---

## 🏗️ Infrastructure & DevOps

- CI/CD 파이프라인 구축 (GitHub Actions)
- AWS EC2 + Nginx 기반 배포
- Docker / Docker Compose 기반 컨테이너 환경 구성
- Redis 캐시 도입 (세션 / 토큰 관리)
- Swagger 기반 API 문서화

---

## 🧩 Project Architecture
<img width="4122" height="2138" alt="image" src="https://github.com/user-attachments/assets/62531155-d720-4d9b-be0f-77f5ce81dc87" />

---

## 🧱 Stacks

### Backend

**Core Application (API Server)**

![Spring Boot](https://img.shields.io/badge/spring%20boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/spring%20security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)

**AI / 문서 분석 파이프라인**

![OpenAI](https://img.shields.io/badge/OpenAI-412991?style=for-the-badge&logo=openai&logoColor=white)
![CLOVA OCR](https://img.shields.io/badge/CLOVA%20OCR-03C75A?style=for-the-badge&logo=naver&logoColor=white)

### Database & Cache

![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)

### Client

![React Native](https://img.shields.io/badge/React%20Native-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)

### Infra & Deployment

![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Docker Compose](https://img.shields.io/badge/Docker%20Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Nginx](https://img.shields.io/badge/Nginx-009639?style=for-the-badge&logo=nginx&logoColor=white)
![AWS](https://img.shields.io/badge/AWS-232F3E?style=for-the-badge&logo=amazonaws&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub%20Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)

### API Documentation

![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)

### Communication

![Notion](https://img.shields.io/badge/Notion-000000?style=for-the-badge&logo=notion&logoColor=white)
![Figma](https://img.shields.io/badge/Figma-F24E1E?style=for-the-badge&logo=figma&logoColor=white)
---


## 🤝 협업 규칙

### 1) Git 브랜치 전략

- `main` : 배포용 (항상 안정 상태 유지)
- `develop` : 개발 통합 브랜치
- `feature/#이슈번호-기능명` : 기능 개발
- `fix/#이슈번호-버그명` : 버그 수정
- `refactor/#이슈번호-내용` : 리팩토링
- `chore/#이슈번호-내용` : 세팅 및 기타 작업

### 2) 프로젝트 구조 결정

- 도메인형 구조로 구성
  - 도메인 별로 controller / service / repository / dto / entity / pipeline 등을 묶어서 관리
  - 공통(전역) 영역은 common, config, consts 패키지로 분리

### 3) PR 규칙

- PR은 **작게**(기능 단위) 올리기
- PR 템플릿 사용:
  - 관련 이슈 연결: `Closes #이슈번호`
  - 변경 내용 요약 작성
  - 테스트/검증 내용 작성 (가능하면 스크린샷/로그 포함)
  - CodeRabbit AI 코드리뷰 승인 후 머지
  - 머지는 원칙적으로 `Squash and merge` (커밋 히스토리 정리)
