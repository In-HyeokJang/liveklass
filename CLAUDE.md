# CLAUDE.md

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.

---

## 5. 현재 과제 컨텍스트

**과제명**: BE 과제 A — 수강 신청 시스템  
**상세 요구사항**: `docs/ASSIGNMENT.md` 참고  
**진행 상태**: 필수 구현 100%, 선택 구현 중 waitlist 제외 완료

### 과제 핵심 제약
- `X-User-Id` 헤더로 인증 (JWT 불필요)
- 크리에이터(CREATOR)만 강의 생성/상태변경 가능
- 클래스메이트(CLASSMATE)만 수강 신청 가능
- 동시 신청 정원 초과 방지 → 비관적 락 (`SELECT FOR UPDATE`)
- 결제 후 7일 내 취소 가능

### 제출 시 확인 항목
- [ ] README 전체 섹션 완성
- [ ] API 명세 최신 상태 유지
- [ ] 테스트 전체 통과 (`./gradlew test`)
- [ ] Docker Compose 로 정상 실행 확인
- [ ] `docs/ASSIGNMENT.md` 체크리스트 완료

---

## 6. Daily Dev Log

매일 개발 내용은 `docs/devlog/YYYY-MM-DD.md` 파일에 기록합니다.

```
docs/devlog/
├── 2026-05-20.md   ← 오늘
└── ...
```

### 일지 작성 방법

새 날짜 파일을 만들 때 아래 템플릿을 사용합니다:

```markdown
# YYYY-MM-DD 개발 일지

## 작업 요약
- 

## 변경 파일
- 

## 이슈 / 버그
- 

## 다음 할 일
- 
```

Claude에게 "오늘 개발 일지 작성해줘" 또는 "devlog 업데이트해줘"라고 하면 자동으로 작성합니다.
