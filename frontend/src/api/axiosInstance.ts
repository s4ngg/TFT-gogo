import axios from 'axios'
import useAuthStore from '../store/useAuthStore'

const DEFAULT_API_BASE_URL = '/api'

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
  (error) => {
    if (error.response?.status === 401) {
      useAuthStore.getState().clearAuth()
    }

    return Promise.reject(error)
  },
)

export default axiosInstance
