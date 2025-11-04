// Analytics Response DTO (matches backend UrlAnalyticsResponse exactly)
export interface AnalyticsResponse {
  shortCode: string;
  originalUrl: string;
  totalClicks: number;
  clicksByDate: { [key: string]: number };      // Map<String, Long> in Java
  clicksByBrowser: { [key: string]: number };   // Map<String, Long> in Java
  clicksByDeviceType: { [key: string]: number }; // Map<String, Long> in Java
  clicksByCountry: { [key: string]: number };   // Map<String, Long> in Java
}

// Helper interfaces for converting Maps to arrays for charts
export interface ChartData {
  label: string;
  value: number;
}
