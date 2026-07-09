import axios, { type InternalAxiosRequestConfig } from 'axios'
import useAuthStore from '../store/useAuthStore'
import type { ApiResponse } from './apiResponse'
import { unwrapApiResponse } from './apiResponse'
import { getAuthSessionRevision, isLogoutInProgress } from './authSessionControl'
import { normalizeAuthResponse, type AuthResponse, type RawAuthResponse } from './memberApiPayload'

const DEFAULT_API_BASE_URL = '/api'
const AUTH_REFRESH_PATH = '/v1/auth/refresh'
let refreshAuthSessionPromise: Promise<AuthResponse> | null = null

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

    if (
      error.response?.status === 401
      && config
      && !config._retry
      && !isAuthEndpoint(config.url)
      && !isLogoutInProgress()
    ) {
      config._retry = true
      const retryStartedAtRevision = getAuthSessionRevision()

      try {
        const auth = await getRefreshAuthSessionPromise()

        if (getAuthSessionRevision() !== retryStartedAtRevision) {
          throw new Error('Retry skipped because auth session changed.')
        }

        config.headers.Authorization = `Bearer ${auth.token}`

        return axiosInstance(config)
      } catch {
        if (getAuthSessionRevision() === retryStartedAtRevision) {
          useAuthStore.getState().clearAuth()
        }
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

export function getRefreshAuthSessionPromise(): Promise<AuthResponse> {
  refreshAuthSessionPromise ??= refreshAuthSession()
    .finally(() => {
      refreshAuthSessionPromise = null
    })

  return refreshAuthSessionPromise
}

async function refreshAuthSession(): Promise<AuthResponse> {
  const refreshStartedAtRevision = getAuthSessionRevision()

  if (isLogoutInProgress()) {
    throw new Error('Refresh session skipped during logout.')
  }

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

    if (isLogoutInProgress() || getAuthSessionRevision() !== refreshStartedAtRevision) {
      throw new Error('Refresh session skipped during logout.')
    }

    useAuthStore.getState().setAuth({ token: auth.token })
    return auth
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error)
    throw new Error(`Refresh session failed: ${message}`)
  }
}

export default axiosInstance
