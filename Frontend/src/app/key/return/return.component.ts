import { Component, OnInit } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { WebSocketService } from '../../util/websocket.service';
import { ToastrService } from 'ngx-toastr';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { NavbarLoggedComponent } from '../../navbar_loggedin/navbar_loggedin.component';
import { Router } from '@angular/router';

@Component({
  selector: 'app-return',
  templateUrl: './return.component.html',
  styleUrls: ['./return.component.css'],
  imports: [FormsModule, CommonModule, NavbarLoggedComponent]
})
export class returnComponent implements OnInit {
  selectedKeyId: string = '';
  userEmail: string | null = null;
  borrowedKeys: any[] = [];
  role: string = '';
  returnTo : string = 'admin';
  constructor(
    private http: HttpClient,
    private webSocketService: WebSocketService,
    private toast: ToastrService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.userEmail = localStorage.getItem('email');
    this.role = localStorage.getItem('role')?.toLowerCase() || 'user';

    if (this.userEmail) {
      this.fetchBorrowedKeys();
    } else {
      this.toast.error('User email not found. Please log in again.');
      this.router.navigate(['/login']);
    }
  }

  getHttpOptions() {
    const token = localStorage.getItem('token');
    return {
      headers: new HttpHeaders({
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : '',
      })
    };
  }

  // ✅ Fetch Keys Borrowed by Logged-in User
  fetchBorrowedKeys() {
    this.http.get<any[]>(`http://localhost:8082/admin/borrowed-keys/${this.userEmail}`, this.getHttpOptions()).subscribe({
      next: (data) => {
        this.borrowedKeys = data;
      },
      error: (err) => {
        this.toast.error('Failed to fetch borrowed keys.');
        console.error(err);
      }
    });
  }

  // ✅ Request Key Return
  requestKeyReturn() {
    if (!this.selectedKeyId) {
      this.toast.error('Please select a key before requesting a return.');
      return;
    }

    const requestData = { keyId: this.selectedKeyId, borrowerEmail: this.userEmail };
    this.toast.info('Sending return request...');

    if(this.returnTo == 'admin'){
      this.http.post('http://localhost:8082/admin/request-key-return', requestData, this.getHttpOptions())
      .subscribe({
        next: () => {
          this.toast.success(`Return request sent for key: ${this.selectedKeyId}`);
          this.fetchBorrowedKeys(); // ✅ Refresh borrowed keys list
          if (this.role) {
            this.router.navigate([`${this.role}/dashboard`]);
          }
        },
        error: () => {
          this.toast.error('Could not request key return.');
        }
      });
    }
    else{
      this.http.post('http://localhost:8082/cr/request-key-return', requestData, this.getHttpOptions())
      .subscribe({
        next: () => {
          this.toast.success(`Return request sent for key: ${this.selectedKeyId}`);
          this.fetchBorrowedKeys(); // ✅ Refresh borrowed keys list
          if (this.role) {
            this.router.navigate([`${this.role}/dashboard`]);
          }
        },
        error: () => {
          this.toast.error('Could not request key return.');
        }
      });
    }
  }
}
