import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavBar } from './shared/components/navbar/navbar';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet,NavBar],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  protected readonly title = signal('url-shortener-ui');
}
