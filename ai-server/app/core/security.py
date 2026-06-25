import hmac

from fastapi import Header, HTTPException, status

from app.core.config import settings


async def verify_internal_secret(x_internal_secret: str = Header(...)):
    if not hmac.compare_digest(x_internal_secret, settings.internal_secret):
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Forbidden")
