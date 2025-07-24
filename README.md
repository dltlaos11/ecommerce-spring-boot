## 프로젝트

## 아키텍처

### E-커머스 STEP05 아키텍처 설계 및 구현 가이드

#### 🎯 선택한 아키텍처: 레이어드 + 인터페이스 (실용적 DIP)

##### 선택 근거

- **실용성과 학습 효과의 균형**: 헥사고날/클린 대비 적절한 복잡도
- **테스트 용이성**: Repository 인터페이스를 통한 Mock 테스트 지원
- **JPA 장점 극대화**: Entity를 도메인 모델로 활용하여 개발 편의성 확보
- **과제 요구사항 부합**: 단위 테스트 + Mock/Stub 활용 가능

#### 🏗️ 아키텍처 구조

##### 의존성 흐름

```
Controller → Facade → Service → Repository(interface) ← RepositoryImpl
                ↓           ↓
           복합도메인    @Entity(JPA 허용)
```

##### DIP 적용 원칙

- **변경 가능성이 있는 것만 인터페이스화**: Repository 레이어
- **변경 가능성이 낮은 것은 구체화**: Service 레이어 (ServiceImpl 패턴 지양)
- **실용적 타협**: JPA 어노테이션을 도메인 모델에 허용

## Getting Started

### Prerequisites

#### Running Docker Containers

`local` profile 로 실행하기 위하여 인프라가 설정되어 있는 Docker 컨테이너를 실행

```bash
docker-compose up -d
```
