# 외부 장소 데이터 공급자 선정

- 결정일: 2026-10-20
- 상태: 채택 및 3단계 구현 완료
- 대상 단계: 3단계

## 결정

TravelPlanner의 기본 외부 장소 데이터 공급자로 **Google Places API (New)**를 채택한다.

국내 한정 서비스라는 요구사항이 없고 PRD가 사용자가 입력한 도시를 기준으로 일정을 만드는 제품을 정의하므로, 한 공급자로 여러 국가의 장소를 검색할 수 있는 범위를 우선했다. Google Places API는 Text Search, Nearby Search, Place Details를 제공하고 현재 장소 모델에 필요한 식별자, 이름, 주소, 좌표, 유형, 영업시간, 가격 수준을 조회할 수 있다.

API 키와 결제 계정이 필요한 실제 연동은 3단계에서 진행한다. 이번 결정에는 외부 API 호출이나 과금이 포함되지 않는다.

## 비교 결과

| 후보 | 장점 | 제약 | 판단 |
|---|---|---|---|
| Google Places API (New) | 여러 국가의 장소 검색, 좌표·영업시간·가격·유형 제공, Routes API와 식별자 연계 가능 | 종량제 과금, 결제 계정과 키 필요, 필드 마스크에 따라 과금 등급이 달라짐 | 채택 |
| 한국관광공사 국문 관광정보 API | 국내 관광 데이터 약 26만 건, 실시간 갱신, 무료, 관광지·숙박·행사·이미지 제공 | 국내 데이터에 한정, 개발계정 일 1,000건, 운영계정 심의 필요 | 국내 콘텐츠 보강 후보 |
| Kakao Local API | 국내 키워드·카테고리 검색과 좌표 조회에 적합, 무료 쿼터 제공 | 응답의 기본 장소 정보가 이름·주소·좌표·카테고리 중심이라 현재 일정 생성에 필요한 영업시간·가격을 한 번에 채우기 어려움 | 국내 검색 보조 후보 |
| NAVER 지역 검색 API | 국내 업체와 상호 검색에 강점 | 2026년 API HUB 이관 일정과 검색 결과 저장·가공 관련 이용조건을 함께 검토해야 하며, 여행 일정용 원천 데이터로 쓰기에는 제약이 큼 | 제외 |

## 선택 기준

1. 국내외 도시를 같은 방식으로 검색할 수 있어야 한다.
2. 일정 생성에 필요한 좌표와 요일별 영업시간을 제공해야 한다.
3. 장소의 외부 식별자가 안정적이어야 한다.
4. 가격 수준이나 가격 범위를 받아 예산 계산을 개선할 수 있어야 한다.
5. 이후 실제 도로 경로 API와 연결하기 쉬워야 한다.

## 데이터 매핑 초안

| TravelPlanner | Google Places |
|---|---|
| 외부 식별자 | `id` |
| 이름 | `displayName.text` |
| 주소 | `formattedAddress`와 `addressComponents` |
| 위도·경도 | `location.latitude`, `location.longitude` |
| 카테고리 | `primaryType`, `types`를 내부 `PlaceCategory`로 변환 |
| 요일별 영업시간 | `regularOpeningHours.periods` |
| 가격 | `priceLevel`, 제공되는 경우 `priceRange` |

현재 `Place`의 단일 시작·종료 시각은 요일별 또는 자정 이후 영업을 표현할 수 없다. 외부 응답을 이 구조에 억지로 축약하지 않고, 3단계에서 요일별 영업시간 모델을 먼저 추가한 뒤 매핑한다.

## 연동 원칙

- 외부 호출은 `PlaceProvider` 인터페이스 뒤에 둬 공급자를 교체하거나 보조 공급자를 추가할 수 있게 한다.
- API 키는 환경변수나 비밀 저장소에서만 읽고 저장소에 커밋하지 않는다.
- 필요한 필드만 FieldMask로 요청하고 개발 단계에서 일별 쿼터와 예산 알림을 설정한다.
- 외부 ID에 고유 제약을 두어 같은 장소의 중복 적재를 막는다.
- 원본 응답 전체를 장기 저장하지 않고 이용약관에 허용된 필드와 갱신 시각만 저장한다.
- 타임아웃, 429, 5xx에 지수 백오프와 제한된 재시도를 적용한다.
- API 장애 시 이미 저장된 장소로 일정 생성을 계속할 수 있게 한다.

## 공식 근거

- [Google Places API (New) 개요](https://developers.google.com/maps/documentation/places/web-service/overview)
- [Google Places REST API](https://developers.google.com/maps/documentation/places/web-service/reference/rest)
- [Google Places 데이터 필드와 과금 등급](https://developers.google.com/maps/documentation/places/web-service/data-fields)
- [Google Places 사용량과 결제](https://developers.google.com/maps/documentation/places/web-service/usage-and-billing)
- [한국관광공사 국문 관광정보 서비스](https://www.data.go.kr/data/15101578/openapi.do)
- [Kakao Local API](https://developers.kakao.com/docs/latest/ko/local/dev-guide)
- [Kakao API 쿼터](https://developers.kakao.com/docs/latest/ko/getting-started/quota)
- [NAVER 검색 API](https://developers.naver.com/products/service-api/search/search.md)
- [NAVER API HUB 이관 공지](https://developers.naver.com/notice/article/32530)
