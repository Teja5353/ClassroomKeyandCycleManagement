import { Component, OnInit } from '@angular/core';
import { WebSocketService } from '../../util/websocket.service';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { NavbarLoggedComponent } from '../../navbar_loggedin/navbar_loggedin.component';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-cr',
  standalone: true,
  imports: [FormsModule, CommonModule, NavbarLoggedComponent],
  templateUrl: './cr.component.html',
  styleUrl: './cr.component.css'
})
export class CrComponent implements OnInit {
  sortOrder: string = 'latest';
  notifications: any[] = [];
  outgoingRequests: any[] = []; // Requests made by this CR
  incomingRequests: any[] = []; // Requests received by this CR
  combinedRequests: any[] = [];
  filteredRequests: any[] = [];
  searchQuery: string = '';
  selectedStatus: string = 'All';
  email: string | null = '';

  constructor(private webSocketService: WebSocketService, private http: HttpClient, private toastr: ToastrService) {}

  ngOnInit() {
    this.email = localStorage.getItem('email');
    
    // ✅ Listen for user-specific notifications
    this.webSocketService.userNotifications$.subscribe((message: any) => {
      this.notifications.push(message.message);
    });

    this.fetchRequests();
  }

  getHttpOptions() {
    const token = localStorage.getItem('token');
    return {
      headers: new HttpHeaders({
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : '',
      }),
    };
  }

  fetchRequests() {
    if (!this.email) return;

    // ✅ Fetch outgoing key requests (requests made by the CR)
    this.http.get(`http://localhost:8082/keys/requests/${this.email}`, this.getHttpOptions())
      .subscribe((requests: any) => {
        this.outgoingRequests = requests.map((req: any) => ({ ...req, type: 'key', direction: 'outgoing' }));
        this.mergeAndFilterRequests();
      });

    // ✅ Fetch incoming key requests (requests received by the CR)
    this.http.get(`http://localhost:8082/cr/crBorrow/${this.email}`, this.getHttpOptions())
      .subscribe((requests: any) => {
        this.incomingRequests = requests.map((req: any) => ({ ...req, type: 'key', direction: 'incoming' }));
        this.mergeAndFilterRequests();
      });

    // ✅ Fetch outgoing cycle requests (requests made by the CR)
    this.http.get(`http://localhost:8082/cycles/requests/${this.email}`, this.getHttpOptions())
      .subscribe((requests: any) => {
        this.outgoingRequests = [...this.outgoingRequests, ...requests.map((req: any) => ({ ...req, type: 'cycle', direction: 'outgoing' }))];
        this.mergeAndFilterRequests();
      });
  }

  mergeAndFilterRequests() {
    // ✅ Merge both outgoing and incoming requests
    this.combinedRequests = [...this.outgoingRequests, ...this.incomingRequests];

    // ✅ Apply sorting and filtering
    this.applyFilters();
  }

  applyFilters() {
    this.filteredRequests = this.combinedRequests.filter(request => {
      return (
        (this.selectedStatus === 'All' || request.status === this.selectedStatus) &&
        (this.searchQuery === '' || request.borrowerEmail?.toLowerCase().includes(this.searchQuery.toLowerCase()))
      );
    }).sort((a, b) => {
      return this.sortOrder === 'latest'
        ? new Date(b.requestTime).getTime() - new Date(a.requestTime).getTime()
        : new Date(a.requestTime).getTime() - new Date(b.requestTime).getTime();
    });
  }

  approveRequest(requestId: number) {
    this.http.post(`http://localhost:8082/cr/approve-key/${requestId}`, {}, this.getHttpOptions()).subscribe({
      next: () => {
        this.toastr.success("Request approved!");
        this.fetchRequests(); // Refresh requests
      },
      error: () => {
        this.toastr.error("Failed to approve request.");
      }
    });
  }

  denyRequest(requestId: number) {
    this.http.post(`http://localhost:8082/cr/deny-key/${requestId}`, {}, this.getHttpOptions()).subscribe({
      next: () => {
        this.toastr.info("Request denied!");
        this.fetchRequests(); // Refresh requests
      },
      error: () => {
        this.toastr.error("Failed to deny request.");
      }
    });
  }
  approveReturnKey(requestId: number,keyId:string) {
    this.http.post(`http://localhost:8082/cr/approve-key-return/${requestId}`, {keyId:keyId,borrowerEmail:this.email}, this.getHttpOptions()).subscribe({
      next: () => {
        this.toastr.success("Request approved!");
        this.fetchRequests(); // Refresh requests
      },
      error: () => {
        this.toastr.error("Failed to approve request.");
      }
    });
  }
  denyKeyReturn(requestId: number,keyId:string) {
    this.http.post(`http://localhost:8082/cr/deny-key-return/${requestId}`, {keyId:keyId,borrowerEmail:this.email}, this.getHttpOptions()).subscribe({
      next: () => {
        this.toastr.info("Request denied!");
        this.fetchRequests(); // Refresh requests
      },
      error: () => {
        this.toastr.error("Failed to deny request.");
      }
    });
  }
}
