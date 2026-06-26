"""
circuit_breaker.py 단위 테스트

given / when / then 패턴으로 작성.
상태 전이: CLOSED → OPEN → half-open → CLOSED / OPEN 경로를 검증한다.
"""
import time
from unittest.mock import patch

from app.core.circuit_breaker import CircuitBreaker


def _make_breaker(threshold: int = 3, window: int = 60, cooldown: int = 5) -> CircuitBreaker:
    breaker = CircuitBreaker()
    return breaker


def _settings(threshold: int = 3, window: int = 60, cooldown: int = 5):
    mock = type("S", (), {
        "circuit_breaker_threshold": threshold,
        "circuit_breaker_window": window,
        "circuit_breaker_cooldown": cooldown,
    })()
    return mock


# ── CLOSED 상태 ─────────────────────────────────────────────────────────

def test_초기_상태는_CLOSED():
    breaker = CircuitBreaker()
    with patch("app.core.circuit_breaker.settings", _settings()):
        assert breaker.is_open() is False


def test_임계치_미만_실패는_CLOSED_유지():
    breaker = CircuitBreaker()
    with patch("app.core.circuit_breaker.settings", _settings(threshold=3)):
        breaker.record_failure()
        breaker.record_failure()
        assert breaker.is_open() is False


# ── CLOSED → OPEN ───────────────────────────────────────────────────────

def test_임계치_도달_시_OPEN_전환():
    breaker = CircuitBreaker()
    with patch("app.core.circuit_breaker.settings", _settings(threshold=3)):
        breaker.record_failure()
        breaker.record_failure()
        breaker.record_failure()
        assert breaker.is_open() is True


# ── OPEN → half-open (쿨다운 경과) ─────────────────────────────────────

def test_쿨다운_경과_후_half_open_전환():
    breaker = CircuitBreaker()
    with patch("app.core.circuit_breaker.settings", _settings(threshold=2, cooldown=1)):
        breaker.record_failure()
        breaker.record_failure()
        assert breaker.is_open() is True

    with patch("app.core.circuit_breaker.settings", _settings(threshold=2, cooldown=0)), \
         patch("time.monotonic", return_value=time.monotonic() + 100):
        assert breaker.is_open() is False


# ── half-open → CLOSED (성공 시) ─────────────────────────────────────────

def test_half_open에서_성공_시_CLOSED_전환():
    breaker = CircuitBreaker()
    with patch("app.core.circuit_breaker.settings", _settings(threshold=2, cooldown=0)):
        breaker.record_failure()
        breaker.record_failure()
        # 쿨다운 0이므로 바로 half-open
        assert breaker.is_open() is False
        breaker.record_success()
        assert breaker.is_open() is False


# ── half-open → OPEN (재실패 시) ─────────────────────────────────────────

def test_half_open에서_재실패_시_다시_OPEN():
    breaker = CircuitBreaker()
    with patch("app.core.circuit_breaker.settings", _settings(threshold=2, cooldown=0)):
        breaker.record_failure()
        breaker.record_failure()
        # half-open으로 전환 (쿨다운 0)
        assert breaker.is_open() is False

    # half-open 상태에서 다시 실패 → 긴 쿨다운으로 OPEN 유지 확인
    with patch("app.core.circuit_breaker.settings", _settings(threshold=2, cooldown=9999)):
        breaker.record_failure()
        breaker.record_failure()
        assert breaker.is_open() is True


# ── 윈도우 밖 실패는 카운트에서 제외 ────────────────────────────────────

def test_윈도우_밖_실패는_제외():
    breaker = CircuitBreaker()
    with patch("app.core.circuit_breaker.settings", _settings(threshold=3, window=1)):
        breaker.record_failure()
        breaker.record_failure()
        # 윈도우(1초) 밖으로 밀어내기 위해 _failures 타임스탬프를 과거로 조작
        breaker._failures = [time.monotonic() - 10, time.monotonic() - 10]
        breaker.record_failure()
        # 윈도우 안에는 1건만 있으므로 CLOSED 유지
        assert breaker.is_open() is False


# ── record_success 호출 시 실패 이력 초기화 ──────────────────────────────

def test_record_success_실패_이력_초기화():
    breaker = CircuitBreaker()
    with patch("app.core.circuit_breaker.settings", _settings(threshold=5)):
        breaker.record_failure()
        breaker.record_failure()
        breaker.record_success()
        assert len(breaker._failures) == 0
        assert breaker._tripped_at is None
