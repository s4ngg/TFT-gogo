import axios, { type InternalAxiosRequestConfig } from 'axios'
import useAuthStore from '../store/useAuthStore'
import type { ApiResponse } from './apiResponse'
import { unwrapApiResponse } from './apiResponse'
import { normalizeAuthResponse, type RawAuthResponse } from './memberApiPayload'

const DEFAULT_API_BASE_URL = '/api'
const AUTH_REFRESH_PATH = '/v1/auth/refresh'
let refreshAccessTokenPromise: Promise<string> | null = null

interface RetriableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean
}

interface ResolveApiBaseUrlOptions {
  apiUrl?: string
  apiBaseUrl?: string
  currentOrigin?: string
}

function getCurrentOrigin(): string | undefined {
  if (typeof window === 'undefined') {
    return undefined
  }

  return window.location.origin
}

function normalizeOrigin(origin: string | undefined): string | undefined {
  if (!origin) {
    return undefined
  }

  try {
    return new URL(origin).origin
  } catch {
    return undefined
  }
}

function isBareOriginUrl(candidate: URL): boolean {
  return candidate.pathname === '/' && !candidate.search && !candidate.hash
}

export function resolveApiBaseUrl({
  apiUrl = import.meta.env?.VITE_API_URL,
  apiBaseUrl = import.meta.env?.VITE_API_BASE_URL,
  currentOrigin = getCurrentOrigin(),
}: ResolveApiBaseUrlOptions = {}): string {
  const candidate = [apiUrl, apiBaseUrl].find((value) => value?.trim())

  if (!candidate) {
    return DEFAULT_API_BASE_URL
  }

  const trimmedCandidate = candidate.trim()

  try {
    const parsedCandidate = new URL(trimmedCandidate)
    const normalizedCurrentOrigin = normalizeOrigin(currentOrigin)

    if (
      normalizedCurrentOrigin &&
      parsedCandidate.origin === normalizedCurrentOrigin &&
      isBareOriginUrl(parsedCandidate)
    ) {
      return DEFAULT_API_BASE_URL
    }
  } catch {
    return trimmedCandidate
  }

  return trimmedCandidate
}

const axiosInstance = axios.create({
  baseURL: resolveApiBaseUrl(),
  timeout: 10000,
  withCredentials: true,
})

axiosInstance.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

axiosInstance.interceptors.response.use(
  (response) => response,
  async (error) => {
    const config = error.config as RetriableRequestConfig | undefined

    if (error.response?.status === 401 && config && !config._retry && !isAuthEndpoint(config.url)) {
      config._retry = true

      try {
        const token = await getRefreshAccessTokenPromise()
        config.headers.Authorization = `Bearer ${token}`

        return axiosInstance(config)
      } catch {
        useAuthStore.getState().clearAuth()
      }
    } else if (error.response?.status === 401) {
      useAuthStore.getState().clearAuth()
    }

    return Promise.reject(error)
  },
)

function isAuthEndpoint(url: string | undefined): boolean {
  return Boolean(url?.startsWith('/v1/auth/'))
}

function getRefreshAccessTokenPromise(): Promise<string> {
  refreshAccessTokenPromise ??= refreshAccessToken()
    .finally(() => {
      refreshAccessTokenPromise = null
    })

  return refreshAccessTokenPromise
}

async function refreshAccessToken(): Promise<string> {
  try {
    const apiBaseUrl = resolveApiBaseUrl().replace(/\/$/, '')
    const response = await axios.post<RawAuthResponse | ApiResponse<RawAuthResponse>>(
      `${apiBaseUrl}${AUTH_REFRESH_PATH}`,
      undefined,
      {
        timeout: 10000,
        withCredentials: true,
      },
    )
    const payload = unwrapApiResponse(response.data)
    const auth = normalizeAuthResponse(payload, payload.user?.email ?? payload.member?.email ?? '')

    useAuthStore.getState().setAuth({ token: auth.token })
    return auth.token
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error)
    throw new Error(`Refresh access token failed: ${message}`)
  }
}

export default axiosInstance
