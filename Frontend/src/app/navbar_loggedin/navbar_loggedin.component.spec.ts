import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NavbarLoggedComponent } from './navbar_loggedin.component';

describe('NavbarLoggedComponent', () => {
  let component: NavbarLoggedComponent;
  let fixture: ComponentFixture<NavbarLoggedComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NavbarLoggedComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(NavbarLoggedComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
