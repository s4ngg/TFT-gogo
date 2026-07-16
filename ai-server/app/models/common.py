from typing import Generic, TypeVar

from pydantic import BaseModel


T = TypeVar("T")


class BaseResponse(BaseModel, Generic[T]):
    success: bool
    code: str
    message: str
    data: T | None = None

    @classmethod
    def ok(cls, data: T, message: str = "OK") -> "BaseResponse[T]":
        return cls(
            success=True,
            code="OK",
            message=message,
            data=data,
        )
