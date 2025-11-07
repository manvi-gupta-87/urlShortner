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
import { MatTableModule } from '@angular/material/table';
  import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
  import { MatDialog, MatDialogModule } from '@angular/material/dialog';
    import { MatTooltipModule } from '@angular/material/tooltip';

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
      MatProgressSpinnerModule,
      MatTableModule,
      MatSnackBarModule,
      MatDialogModule,
      MatTooltipModule
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
  urlList = signal<UrlResponse[]>([]);
  isLoadingUrls = signal<boolean>(false);

    expirationOptions = [
      { value: 7, label: '7 days' },
      { value: 14, label: '14 days' },
      { value: 30, label: '30 days' },
      { value: 90, label: '90 days' },
      { value: 365, label: '1 year' }
    ]

  constructor(private fb: FormBuilder,
      private urlService: UrlService, 
      private snackBar: MatSnackBar,
      private dialog: MatDialog) {
        this.loadUserUrls();
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
            this.loadUserUrls()
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
    const fullUrl = url.startsWith('https://') || url.startsWith('http://') ? url : 
    `${environment.apiUrl.replace('/api/v1', '')}/${url}`;
    navigator.clipboard.writeText(fullUrl).then(() => {
      this.snackBar.open('URL copied to clipboard!', 'Close', {
        duration: 3000,
        horizontalPosition: 'center',
        verticalPosition: 'bottom'
      });
    }).catch(err => {
      console.error('Failed to copy URL:', err);
      this.snackBar.open('Failed to copy URL', 'Close', {
        duration: 3000
      });
    });
  }

  loadUserUrls():void {
    this.isLoadingUrls.set(true);
    this.urlService.getAllUrls().subscribe({
      next: (urls) => {
        this.urlList.set(urls);
        this.isLoadingUrls.set(false);
      },
      error: (error) => {
        console.error('Error loading URLs:', error);
        this.isLoadingUrls.set(false);
      } 
    })
  }

  deactivateUrl(shortUrl: string ):void {
    if(confirm('Are you sure you want to deactivate this URL? It will no longer redirect.')) {
      this.urlService.deactivateUrl(shortUrl).subscribe({
        next: ()=>{
          this.snackBar.open('URL deactivated successfully', 'Close', {
            duration: 3000
          });
          this.loadUserUrls();
        },
        error: (error) => {
          console.error('Error deactivating URL:', error);
          this.snackBar.open('Failed to deactivate URL', 'Close', {
            duration: 3000
          });
        }
      })
    }
  }

}
