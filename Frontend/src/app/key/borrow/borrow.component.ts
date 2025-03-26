import { Component, OnInit } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { WebSocketService } from '../../util/websocket.service';
import { ToastrService } from 'ngx-toastr';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { NavbarLoggedComponent } from '../../navbar_loggedin/navbar_loggedin.component';
import { KeyListComponent } from "../../util/key-list/key-list.component";
@Component({
  selector: 'app-borrow',
  templateUrl: './borrow.component.html',
  styleUrls: ['./borrow.component.css'],
  imports: [FormsModule, CommonModule, NavbarLoggedComponent, KeyListComponent]
})
export class borrowComponent implements OnInit {
  selectedLocation: string = '';
  selectedKeyId: string = '';
  userEmail: string | null = null;
  notifications: string[] = [];
  availableKeys: any[] = [];
  filteredKeys: any[] = [];
  borrowedKeys: any[] = [];
  locations: string[] = [];
  role: string = '';
  duration: number | null = null;
  borrowFrom: string = 'admin'; // Default: Borrow from CR
  startTime: string = '';
  endTime: string = '';
  constructor(
    private http: HttpClient,
    private webSocketService: WebSocketService,
    private toast: ToastrService,
    private router: Router
  ) {}
  formatToLocalTime(time: string): string {
    return time ? `${time}:00` : ''; // Append ":00" to match "HH:mm:ss"
  }

  ngOnInit(): void {
    this.userEmail = localStorage.getItem('email');
    this.role = localStorage.getItem('role')?.toLowerCase() || 'user';

    if (this.role !== 'cr' && this.role !== 'admin') {
      this.toast.error('You are not authorized to borrow keys.');
      this.router.navigate(['/']);
      return;
    }

    this.webSocketService.userNotifications$.subscribe((message) => {
      if (message) {
        this.notifications.push(message);
      }
    });

    this.loadKeys();
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

  loadKeys() {
    this.http.get<any[]>('http://localhost:8082/keys/all', this.getHttpOptions()).subscribe(
      (data) => {
        this.availableKeys = data;
        
        // Extract unique locations
        this.locations = [...new Set(this.availableKeys.map(key => key.location))];
      },
      (error) => {
        console.error('Error fetching keys:', error);
      }
    );
  }

  onLocationChange() {
    if (this.selectedLocation) {
      this.filteredKeys = this.availableKeys.filter(key => key.location === this.selectedLocation);
      this.selectedKeyId = ''; // Reset selected key
    } else {
      this.filteredKeys = [];
    }
  }

  requestBorrow() {
    if (!this.selectedLocation) {
      this.toast.error('Please select a location.');
      return;
    }
    if (!this.selectedKeyId) {
      this.toast.error('Please select a key.');
      return;
    }
    if(!this.startTime || !this.endTime) {
      this.toast.error('Please select start and end times.');
      return;
    }
    console.log("Raw Start Time:", this.startTime); // Debugging
    console.log("Raw End Time:", this.endTime);
    const startTimer = this.formatToLocalTime(this.startTime);
    const endTimer = this.formatToLocalTime(this.endTime);
    console.log("Formatted Start Time:", startTimer); // Debugging
    console.log("Formatted End Time:", endTimer);
    const requestData = { 
      keyId: this.selectedKeyId, 
      borrowerEmail: this.userEmail,
      borrowFrom: this.borrowFrom,
      location:this.selectedLocation,
      startTime: startTimer,
      endTime:endTimer
    };

    const endpoint = this.borrowFrom === 'admin' 
      ? 'http://localhost:8082/admin/request-key-borrow' 
      : 'http://localhost:8082/cr/request-key-borrow';

    this.toast.info(`Sending borrow request to ${this.borrowFrom.toUpperCase()}...`);

    this.http.post(endpoint, requestData, this.getHttpOptions())
      .subscribe({
        next: () => {
          this.toast.success(`Borrow request sent for key: ${this.selectedKeyId}`);
          if (this.role) {
            this.router.navigate([`${this.role}/dashboard`]);
          }
        },
        error: () => {
          this.toast.error('Key is not available or request failed.');
        }
      });
  }
  
  
}
