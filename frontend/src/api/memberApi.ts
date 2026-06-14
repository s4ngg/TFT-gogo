import axiosInstance from './axiosInstance'
import type { ApiResponse } from './apiResponse'
import { unwrapApiResponse } from './apiResponse'
import {
  normalizeAuthResponse,
  normalizeMemberResponse,
  type AuthResponse,
  type MemberProfile,
  type RawAuthResponse,
  type RawMemberResponse,
} from './memberApiPayload'

export interface LoginRequest {
  email: string
  password: string
}

export interface SignupRequest extends LoginRequest {
  nickname: string
}

export async function login(request: LoginRequest): Promise<AuthResponse> {
  try {
    const response = await axiosInstance.post<RawAuthResponse | ApiResponse<RawAuthResponse>>(
      '/v1/auth/login',
      request,
    )
    const payload = unwrapApiResponse(response.data)

    return normalizeAuthResponse(payload, request.email)
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error)
    throw new Error(`Login failed: ${message}`)
  }
}

export async function signup(request: SignupRequest): Promise<AuthResponse> {
  try {
    const response = await axiosInstance.post<RawAuthResponse | ApiResponse<RawAuthResponse>>(
      '/v1/auth/signup',
      request,
    )

    const payload = unwrapApiResponse(response.data)

    return normalizeAuthResponse(payload, request.email)
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error)
    throw new Error(`Signup failed: ${message}`)
  }
}

export async function getMe(): Promise<MemberProfile> {
  try {
    const response = await axiosInstance.get<RawMemberResponse | ApiResponse<RawMemberResponse>>(
      '/v1/members/me',
    )
    const payload = unwrapApiResponse(response.data)

    return normalizeMemberResponse(payload)
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error)
    throw new Error(`Fetch current member failed: ${message}`)
  }
}
