import hmac
from typing import Optional

from fastapi import Header, HTTPException, status

from app.core.config import settings


async def verify_internal_secret(x_internal_secret: Optional[str] = Header(default=None)):
    if x_internal_secret is None or not hmac.compare_digest(x_internal_secret, settings.internal_secret):
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Forbidden")
