# Pleos Village 디자인 참고 자료 (v4: 파스텔 선화)

시안(`docs/design/*.png`)과 그 소스(`docs/design/mockups.html`)를 실제 화면으로 옮길 때 이 문서를 따른다.
`mockups.html`을 브라우저로 열면 모든 시안이 보인다. 주소 끝에 `?theme=night`를 붙이면 밤 모드가 보인다. 글꼴(Pretendard, Jua)은 인터넷에서 불러온다.

v4 변경: v3(종이와 잉크)의 흔들리는 선, 종이 결, 명조 제목을 없앴다. 모든 그림과 UI를 같은 굵기의 외곽선과 평평한 파스텔 색으로 다시 그렸다. 제목 글꼴은 둥근 글꼴 Jua로 바꿨다.

---

## 1. 스타일 방향

"같은 굵기의 선으로 그린 파스텔 그림책". 그림과 UI가 같은 선, 같은 색 규칙을 쓴다.

| 요소 | 규칙 |
|---|---|
| 선 | 모든 그림과 UI 테두리는 2dp, 색 #3D3B52(짙은 남보라), 끝과 꺾인 곳은 둥글게. 크게 그린 주민(도착 요약)만 3dp |
| 칠 | 평평한 파스텔 색만 쓴다. 둥근 물체에만 작은 밝은 하이라이트 1개를 둔다. 그라데이션, 그림자, 질감을 쓰지 않는다 |
| 색 | 큰 면은 차가운 파스텔(연보라, 하늘, 민트, 연두)이다. 분홍, 버터 노랑, 복숭아는 볼, 꽃, 별 같은 작은 곳에만 쓴다 |
| 글꼴 | 제목, 주민 이름, 말풍선: Jua. 본문과 숫자: Pretendard |
| 카드 | 흰 바탕 + 2dp 외곽선 + 모서리 14~18dp. 그림자를 쓰지 않는다 |
| 버튼 | 주 버튼: #B9C3F7 바탕 + 2dp 외곽선 + 어두운 글자. 보조 버튼: 흰 바탕 + 2dp 외곽선 |
| 선택 표시 | 선택된 메뉴와 탭은 #C9CCF5 바탕 + 외곽선 |
| 주민 | 단순한 기하 모양, 점 눈, 작은 입, 양 볼에 분홍 타원. 종류마다 몸 모양이 다르다 |
| 마을 | 둥근 잔디 섬(비스듬히 내려다봄), 흙 옆면, 크림색 굽은 길, 연못과 작은 배, 울타리, 벤치, 가로등, 꽃밭, 열매 나무. 주민 집은 주민 모양을 따른다 |
| 장식 | 작은 노란 별, 네 갈래 반짝이, 작은 거품, 둥근 물결 모양 구름 |
| 연출 | 작고 짧게. 주민이 통 튀어 오르며 나타나고 반짝이 1~2개가 생긴다. 큰 색종이와 빛줄기는 쓰지 않는다 |

**하지 말 것**: 특정 작가나 게임의 캐릭터, 그림, 구도, 간판 글자, 화면 배치를 따라 그리지 않는다. 그림체의 방향(선, 색, 디테일을 넣는 방식)만 참고한다. 참고 이미지 파일을 앱, 문서, 저장소에 넣지 않는다.

### AI가 만든 느낌을 주는 것, 쓰지 말 것

- 배경에 흐린 색 원(블러 블롭)을 까는 것
- 그라데이션과 부드러운 그림자 카드
- 모든 제목 위에 작은 영문 대문자 라벨을 다는 것
- "NEW" 알약 표시, 이모지
- 기본 파랑, 청록 강조색
- 선 굵기가 그림마다 다른 것

---

## 2. 색

모든 글자는 바탕과 명암비 4.5:1 이상이어야 한다(Pleos 기준). 아래 숫자는 계산한 값이다.

**UI (낮)**

