import { HttpClient } from "@angular/common/http";
import { UrlRequest, UrlResponse } from "../models/url.model";
import { Observable } from "rxjs";
import { environment } from "../../environments/environment";
import { Injectable } from "@angular/core";

@Injectable({
    providedIn: 'root'
  })
export class UrlService {
    private apiUrl = environment.apiUrl;

    constructor(private http:HttpClient) {}

    /**
     * Create a shortened URL
     * @param request - Contains original URL and optional expiration days
     * @returns Observable of UrlResponse with shortened URL details
     */

    createShortUrl(request:UrlRequest): Observable<UrlResponse> {
        return this.http.post<UrlResponse>(`${this.apiUrl}/urls`, request);
    }

    getAllUrls():Observable<UrlResponse[]> {
        return this.http.get<UrlResponse[]>(`${this.apiUrl}/urls`);
    }
}