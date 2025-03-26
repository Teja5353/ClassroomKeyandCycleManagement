import { Component, OnInit } from '@angular/core';
import { NavbarLoggedComponent } from '../../navbar_loggedin/navbar_loggedin.component';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { HttpClient,HttpHeaders } from '@angular/common/http';
import { forkJoin } from 'rxjs';
@Component({
  selector: 'app-cr-dashboard',
  imports: [NavbarLoggedComponent,FormsModule,CommonModule],
  templateUrl: './cr-dashboard.component.html',
  styleUrl: './cr-dashboard.component.css'
})
export class CrDashboardComponent implements OnInit {
  firstName: string = "";
  lastName: string = "";
  email: string = "";
  role: string = "";
  borrowedCycles:any[] = [];
  borrowedKeys:any[] = [];
  borrowRequests:any[] = [];
  uemail:string = "";
  constructor(private router:Router,private authService:AuthService,private http:HttpClient){}
  ngOnInit(): void {
    this.uemail = localStorage.getItem('email') || ''; 
  
    if (this.uemail) {
      this.fetchAllocatedResources();  // ✅ Call if email is present
    }
    this.loadUserDetails();
  }
  
  loadUserDetails() {
    this.authService.getUserDetails().subscribe(
      user => {
        this.firstName = user.firstname;
        this.lastName = user.lastname;
        this.email = user.email;
        this.role = user.role;
      },
      error => {
        console.error("Error fetching user details", error);
      }
    );
  }
  getHttpOptions() {
    const token = localStorage.getItem('token');
    return {
      headers: new HttpHeaders({
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
      })
    };
  }
  fetchAllocatedResources(){
    const borrowedCycles$ = this.http.get(`http://localhost:8082/cycles/get-cycles/${this.uemail}`,this.getHttpOptions());
    const borrowedKeys$ = this.http.get(`http://localhost:8082/keys/get-keys/${this.uemail}`,this.getHttpOptions());
    forkJoin([borrowedCycles$, borrowedKeys$]).subscribe(([borrowedCycles, borrowedKeys]: any[]) => {
      this.borrowedCycles = Array.isArray(borrowedCycles) ? borrowedCycles : [];
      this.borrowedKeys = Array.isArray(borrowedKeys) ? borrowedKeys : [];
      console.log(this.borrowedKeys);
      this.borrowRequests = [...this.borrowedCycles, ...this.borrowedKeys]
  });
  }
  sendRequestForKey(){
    this.router.navigate(["key/borrow"])
  }
  sendRequestForCycle(){
    this.router.navigate(["cycles/borrow"])
  }
  returnKey(){
    this.router.navigate(["key/return"])
  }
  returnCycle(){
    this.router.navigate(["cycles/return"])
  }
  viewRequests(){
    this.router.navigate(["cr/requests"])
  }
}