| 토큰 | 값 | 쓰는 곳 | 명암비 |
|---|---|---|---|
| bg | #F6F6FE | 화면 바탕 | - |
| surface2 | #ECEDFB | 서랍, 구역 바탕 | - |
| card | #FFFFFF | 카드, 말풍선 | - |
| ol | #3D3B52 | 모든 외곽선 | - |
| fg | #2E2C42 | 제목, 숫자, 본문 | bg 12.6 / surface2 11.6 / card 13.5 |
| fg2 | #5B5873 | 설명 | bg 6.3 / surface2 5.9 / card 6.8 |
| ok | #2F7A5E | 80점 통과, "만남" | bg 4.8 / card 5.2 (surface2 위에는 쓰지 않는다) |
| warn | #B5533C | 80점 미만, 감점 | bg 4.6 (surface2 위에는 쓰지 않는다) |
| btn / btn-fg | #B9C3F7 / #2E2C42 | 주 버튼 | 7.9 |
| sel | #C9CCF5 | 선택된 메뉴와 탭 바탕 | fg와 8.6 |

**UI (밤, 시스템 야간 모드)**

| 토큰 | 값 | 쓰는 곳 | 명암비 |
|---|---|---|---|
| bg | #2E3157 | 화면 바탕(짙은 보라 밤하늘) | - |
| surface2 | #383C68 | 서랍, 구역 바탕 | - |
| card | #F7F6FE | 카드는 밝게 둔다. 카드 안 글자는 낮과 같다 | #2E2C42와 12.6 |
| fg | #F2F1FF | 바탕 위 제목, 숫자 | bg 11.1 / surface2 9.3 |
| fg2 | #C9C8EA | 바탕 위 설명 | bg 7.6 / surface2 6.4 |
| ok / warn | #9FE0C3 / #FFB7A3 | 바탕 위 상태 글자 | 8.2 / 7.4 |

- 밤에는 그림 색을 바꾸지 않고 하늘(달, 별)만 바꾼다.
- 밤에는 바탕 위에 바로 놓이는 외곽선(못 만난 주민의 점선 등)을 #C9C8EA로 바꾼다. #3D3B52는 밤 바탕에서 거의 보이지 않는다(명암비 1.15).

**그림 색 (낮·밤 같음)**

| 이름 | 값 | 쓰는 곳 |
|---|---|---|
| peri / lav | #C9CCF5 / #B9B3EE | 밤 주민, 지붕, 선택 |
| baby | #BFE3F7 | 비 주민, 물, 하늘색 소품 |
| mint / mint2 | #C8EBD7 / #A8DCC0 | 나무, 목도리, 배낭 |
| sage / sage2 | #D6F0C9 / #BFE3B4 | 잔디 |
| pink / rose | #F9C9DC / #F5A6C2 | 가족 주민, 꽃, 하트 |
| blush | #F6A5C0 | 볼 |
| butter | #FBEAA0 | 별, 달, 번개, 충전 주민 |
| peach | #FAD3B8 | 가족 작은 주민, 충전 주민 |
| cream / earth / earthD | #FFF6E6 / #F6DEC9 / #ECCBB0 | 길, 섬 옆면 |
| sil | #E4E2FA | 못 만난 주민 실루엣(점선 외곽선 + "?") |

**주민별 몸 모양과 우표 안쪽 색**

| 주민 | 몸 모양 | 우표 안쪽 | 집 |
|---|---|---|---|
| 비 | 하늘색 물방울 + 파스텔 우산 | #DDF0FB | 우산 지붕 집 |
| 밤 | 연보라 둥근 몸 + 노란 초승달 | #E6E3FA | 달 지붕 집 |
| 가족 | 분홍 큰 몸 + 복숭아색 작은 몸 + 작은 하트 | #FCE3EC | 하트 지붕 두 채 |
| 겨울 | 흰 눈덩이 + 민트 목도리 | #E6F0FA | 이글루 |
| 장거리 | 민트 긴 콩 모양 + 작은 배낭 | #DFF3E7 | (2차 개발) |
| 충전 | 버터색 둥근 몸 + 번개 머리털 | #FCF2CC | (2차 개발) |

---

## 3. 부품을 Compose로 만드는 방법

