import axiosInstance from "./axiosInstance"
import type {AuthUser} from "../store/useAuthStore"


interface ApiEnvelope<T>{
    data: T
    message?: string
    success?: boolean
}

interface RawAuthResponse {
    accessToken?: string
    jwt?: string
    member?: Partial<AuthUser>
    token?: string
    user?: Partial<AuthUser>
}

export interface LoginRequest {
    email: string
    password: string
}

export interface SignupRequest extends LoginRequest {
    summonerName?: string
    tagLine?:string
}

export interface AuthResponse {
    token: string
    user: AuthUser
}

function unwrapResponse<T>(payload: T | ApiEnvelope<T>) {
    if (payload && typeof payload === 'object' && 'data' in payload) {
        return payload.data
    }

    return payload
}

function normalizeAuthResponse(payload: RawAuthResponse, fallbackEmail: string): AuthResponse {
    const token = payload.token ?? payload.accessToken ?? payload.jwt

    if (!token) {
        throw new Error('Auth response does not include a token')
    }

    const userPayload = payload.user ?? payload.member ?? {}
    const email = userPayload.email ?? fallbackEmail

    return{
        token,
        user :{
            ...userPayload,
            email,
        },
    }
}

export async function login(request: LoginRequest) {
    try {
        const response = await axiosInstance.post<RawAuthResponse | ApiEnvelope<RawAuthResponse>>(
            '/members/login',
            request,
        )
        const payload = unwrapResponse(response.data)

        return normalizeAuthResponse(payload, request.email)
    } catch (error) {
        const message = error instanceof Error ? error.message : String(error)
        throw new Error(`Login failed: ${message}`)
    }
}

export async function signup(request: SignupRequest) {
    try {
        const response = await axiosInstance.post<RawAuthResponse | ApiEnvelope<RawAuthResponse>>(
            '/members/signup',
            request,
        )

        const payload = unwrapResponse(response.data)

        return normalizeAuthResponse(payload, request.email)
    } catch (error) {
        const message = error instanceof Error ? error.message : String(error)
        throw new Error(`Signup failed: ${message}`)
    }
}

