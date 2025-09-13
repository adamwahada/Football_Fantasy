import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminDashboardFundsComponent } from './admin-dashboard-funds.component';

describe('AdminDashboardComponent', () => {
  let component: AdminDashboardFundsComponent;
  let fixture: ComponentFixture<AdminDashboardFundsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminDashboardFundsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdminDashboardFundsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
