import { Component, OnInit } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { ToastrService } from 'ngx-toastr';
import { NavbarLoggedComponent } from "../../navbar_loggedin/navbar_loggedin.component";
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
@Component({
  selector: 'app-key-list',
  templateUrl: './key-list.component.html',
  styleUrls: ['./key-list.component.css'],
  imports: [NavbarLoggedComponent,FormsModule,CommonModule]
})
export class KeyListComponent implements OnInit {
  keys: any[] = [];
  role = "";
  constructor(private http: HttpClient, private toast: ToastrService) {}

  ngOnInit(): void {
    this.role = localStorage.getItem('role')?.toLowerCase() || 'user';
    this.fetchKeys();
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
  deleteKey(keyId: string,location: string) {
    this.http.post(`http://localhost:8082/keys/delete`,{keyId:keyId,location:location}, this.getHttpOptions()).subscribe({
      next: () => {
        this.toast.success('Key deleted successfully.');
        this.fetchKeys();
      },
      error: () => {
        this.toast.error('Failed to delete key.');
      }
    });
  }
  fetchKeys() {
    this.http.get<any[]>('http://localhost:8082/keys/all', this.getHttpOptions()).subscribe({
      next: (response) => {
        this.keys = response;
      },
      error: () => {
        this.toast.error('Failed to fetch keys.');
      }
    });
  }
}
