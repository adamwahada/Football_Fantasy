import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminDashboardUsersManagementComponent } from './admin-dashboard-users-management.component';

describe('AdminDashboardUsersManagementComponent', () => {
  let component: AdminDashboardUsersManagementComponent;
  let fixture: ComponentFixture<AdminDashboardUsersManagementComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminDashboardUsersManagementComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdminDashboardUsersManagementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
