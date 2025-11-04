import { Component, signal } from '@angular/core';
  import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
  import { CommonModule } from '@angular/common';
  import { MatCardModule } from '@angular/material/card';
  import { MatFormFieldModule } from '@angular/material/form-field';
  import { MatInputModule } from '@angular/material/input';
  import { MatSelectModule } from '@angular/material/select';
  import { MatButtonModule } from '@angular/material/button';
  import { MatIconModule } from '@angular/material/icon';
  import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
  import { UrlService } from '../../core/services/url.service';
  import { UrlRequest, UrlResponse } from '../../core/models/url.model';
  import { environment } from '../../environments/environment';

@Component({
  selector: 'app-dashboard',
  imports: [
      CommonModule,
      ReactiveFormsModule,
      MatCardModule,
      MatFormFieldModule,
      MatInputModule,
      MatSelectModule,
      MatButtonModule,
      MatIconModule,
      MatProgressSpinnerModule
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})

export class Dashboard {
  urlForm: FormGroup;
  loading = signal<boolean>(false);
  errorMessage = signal<string>('');
  shortenedUrl = signal<UrlResponse | null>(null);
  fullShortUrl = ():string | null => {
    const url = this.shortenedUrl();
    const baseUrl = environment.apiUrl.replace('/api/v1', '');
    return url ? `${baseUrl}/${url.shortUrl}` : null;
  };

    expirationOptions = [
      { value: 7, label: '7 days' },
      { value: 14, label: '14 days' },
      { value: 30, label: '30 days' },
      { value: 90, label: '90 days' },
      { value: 365, label: '1 year' }
    ]

  constructor(private fb: FormBuilder,
      private urlService: UrlService) {
        this.urlForm = this.fb.group({
          url: ['', [Validators.required, Validators.pattern(/^(https?:\/\/)?([\da-z\.-]+)\.([a-z\.]{2,6})([\/\w \.-]*)*\/?$/)]],
          expirationDays: [30, [Validators.required]]
        });
      }

  onSubmit():void{
    if(this.urlForm.valid) {
      this.loading.set(true);
      this.errorMessage.set('');
       this.shortenedUrl.set(null);
       const request: UrlRequest = this.urlForm.value;
       
       this.urlService.createShortUrl(request).subscribe({
        next: (response) => {
          console.log('URL shortened successfully:', response);
            this.loading.set(false);
            this.shortenedUrl.set(response);
            this.urlForm.reset({ url: '', expirationDays: 30 });
        },
        error: (error) => {
          console.error('URL shortening failed:', error);
          this.loading.set(false);
          const message = error.error?.message || 'Failed to shorten URL. Please try again.';
          this.errorMessage.set(message);
        }
       })
    }
  } 

  copyToClipboard(url: string): void {
    navigator.clipboard.writeText(url).then(() => {
      console.log('URL copied to clipboard!');
    }).catch(err => {
      console.error('Failed to copy URL:', err);
    });
  }

}