| 부품 | 방법 |
|---|---|
| 외곽선 | 모든 그림은 Path를 채운 다음 `drawPath(path, ol, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))`로 선을 그린다. 크기를 바꿔도 선 굵기는 dp로 고정한다 |
| 카드 | `Surface(shape = RoundedCornerShape(16.dp), color = card, border = BorderStroke(2.dp, ol))`, 그림자 0 |
| 주 버튼 / 보조 버튼 | 높이 60~64dp, 모서리 16dp, `BorderStroke(2.dp, ol)`. 주: btn 바탕. 보조: card 바탕 |
| 메뉴, 탭 | 선택된 것만 sel 바탕 + 외곽선 + 모서리 12dp |
| 우표 카드 | 흰 바탕 + 톱니 가장자리(반원 구멍을 반복한 Path) + 안쪽에 주민별 색 |
| 꾸미기 서랍 | 네모 카드(모서리 12dp, 외곽선) 가로 목록. 선택된 카드는 sel 바탕 |
| 말풍선 | card 바탕 + 외곽선 + 꼬리. 글꼴 Jua |
| 주민과 마을 | `mockups.html`의 주민, 집, 나무, 분수, 가로등, 마을 함수를 `Canvas`로 옮긴다. 좌표는 viewBox 100×100 기준이므로 `size/100`을 곱한다. 타일 좌표 ↔ 화면 좌표: `x = OX + (i - j) * TW/2`, `y = OY + (i + j) * TH/2`, `TH = TW/2` |

- 처음에는 모든 그림을 Canvas 코드로 그린다. 나중에 그림 파일을 받으면 같은 크기의 `ImageBitmap`으로 바꾼다.

---

## 4. 무료로 쓸 수 있는 자원

| 자원 | 라이선스 | 쓰는 법 |
|---|---|---|
| Pretendard | SIL Open Font License | https://github.com/orioncactus/pretendard 에서 받아 `res/font/`에 넣는다 (Regular, SemiBold, Bold) |
| Jua | SIL Open Font License | Google Fonts에서 받아 `res/font/jua_regular.ttf`로 넣는다 |
| Kenney 아이소메트릭 에셋 | CC0 | 그림체가 다르다. 그림이 나오기 전 자리 채우기용으로만 |

- 라이선스 전문은 `docs/licenses/`에 둔다.

---

## 5. 그림을 만들 때 쓸 프롬프트 (이미지 생성 AI용)

공통 문장: `flat pastel line-art illustration, uniform 2px dark navy-violet outline (#3D3B52), round line caps, flat pastel fills, no gradients, no shadows, no texture, simple geometric cute character, dot eyes, small mouth, pink blush ovals, cool pastel palette (lavender, baby blue, mint) with small accents of pink and butter yellow, plain white background, centered, no text, no logo, original design, not based on any existing character, artist or game`

**주민 (512×512)**
- 비: `a water-drop shaped character, baby blue body (#BFE3F7), holding a small umbrella with alternating pastel panels`
- 밤: `a round character, lavender body (#B9B3EE), a small butter yellow crescent moon on its head`
- 가족: `two round characters side by side, a big soft pink one (#F9C9DC) and a small peach one (#FAD3B8), a tiny heart above them`
- 겨울: `a white snowball character wearing a mint scarf (#C8EBD7)`
- 장거리: `a tall bean shaped mint character (#A8DCC0) with a small backpack`
- 충전: `a round butter yellow character (#FBEAA0) with a small lightning shaped tuft`

**마을 (2:1 아이소메트릭, 배경 투명)**
- 섬: `isometric 2:1 rounded grassy island, pale sage grass, soft peach earth side, curvy cream paths, same uniform outline`
- 집: `isometric tiny houses shaped after objects: an umbrella-roof house, a moon-topped house, an igloo, a heart-roof house`
- 장식: `isometric decoration set: fruit tree, flower patch, small fountain, slim lamp, bench, fence, pond with a toy boat`

### 그림 규격
- 주민 512×512, 집 384×384, 타일 208×104(2:1), 장식 256×256. 모두 투명 PNG.
- 같은 프롬프트 규칙으로 한꺼번에 만들어서 그림체를 맞춘다.
- 이미지 생성 도구의 이용 약관에서 상업적 사용이 되는지 먼저 확인한다.
- 가능하면 최종 그림은 사람 일러스트레이터가 이 규칙으로 다시 그린다.
