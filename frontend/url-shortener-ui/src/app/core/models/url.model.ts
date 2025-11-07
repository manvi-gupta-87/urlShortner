// URL Request DTO (matches backend UrlRequestDto exactly)
export interface UrlRequest {
  url: string;              // Backend uses 'url', not 'originalUrl'
  expirationDays?: number;  // Backend uses 'expirationDays', optional
}

// URL Response DTO (matches backend UrlResponseDto exactly)
export interface UrlResponse {
  id: number;
  originalUrl: string;
  shortUrl: string;
  expiresAt: string;       // LocalDateTime comes as ISO string
  clickCount: number;
  deactivated:boolean;
  // Note: Backend doesn't have createdAt or active fields!
}
