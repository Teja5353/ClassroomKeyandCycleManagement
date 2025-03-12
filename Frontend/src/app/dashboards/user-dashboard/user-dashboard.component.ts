import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../auth/auth.service';
import { NavbarLoggedComponent } from "../../navbar_loggedin/navbar_loggedin.component";
import { HttpClient, HttpHeaders } from '@angular/common/http';
@Component({
  selector: 'app-user-dashboard',
  standalone: true, // ✅ Use standalone component
  imports: [CommonModule, NavbarLoggedComponent],
  templateUrl: './user-dashboard.component.html',
  styleUrls: ['./user-dashboard.component.css'] // ✅ Fixed property name
})
export class UserDashboardComponent implements OnInit {
  firstName: string = "";
  lastName: string = "";
  email: string = "";
  role: string = "";
  availableCycles: any[] = [];
  borrowedCycles: any[] = [];

  constructor(private authService: AuthService, private http: HttpClient) {}

  ngOnInit(): void {
    this.loadUserDetails();
  }

  loadUserDetails() {
    this.authService.getUserDetails().subscribe(
      user => {
        this.firstName = user.firstname;
        this.lastName = user.lastname;
        this.email = user.email;
        this.role = user.role;
        this.loadAvailableCycles(); // ✅ Load cycles after fetching user details
      },
      error => {
        console.error("Error fetching user details", error);
      }
    );
  }

  loadAvailableCycles() {
    const httpOptions = {
      headers: new HttpHeaders({
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      })
    };
  
    this.http.get<any>("http://localhost:8082/userDashboard/available", { ...httpOptions, params: { email: this.email } })
      .subscribe(
        response => {
          if (response) {
            if (Array.isArray(response)) {
              this.availableCycles = response;
            } else {
              this.availableCycles = [response]; // ✅ Wrap single object in an array
            }
          } else {
            this.availableCycles = [];
          }
        },
        error => console.error("❌ Error fetching available cycles", error)
      );
  }
  

  logout() {
    this.authService.logout();
  }
}
